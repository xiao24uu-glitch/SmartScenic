package com.scenic.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.common.exception.BusinessException;
import com.scenic.config.DeepSeekProperties;
import com.scenic.config.ScenicProperties;
import com.scenic.dto.AiChatDTO;
import com.scenic.dto.TravelogueDTO;
import com.scenic.dto.TravelogueDownloadDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final DeepSeekProperties deepSeekProperties;
    private final ScenicProperties scenicProperties;
    private final ScenicSpotMapper scenicSpotMapper;
    private final ScenicFacilityMapper scenicFacilityMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final AiConversationMapper aiConversationMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final EntryLogMapper entryLogMapper;

    @Override
    public SseEmitter chat(Long userId, AiChatDTO dto) {
        SseEmitter emitter = new SseEmitter(300000L);

        // 暂停运营时拒绝AI对话（前端已拦截，此处后端兜底）
        if (scenicProperties.getStatus() != null && scenicProperties.getStatus() == 0) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(JSON.toJSONString(new SseData("景区暂停运营，AI助手暂时无法提供服务", "error"))));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = IdUtil.fastSimpleUUID();
        }

        // 构建System Prompt
        String systemPrompt = buildSystemPrompt();

        // 获取历史对话
        List<AiConversation> history = aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getSessionId, sessionId)
                        .orderByAsc(AiConversation::getCreateTime)
        );

        final String finalSessionId = sessionId;
        final String intent = detectIntent(dto.getMessage());

        SecurityContext securityContext = SecurityContextHolder.getContext();

        new Thread(() -> {
            SecurityContextHolder.setContext(securityContext);
            HttpURLConnection conn = null;
            try {
                // 构建OpenAI兼容格式的消息列表
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));

                // 添加历史对话作为上下文
                for (AiConversation conv : history) {
                    messages.add(Map.of("role", "user", "content", conv.getQuestion()));
                    messages.add(Map.of("role", "assistant", "content", conv.getAnswer()));
                }
                // 当前用户消息
                messages.add(Map.of("role", "user", "content", dto.getMessage()));

                // 构建请求体
                Map<String, Object> requestBody = Map.of(
                        "model", deepSeekProperties.getModel(),
                        "messages", messages,
                        "stream", true,
                        "max_tokens", deepSeekProperties.getMaxTokens(),
                        "temperature", deepSeekProperties.getTemperature()
                );

                String jsonBody = JSON.toJSONString(requestBody);

                // 发起DeepSeek API请求
                URI apiUri = URI.create(deepSeekProperties.getBaseUrl());
                conn = (HttpURLConnection) apiUri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + deepSeekProperties.getApiKey());
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(300000);
                conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
                conn.getOutputStream().flush();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    String errorBody = readErrorStream(conn);
                    log.error("DeepSeek API返回错误: code={}, body={}", responseCode, errorBody);
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(JSON.toJSONString(new SseData("AI服务暂时不可用(HTTP " + responseCode + ")，请稍后重试", "error"))));
                    emitter.complete();
                    return;
                }

                // 读取SSE流并转发给前端
                StringBuilder fullAnswer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;

                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            Map<String, Object> chunk = JSON.parseObject(data, Map.class);
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices == null || choices.isEmpty()) continue;

                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta == null || !delta.containsKey("content")) continue;

                            String content = (String) delta.get("content");
                            if (content != null && !content.isEmpty()) {
                                fullAnswer.append(content);
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(JSON.toJSONString(new SseData(content, "text"))));
                            }
                        } catch (Exception parseEx) {
                            log.warn("解析DeepSeek流数据失败: {}", parseEx.getMessage());
                        }
                    }
                }

                // 保存对话记录（先保存以获得自增ID）
                Long conversationId = null;
                if (!fullAnswer.isEmpty()) {
                    AiConversation conversation = new AiConversation();
                    conversation.setUserId(userId);
                    conversation.setSessionId(finalSessionId);
                    conversation.setQuestion(dto.getMessage());
                    conversation.setAnswer(fullAnswer.toString());
                    conversation.setIntent(intent);
                    conversation.setTokensUsed(fullAnswer.length());
                    conversation.setFeedback(0);
                    aiConversationMapper.insert(conversation);
                    conversationId = conversation.getId();
                }

                // 根据AI回复内容修正意图（AI可能在回复中建议购票）
                String finalIntent = refineIntentByAnswer(fullAnswer.toString(), intent);
                if (!finalIntent.equals(intent)) {
                    log.info("意图已修正: {} -> {} (基于AI回复)", intent, finalIntent);
                }

                // 发送完成信号（附带sessionId、conversationId、intent供前端处理）
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(JSON.toJSONString(Map.of(
                                "type", "done",
                                "delta", "[DONE]",
                                "sessionId", finalSessionId,
                                "conversationId", conversationId,
                                "intent", finalIntent
                        ))));

                emitter.complete();

            } catch (org.apache.catalina.connector.ClientAbortException e) {
                log.info("AI对话客户端断开连接: userId={}, session={}", userId, finalSessionId);
                emitter.complete();
            } catch (Exception e) {
                log.error("AI对话异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(JSON.toJSONString(new SseData("AI服务暂时不可用，请稍后重试", "error"))));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                SecurityContextHolder.clearContext();
                if (conn != null) conn.disconnect();
            }
        }).start();

        return emitter;
    }

    private String readErrorStream(HttpURLConnection conn) throws IOException {
        InputStream errStream = conn.getErrorStream();
        if (errStream == null) return "(无错误详情)";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你叫\"小景\"，是").append(scenicProperties.getName()).append("的智能助手。\n");
        prompt.append("你有三大核心能力：\n");
        prompt.append("1. 💬 咨询问答：回答关于景区开放时间、票价、交通、注意事项等问题\n");
        prompt.append("2. 🛒 辅助购票：帮助游客选择合适票种和日期，引导完成购票\n");
        prompt.append("3. 🗺️ 景区导览：推荐游玩路线、介绍景点、查询设施位置\n\n");

        prompt.append("景区信息：\n");
        prompt.append("- 名称：").append(scenicProperties.getName()).append("\n");
        prompt.append("- 地址：").append(scenicProperties.getAddress()).append("\n");
        prompt.append("- 简介：").append(scenicProperties.getDescription()).append("\n");
        String openTime = scenicProperties.getOpenTime();
        String closeTime = scenicProperties.getCloseTime();
        prompt.append("- 开放时间：").append(openTime).append("-").append(closeTime).append("\n");
        prompt.append("- 最大承载量：").append(scenicProperties.getMaxCapacity()).append("人/天\n\n");

        // 票种信息
        List<TicketType> ticketTypes = ticketTypeMapper.selectList(
                new LambdaQueryWrapper<TicketType>().eq(TicketType::getStatus, 1)
        );
        if (!ticketTypes.isEmpty()) {
            prompt.append("在售票种：\n");
            for (TicketType t : ticketTypes) {
                prompt.append("- ").append(t.getName()).append("：¥").append(t.getPrice())
                        .append("（").append(t.getDescription() != null ? t.getDescription() : "").append("）\n");
            }
            prompt.append("\n");
        }

        // 景点信息
        List<ScenicSpot> spots = scenicSpotMapper.selectList(
                new LambdaQueryWrapper<ScenicSpot>().eq(ScenicSpot::getStatus, 1)
                        .orderByAsc(ScenicSpot::getSortOrder)
        );
        if (!spots.isEmpty()) {
            prompt.append("景区景点：\n");
            for (ScenicSpot spot : spots) {
                prompt.append("- ").append(spot.getName()).append("：")
                        .append(spot.getDescription() != null ? spot.getDescription() : "").append("\n");
            }
            prompt.append("\n");
        }

        // 设施信息
        List<ScenicFacility> facilities = scenicFacilityMapper.selectList(null);
        if (!facilities.isEmpty()) {
            prompt.append("景区设施：\n");
            for (ScenicFacility f : facilities) {
                prompt.append("- ").append(getFacilityTypeName(f.getType()))
                        .append("：").append(f.getName()).append("（").append(f.getDescription()).append("）\n");
            }
            prompt.append("\n");
        }

        prompt.append("【购票引导规范 —— 严格按以下步骤引导，每次只问一个问题】：\n");
        prompt.append("当用户表达购票意愿时，按以下步骤逐步引导（切勿一次性问完所有问题）：\n");
        prompt.append("第1步：先询问游览日期。例如：\"请问您计划哪天来游玩呢？\"\n");
        prompt.append("第2步：用户提供日期后，介绍票种并询问需要哪种。例如：\"我们有以下票种可供选择...请问您需要哪种票呢？\"\n");
        prompt.append("第3步：用户选择票种后，询问购买数量。例如：\"好的，请问需要购买几张呢？\"\n");
        prompt.append("第4步：用户确定数量后，汇总购物车信息让用户确认。格式：\"📋 购物车确认：\\n游览日期：xxx\\n票种及数量：xxx x张，小计¥xxx\\n合计：¥xxx\\n请确认以上信息是否正确？\"\n");
        prompt.append("第5步：用户确认后，告知：\"✅ 订单已确认！请在右侧面板完成支付。\"\n");
        prompt.append("第6步：支付完成后，提醒用户：\"⚡ 支付成功！请记得为每张门票录入人脸信息，游览当天可刷脸入园哦～\"\n");
        prompt.append("第7步：用户完成所有步骤后：\"🎉 恭喜您完成所有步骤！祝您在").append(scenicProperties.getName()).append("游玩愉快！\"\n");
        prompt.append("重要：在第1~4步引导过程中，严禁添加任何形式的\"请点击下方\"、\"前往购票\"、\"点击按钮\"、\"点击下方\"等引导用户操作界面元素的提示语。这些步骤的操作入口已在右侧面板展示，任何此类提示都是多余的。\n\n");

        prompt.append("兜底规则：仅当用户咨询票价、如何购票等一般性问题（而非真正进入购票流程）时，才可以在回复末尾添加：\"💡 请点击下方「前往购票」按钮直达购票页面\"。\n");
        prompt.append("当用户询问路线/地图/景点时，应在回复中介绍景点和路线，末尾告知：\"💡 请点击下方「🗺️ 查看景区地图」按钮查看各景点位置分布\"。\n");
        prompt.append("不要在回复中使用Markdown链接（如[查看地图](#)），这些链接无法被点击，始终引导用户使用下方的操作按钮。\n");
        prompt.append("请保持友好、专业的语气，回复简洁明了。\n");

        return prompt.toString();
    }

    private String detectIntent(String message) {
        // 购票意图关键词（注意：要覆盖单字"买"以及词组）
        if (containsAny(message, "购票", "买票", "买", "门票", "多少钱", "票价", "价格", "购买", "下单", "支付")) {
            return "buy_ticket";
        }
        // 导览意图关键词
        if (containsAny(message, "路线", "游玩", "卫生间", "设施", "地图", "景点",
                "导览", "导航", "怎么去", "在哪", "洗手间", "停车场", "在哪里",
                "推荐", "一日游", "半日游", "游览", "攻略", "玩什么", "好玩",
                "好看的", "特色", "必去", "打卡", "有什么", "有哪些", "好去处")) {
            return "guide";
        }
        return "consult";
    }

    /**
     * 根据AI回复内容修正意图 —— 当AI主动建议用户购票时，即使原意图是guide/consult也改为buy_ticket
     */
    private String refineIntentByAnswer(String answer, String originalIntent) {
        if ("buy_ticket".equals(originalIntent)) return originalIntent;
        // AI回复中明确提到购票引导（必须是"购票"语义，不能和"地图"混淆）
        if (containsAny(answer, "前往购票", "立即购票", "购票页面", "购买门票", "引导购票", "辅助购票")) {
            return "buy_ticket";
        }
        return originalIntent;
    }

    private boolean containsAny(String message, String... keywords) {
        for (String kw : keywords) {
            if (message != null && message.contains(kw)) return true;
        }
        return false;
    }

    private String getFacilityTypeName(Integer type) {
        return switch (type) {
            case 1 -> "卫生间";
            case 2 -> "餐饮";
            case 3 -> "停车场";
            case 4 -> "医疗";
            case 5 -> "游客中心";
            default -> "其他";
        };
    }

    @Override
    public void updateFeedback(Long conversationId, Long userId, Integer feedback) {
        AiConversation conv = aiConversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("对话记录不存在");
        }
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此对话记录");
        }
        if (feedback == null || feedback < 0 || feedback > 2) {
            throw new BusinessException("无效的评价类型");
        }
        conv.setFeedback(feedback);
        aiConversationMapper.updateById(conv);
    }

    @Override
    public SseEmitter generateTravelogue(Long userId, TravelogueDTO dto) {
        SseEmitter emitter = new SseEmitter(300000L);

        // 查询订单信息
        TicketOrder order = ticketOrderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, dto.getOrderNo()));
        if (order == null || !order.getUserId().equals(userId)) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data("{\"delta\":\"订单不存在或无权访问\",\"type\":\"error\"}"));
                emitter.complete();
            } catch (IOException e) { emitter.completeWithError(e); }
            return emitter;
        }

        // 查询订单项
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        StringBuilder ticketInfo = new StringBuilder();
        for (OrderItem item : items) {
            TicketType tt = ticketTypeMapper.selectById(item.getTicketTypeId());
            ticketInfo.append("- ").append(tt != null ? tt.getName() : "门票")
                    .append(" ×").append(item.getQuantity()).append("张\n");
        }

        // 查询入园记录
        EntryLog entryLog = entryLogMapper.selectOne(
                new LambdaQueryWrapper<EntryLog>().eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getUserId, userId).orderByAsc(EntryLog::getEntryTime).last("LIMIT 1"));
        String entryTimeStr = entryLog != null && entryLog.getEntryTime() != null
                ? entryLog.getEntryTime().toString() : "未记录";

        // 查询景点列表
        List<ScenicSpot> spots = scenicSpotMapper.selectList(
                new LambdaQueryWrapper<ScenicSpot>().eq(ScenicSpot::getStatus, 1).orderByAsc(ScenicSpot::getSortOrder));
        StringBuilder spotInfo = new StringBuilder();
        for (ScenicSpot s : spots) {
            spotInfo.append("- ").append(s.getName()).append("：")
                    .append(s.getDescription() != null ? s.getDescription() : "").append("\n");
        }

        // 构建游记生成的System Prompt
        String style = dto.getStyle() != null ? dto.getStyle() : "literary";
        String styleDesc = switch (style) {
            case "humor" -> "用幽默风趣的口吻，加入一些俏皮话和网络热梗";
            case "simple" -> "用简洁明了的风格，条理清晰";
            default -> "用优美文艺的笔触，富有诗意和画面感";
        };

        String systemPrompt = "你是一位才华横溢的旅行作家，现在需要为一位游客撰写一篇" + scenicProperties.getName() + "的游记。\n\n"
                + "游记写作要求：\n"
                + "1. " + styleDesc + "\n"
                + "2. 游记需要包含：标题、游览概况、景点体验、美食推荐、实用贴士、结语\n"
                + "3. 字数控制在500-800字\n"
                + "4. 使用📝🎒🗺️✨🌟等相关emoji点缀\n"
                + "5. 用Markdown格式排版，包含二级标题\n\n"
                + "游客游览信息：\n"
                + "- 景区名称：" + scenicProperties.getName() + "\n"
                + "- 游览日期：" + order.getVisitDate() + "\n"
                + "- 购买票种：\n" + ticketInfo
                + "- 入园时间：" + entryTimeStr + "\n"
                + "- 门票总金额：¥" + order.getTotalAmount() + "\n\n"
                + "景区景点信息（供参考）：\n" + spotInfo + "\n"
                + "请以第一人称游客视角（\"我\"）来写这篇游记，让读者仿佛身临其境。";

        SecurityContext securityContext = SecurityContextHolder.getContext();

        new Thread(() -> {
            SecurityContextHolder.setContext(securityContext);
            HttpURLConnection conn = null;
            try {
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", systemPrompt));
                messages.add(Map.of("role", "user", "content", "请根据以上信息，为我生成一篇游览" + scenicProperties.getName() + "的游记。"));

                Map<String, Object> requestBody = Map.of(
                        "model", deepSeekProperties.getModel(),
                        "messages", messages,
                        "stream", true,
                        "max_tokens", deepSeekProperties.getMaxTokens(),
                        "temperature", 0.8
                );

                String jsonBody = JSON.toJSONString(requestBody);
                URI apiUri = URI.create(deepSeekProperties.getBaseUrl());
                conn = (HttpURLConnection) apiUri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + deepSeekProperties.getApiKey());
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(300000);
                conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
                conn.getOutputStream().flush();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"delta\":\"AI服务暂时不可用，请稍后重试\",\"type\":\"error\"}"));
                    emitter.complete();
                    return;
                }

                StringBuilder fullAnswer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;
                        try {
                            Map<String, Object> chunk = JSON.parseObject(data, Map.class);
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices == null || choices.isEmpty()) continue;
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta == null || !delta.containsKey("content")) continue;
                            String content = (String) delta.get("content");
                            if (content != null && !content.isEmpty()) {
                                fullAnswer.append(content);
                                emitter.send(SseEmitter.event().name("message")
                                        .data(JSON.toJSONString(new SseData(content, "text"))));
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (!fullAnswer.isEmpty()) {
                    AiConversation conversation = new AiConversation();
                    conversation.setUserId(userId);
                    conversation.setSessionId("travelogue_" + dto.getOrderNo());
                    conversation.setQuestion("生成游记 - 订单" + dto.getOrderNo());
                    conversation.setAnswer(fullAnswer.toString());
                    conversation.setIntent("travelogue");
                    conversation.setTokensUsed(fullAnswer.length());
                    conversation.setFeedback(0);
                    aiConversationMapper.insert(conversation);
                }

                emitter.send(SseEmitter.event().name("done")
                        .data(JSON.toJSONString(Map.of("type", "done", "delta", "[DONE]"))));
                emitter.complete();

            } catch (Exception e) {
                log.error("AI游记生成异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"delta\":\"AI服务暂时不可用，请稍后重试\",\"type\":\"error\"}"));
                    emitter.complete();
                } catch (IOException ex) { emitter.completeWithError(ex); }
            } finally {
                SecurityContextHolder.clearContext();
                if (conn != null) conn.disconnect();
            }
        }).start();

        return emitter;
    }

    // ==================== 游记下载（Word） ====================

    @Override
    public byte[] downloadTravelogue(TravelogueDownloadDTO dto) {
        if ("word".equals(dto.getFormat())) {
            return markdownToWord(dto.getContent());
        }
        throw new BusinessException("不支持的导出格式: " + dto.getFormat());
    }

    /** Markdown → Word (.docx) */
    private byte[] markdownToWord(String markdown) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("景区游记");
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("微软雅黑");
            titleRun.setColor("1a1a2e");
            titlePara.setSpacingAfter(400);

            // 逐行解析 markdown
            String[] lines = markdown.split("\n");
            XWPFParagraph currentPara = null;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    currentPara = null;
                    continue;
                }
                // 标题
                if (trimmed.startsWith("### ")) {
                    currentPara = doc.createParagraph();
                    setRun(currentPara.createRun(), trimmed.substring(4), true, 14, "2c3e50");
                    currentPara.setSpacingBefore(200);
                } else if (trimmed.startsWith("## ")) {
                    currentPara = doc.createParagraph();
                    setRun(currentPara.createRun(), trimmed.substring(3), true, 16, "2c3e50");
                    currentPara.setSpacingBefore(300);
                    currentPara.setSpacingAfter(100);
                    addBottomBorder(currentPara);
                } else if (trimmed.startsWith("# ")) {
                    currentPara = doc.createParagraph();
                    setRun(currentPara.createRun(), trimmed.substring(2), true, 18, "1a1a2e");
                    currentPara.setAlignment(ParagraphAlignment.CENTER);
                    currentPara.setSpacingBefore(200);
                    currentPara.setSpacingAfter(200);
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    // 无序列表
                    currentPara = doc.createParagraph();
                    currentPara.setIndentationLeft(400);
                    String text = parseInlineFormatting(trimmed.substring(2));
                    addFormattedRun(currentPara, text, false, 12, "333333");
                } else if (trimmed.matches("^\\d+\\.\\s.*")) {
                    // 有序列表
                    currentPara = doc.createParagraph();
                    currentPara.setIndentationLeft(400);
                    String text = parseInlineFormatting(trimmed.replaceFirst("^\\d+\\.\\s", ""));
                    addFormattedRun(currentPara, text, false, 12, "333333");
                } else if (trimmed.startsWith("> ")) {
                    // 引用
                    currentPara = doc.createParagraph();
                    currentPara.setIndentationLeft(300);
                    XWPFRun run = currentPara.createRun();
                    run.setText(trimmed.substring(2));
                    run.setItalic(true);
                    run.setFontSize(12);
                    run.setColor("555555");
                    currentPara.setSpacingAfter(60);
                } else if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                    // 分隔线
                    currentPara = doc.createParagraph();
                    XWPFRun hrRun = currentPara.createRun();
                    hrRun.setText("─".repeat(40));
                    hrRun.setColor("cccccc");
                    hrRun.setFontSize(8);
                    currentPara.setAlignment(ParagraphAlignment.CENTER);
                } else {
                    // 普通段落
                    currentPara = doc.createParagraph();
                    currentPara.setIndentationFirstLine(480); // 首行缩进2字符
                    String text = parseInlineFormatting(trimmed);
                    addFormattedRun(currentPara, text, false, 12, "333333");
                    currentPara.setSpacingAfter(60);
                }
            }

            // 页脚
            XWPFParagraph footerPara = doc.createParagraph();
            footerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun footerRun = footerPara.createRun();
            footerRun.setText("由 SmartScenic AI 生成");
            footerRun.setFontSize(10);
            footerRun.setColor("999999");
            footerPara.setSpacingBefore(400);

            doc.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Word生成失败", e);
            throw new BusinessException("Word生成失败: " + e.getMessage());
        }
    }

    private void setRun(XWPFRun run, String text, boolean bold, int fontSize, String color) {
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("微软雅黑");
        run.setColor(color);
    }

    private void addBottomBorder(XWPFParagraph para) {
        para.setBorderBottom(Borders.SINGLE);
    }

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");

    private void addFormattedRun(XWPFParagraph para, String text, boolean bold, int fontSize, String color) {
        // 简单处理：先处理 **粗体**，再处理 *斜体*，最后处理 `代码`
        int lastEnd = 0;
        // 这里用简化方式：对整个文本进行粗体/斜体标记分割
        java.util.List<TextSegment> segments = parseTextSegments(text);
        if (segments.isEmpty()) {
            setRun(para.createRun(), text, bold, fontSize, color);
        } else {
            for (TextSegment seg : segments) {
                setRun(para.createRun(), seg.text, seg.bold || bold, 
                        seg.bold ? fontSize + 2 : fontSize, 
                        seg.code ? "e74c3c" : color);
                if (seg.italic && !seg.bold) {
                    // 斜体
                    XWPFRun lastRun = para.getRuns().get(para.getRuns().size() - 1);
                    lastRun.setItalic(true);
                }
            }
        }
    }

    private java.util.List<TextSegment> parseTextSegments(String text) {
        java.util.List<TextSegment> result = new java.util.ArrayList<>();
        java.util.regex.Matcher m = Pattern.compile("(\\*\\*.+?\\*\\*)|((?<!\\*)\\*(?!\\*).+?(?<!\\*)\\*(?!\\*))|(`.+?`)").matcher(text);
        int lastEnd = 0;
        while (m.find()) {
            if (m.start() > lastEnd) {
                result.add(new TextSegment(text.substring(lastEnd, m.start()), false, false, false));
            }
            String group = m.group();
            if (group.startsWith("**") && group.endsWith("**")) {
                result.add(new TextSegment(group.substring(2, group.length() - 2), true, false, false));
            } else if (group.startsWith("`") && group.endsWith("`")) {
                result.add(new TextSegment(group.substring(1, group.length() - 1), false, false, true));
            } else if (group.startsWith("*") && group.endsWith("*")) {
                result.add(new TextSegment(group.substring(1, group.length() - 1), false, true, false));
            } else {
                result.add(new TextSegment(group, false, false, false));
            }
            lastEnd = m.end();
        }
        if (lastEnd < text.length()) {
            result.add(new TextSegment(text.substring(lastEnd), false, false, false));
        }
        return result;
    }

    private String parseInlineFormatting(String text) {
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                   .replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1")
                   .replaceAll("`(.+?)`", "$1");
    }

    @lombok.AllArgsConstructor
    private static class TextSegment {
        String text;
        boolean bold;
        boolean italic;
        boolean code;
    }

    @lombok.AllArgsConstructor
    @lombok.Data
    public static class SseData {
        private String delta;
        private String type;
    }
}
