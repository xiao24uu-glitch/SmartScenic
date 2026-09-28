package com.scenic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scenic.client.BaiduFaceApiClient;
import com.scenic.common.exception.BusinessException;
import com.scenic.config.BaiduFaceProperties;
import com.scenic.config.LocalCacheStore;
import com.scenic.dto.FaceEntryDTO;
import com.scenic.dto.FaceRegisterDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.service.FaceService;
import com.scenic.vo.FaceEntryVO;
import com.scenic.vo.GroupFaceProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceServiceImpl implements FaceService {

    private final FaceDataMapper faceDataMapper;
    private final TouristMapper touristMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final EntryLogMapper entryLogMapper;
    private final SysRoleMapper sysRoleMapper;
    private final GroupOrderMapper groupOrderMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final BaiduFaceProperties baiduFaceProperties;
    private final BaiduFaceApiClient baiduFaceApiClient;
    private final LocalCacheStore cacheStore;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    /** 人脸库分组前缀 */
    private static final String GROUP_PREFIX = "scenic";
    /** 默认分组（兼容旧数据） */
    private static final String DEFAULT_GROUP = GROUP_PREFIX + "_default";
    /** 内部通道分组（管理员/检票员日常通行用） */
    private static final String INTERNAL_GROUP = GROUP_PREFIX + "_internal";
    /** 百度 user_id 前缀 */
    private static final String USER_ID_PREFIX = "tourist";

    @Override
    @Transactional
    public void registerFace(Long userId, FaceRegisterDTO dto) {
        // 查找游客
        Tourist tourist = touristMapper.selectOne(
                new LambdaQueryWrapper<Tourist>().eq(Tourist::getUserId, userId)
        );
        if (tourist == null) {
            SysUser sysUser = sysUserMapper.selectById(userId);
            if (sysUser == null) {
                throw new BusinessException("用户信息不存在");
            }
            tourist = new Tourist();
            tourist.setUserId(userId);
            tourist.setRealName(sysUser.getRealName() != null ? sysUser.getRealName() : sysUser.getUsername());
            tourist.setPhone(sysUser.getPhone());
            tourist.setFaceStatus(0);
            touristMapper.insert(tourist);
            log.info("自动创建游客记录: userId={}, touristId={}", userId, tourist.getId());
        }

        String groupId;
        LocalDateTime expireTime = null;
        Long orderId = null;
        boolean isTicketFace = StrUtil.isNotBlank(dto.getOrderNo());

        if (isTicketFace) {
            // ===== 票务人脸：关联订单 =====
            TicketOrder order = orderMapper.selectOne(
                    new LambdaQueryWrapper<TicketOrder>()
                            .eq(TicketOrder::getOrderNo, dto.getOrderNo())
                            .eq(TicketOrder::getUserId, userId)
            );
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            if (order.getStatus() != 1) {
                throw new BusinessException("订单未支付，无法录入人脸");
            }
            // 检查游览日期是否已过期
            if (order.getVisitDate() != null && order.getVisitDate().isBefore(LocalDate.now())) {
                throw new BusinessException("该订单游览日期已过期（" + order.getVisitDate() + "），无法录入人脸");
            }
            groupId = buildGroupId(order.getVisitDate());
            expireTime = order.getVisitDate().plusDays(1).atTime(23, 59, 59);
            orderId = order.getId();
        } else {
            // ===== 内部通道人脸：不关联订单，仅限管理员/检票员 =====
            List<SysRole> roles = sysRoleMapper.findRolesByUserId(userId);
            boolean isInternalUser = roles.stream().anyMatch(r ->
                    r.getRoleLevel() != null && r.getRoleLevel() <= 3); // ADMIN=1, MANAGER=2, CHECKER=3
            if (!isInternalUser) {
                throw new BusinessException("游客请在订单支付后录入人脸");
            }
            groupId = INTERNAL_GROUP;
            // 内部通道人脸不过期
        }

        // 查找已有的人脸（内部通道人脸按用户覆盖；票务人脸传faceId则替换，否则新增不覆盖）
        FaceData existingFace = null;
        if (!isTicketFace) {
            // 内部通道人脸：始终覆盖（一个用户只有一张内部通道人脸）
            existingFace = faceDataMapper.selectOne(
                    new LambdaQueryWrapper<FaceData>()
                            .eq(FaceData::getUserId, userId)
                            .isNull(FaceData::getOrderId)
                            .eq(FaceData::getStatus, 1)
                            .orderByDesc(FaceData::getId)
                            .last("LIMIT 1")
            );
        } else if (dto.getFaceId() != null) {
            // 票务人脸：指定了faceId，替换该人脸
            existingFace = faceDataMapper.selectById(dto.getFaceId());
            if (existingFace != null && (!existingFace.getUserId().equals(userId) || !existingFace.getOrderId().equals(orderId))) {
                existingFace = null; // 安全检查不通过，不替换
            }
        }
        // 票务人脸且未传faceId：直接新增，不覆盖已有

        String baiduUserId = buildUserId(tourist.getId(), orderId);

        // ===== 清理百度云端该分组下该游客的旧格式人脸 =====
        // 旧代码用 touristId 作为 userId（如 tourist_123），百度库中可能残留旧数据
        // 搜索时百度可能匹配到旧数据导致返回错误的 userId，造成匹配错人
        // 这里先清理掉该 touristId 在该分组下的旧格式+新格式残留
        try {
            String oldUserId = USER_ID_PREFIX + "_" + tourist.getId();
            baiduFaceApiClient.deleteFaceByUserId(oldUserId, groupId);
        } catch (Exception e) {
            log.warn("清理旧百度人脸失败: touristId={}, groupId={}", tourist.getId(), groupId, e);
        }
        // 同时清理该 baiduUserId 可能的历史残留（极端情况）
        try {
            baiduFaceApiClient.deleteFaceByUserId(baiduUserId, groupId);
        } catch (Exception e) {
            log.warn("清理历史百度人脸失败: baiduUserId={}, groupId={}", baiduUserId, groupId, e);
        }

        // 调用百度API进行人脸注册
        BaiduFaceApiClient.FaceRegisterResult apiResult;
        try {
            apiResult = baiduFaceApiClient.registerFace(dto.getImageBase64(), groupId, baiduUserId);
        } catch (Exception e) {
            log.error("百度人脸注册失败: userId={}", userId, e);
            throw new BusinessException("人脸注册失败: " + e.getMessage());
        }

        // 保存人脸图片到本地（使用真实姓名生成可读文件名）
        String faceImagePath = saveFaceImage(dto.getImageBase64(), apiResult.getFaceToken(), dto.getRealName());

        // 获取人脸质量分
        Double qualityScore = apiResult.getQualityScore();
        if (qualityScore == null) {
            try {
                qualityScore = baiduFaceApiClient.detectFaceQuality(dto.getImageBase64());
            } catch (Exception e) {
                log.warn("获取人脸质量分失败", e);
            }
        }

        // 如有旧人脸，先物理删除并从百度删除
        if (existingFace != null) {
            try {
                if (StrUtil.isNotBlank(existingFace.getBaiduFaceToken())
                        && StrUtil.isNotBlank(existingFace.getBaiduGroupId())) {
                    baiduFaceApiClient.deleteFace(existingFace.getBaiduFaceToken(), existingFace.getBaiduGroupId());
                }
            } catch (Exception e) {
                log.warn("删除旧百度人脸失败: {}", existingFace.getBaiduFaceToken(), e);
            }
            try {
                if (StrUtil.isNotBlank(existingFace.getFaceImagePath())) {
                    Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                    Path oldImagePath = basePath.resolve(
                            existingFace.getFaceImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(oldImagePath);
                }
            } catch (Exception e) {
                log.warn("删除旧人脸图片失败: {}", existingFace.getFaceImagePath(), e);
            }
            faceDataMapper.deleteById(existingFace.getId());
        }

        // 保存新的人脸数据
        FaceData faceData = new FaceData();
        faceData.setTouristId(tourist.getId());
        faceData.setUserId(userId);
        faceData.setOrderId(orderId);
        faceData.setRealName(dto.getRealName());
        faceData.setPhone(dto.getPhone());
        faceData.setBaiduFaceToken(apiResult.getFaceToken());
        faceData.setBaiduUserId(baiduUserId);
        faceData.setBaiduGroupId(groupId);
        faceData.setFaceImagePath(faceImagePath);
        faceData.setQualityScore(qualityScore != null
                ? BigDecimal.valueOf(qualityScore).setScale(2, java.math.RoundingMode.HALF_UP) : null);
        faceData.setExpireTime(expireTime); // 内部通道人脸为 null
        faceData.setStatus(1);
        faceDataMapper.insert(faceData);

        log.info("人脸注册成功: userId={}, touristId={}, baiduUserId={}, realName={}, faceToken={}, groupId={}, orderId={}, orderNo={}, isTicketFace={}",
                userId, tourist.getId(), baiduUserId, dto.getRealName(),
                apiResult.getFaceToken(), groupId, orderId, dto.getOrderNo(), isTicketFace);

        // 更新游客人脸状态
        tourist.setFaceStatus(1);
        tourist.setBaiduFaceToken(apiResult.getFaceToken());
        touristMapper.updateById(tourist);
    }

    @Override
    @Transactional
    public FaceEntryVO faceEntry(FaceEntryDTO dto) {
        LocalDate today = LocalDate.now();

        log.info("人脸核验请求: imageBase64长度={}, gateNo={}",
                dto.getImageBase64() != null ? dto.getImageBase64().length() : 0, dto.getGateNo());

        // 先保存闸机抓拍照片（无论成功失败都保存）
        String captureImagePath = null;
        if (StrUtil.isNotBlank(dto.getImageBase64())) {
            try {
                captureImagePath = saveFaceImage(dto.getImageBase64(),
                        "capture_" + System.currentTimeMillis(), null);
            } catch (Exception e) {
                log.warn("保存闸机抓拍照片失败", e);
            }
        }

        // ========== 第一步：先搜索票务人脸分组（当天 + 前一天） ==========
        String todayGroup = buildGroupId(today);
        String yesterdayGroup = buildGroupId(today.minusDays(1));
        String ticketGroupList = yesterdayGroup + "," + todayGroup;

        BaiduFaceApiClient.FaceSearchResult searchResult = searchFaceInGroups(dto.getImageBase64(), ticketGroupList);

        // ========== 第二步：如果没匹配到票务人脸，搜索内部通道 + 历史兼容分组 ==========
        if (searchResult == null || !searchResult.isMatched()) {
            String fallbackGroupList = INTERNAL_GROUP + "," + DEFAULT_GROUP;
            searchResult = searchFaceInGroups(dto.getImageBase64(), fallbackGroupList);
            // 不在此时统一标记 isInternalFace，后续根据 matchedFace 的 orderId 精确判断
        }

        // 未匹配到任何人脸
        if (searchResult == null || !searchResult.isMatched()) {
            String failMsg = searchResult != null ? searchResult.getFailedMessage() : "未找到匹配的人脸";
            if ("未检测到人脸".equals(failMsg)) {
                recordEntryLog(null, null, null, null,
                        BigDecimal.ZERO, dto.getGateNo(), false, failMsg, captureImagePath);
                return FaceEntryVO.builder()
                        .success(false)
                        .score(BigDecimal.ZERO)
                        .message("未检测到人脸，请正对摄像头后重试")
                        .gateNo(dto.getGateNo())
                        .build();
            } else {
                recordEntryLog(null, null, null, null,
                        BigDecimal.ZERO, dto.getGateNo(), false,
                        StrUtil.isNotBlank(failMsg) ? failMsg : "人脸比对未通过", captureImagePath);
                return FaceEntryVO.builder()
                        .success(false)
                        .score(BigDecimal.ZERO)
                        .message(StrUtil.isNotBlank(failMsg) ? failMsg : "人脸未录入系统，请先注册人脸信息")
                        .gateNo(dto.getGateNo())
                        .build();
            }
        }

        // 从 user_id 中解析 touristId
        Long touristId = parseTouristId(searchResult.getUserId());
        if (touristId == null) {
            log.error("无法解析百度返回的 userId: {}", searchResult.getUserId());
            return FaceEntryVO.builder()
                    .success(false)
                    .score(BigDecimal.valueOf(searchResult.getScore()))
                    .message("人脸识别失败，用户信息异常")
                    .gateNo(dto.getGateNo())
                    .build();
        }

        // 用百度返回的唯一 user_id 精确匹配 FaceData
        // 不能用 touristId 取最新，因为同一用户可能为多人买票（同一touristId下有多张不同人的FaceData）
        log.info("人脸搜索匹配开始: searchUserId={}, score={}, group={}",
                searchResult.getUserId(), searchResult.getScore(), searchResult.getGroupId());

        FaceData matchedFace = null;
        if (StrUtil.isNotBlank(searchResult.getUserId())) {
            matchedFace = faceDataMapper.selectOne(
                    new LambdaQueryWrapper<FaceData>()
                            .eq(FaceData::getBaiduUserId, searchResult.getUserId())
                            .eq(FaceData::getStatus, 1)
            );
            log.info("baiduUserId 精确匹配结果: {}", matchedFace != null
                    ? "faceDataId=" + matchedFace.getId() + ", realName=" + matchedFace.getRealName() + ", orderId=" + matchedFace.getOrderId()
                    : "未匹配到");
        }
        // 兼容旧数据：如果 baiduUserId 没匹配到，回退按 touristId 查找（旧 FaceData 没有 baiduUserId）
        if (matchedFace == null) {
            log.warn("baiduUserId 匹配失败，回退到 touristId 查找: touristId={}", touristId);
            matchedFace = faceDataMapper.selectOne(
                    new LambdaQueryWrapper<FaceData>()
                            .eq(FaceData::getTouristId, touristId)
                            .eq(FaceData::getStatus, 1)
                            .orderByDesc(FaceData::getId)
                            .last("LIMIT 1")
            );
            log.info("touristId 回退匹配结果: {}", matchedFace != null
                    ? "faceDataId=" + matchedFace.getId() + ", realName=" + matchedFace.getRealName() + ", orderId=" + matchedFace.getOrderId() + ", baiduUserId=" + matchedFace.getBaiduUserId()
                    : "未匹配到");
        }
        if (matchedFace == null) {
            log.warn("人脸匹配到但数据库无记录: touristId={}, faceToken={}", touristId, searchResult.getFaceToken());
            return FaceEntryVO.builder()
                    .success(false)
                    .score(BigDecimal.valueOf(searchResult.getScore()))
                    .message("人脸未在系统内注册，请先录入人脸")
                    .gateNo(dto.getGateNo())
                    .build();
        }

        BigDecimal score = BigDecimal.valueOf(searchResult.getScore());

        // ========== 内部通道人脸：直接放行（不检查订单） ==========
        // 内部通道人脸 orderId 为 null，票务人脸 orderId 不为 null
        if (matchedFace.getOrderId() == null) {
            recordEntryLog(null, null, touristId, matchedFace, score, dto.getGateNo(), true, null, captureImagePath);

            // 使用匹配到的 FaceData 中的 realName，这才是真正的姓名
            String userName = matchedFace.getRealName() != null ? matchedFace.getRealName() : "员工";

            log.info("内部通道人脸核验成功: userId={}, touristId={}, faceDataId={}, score={}, gateNo={}",
                    matchedFace.getUserId(), touristId, matchedFace.getId(), score, dto.getGateNo());

            return FaceEntryVO.builder()
                    .success(true)
                    .score(score)
                    .message("内部通道验证通过，欢迎入园！")
                    .userName(userName)
                    .action("ENTRY")
                    .entryTime(LocalDateTime.now())
                    .gateNo(dto.getGateNo())
                    .captureImagePath(captureImagePath)
                    .build();
        }

        // ========== 票务人脸：按订单检票逻辑 ==========
        Long orderId = matchedFace.getOrderId();
        TicketOrder validOrder = orderMapper.selectById(orderId);

        log.info("票务检票: faceDataId={}, realName={}, orderId={}, validOrder={}",
                matchedFace.getId(), matchedFace.getRealName(), orderId,
                validOrder != null ? validOrder.getId() + "/" + validOrder.getOrderNo() : "null");

        // 兼容旧数据：如果 FaceData 没有 orderId，回退到按 userId + visitDate 查找
        if (validOrder == null) {
            validOrder = orderMapper.selectList(
                    new LambdaQueryWrapper<TicketOrder>()
                            .eq(TicketOrder::getUserId, matchedFace.getUserId())
                            .eq(TicketOrder::getVisitDate, today)
                            .eq(TicketOrder::getStatus, 1)
            ).stream().findFirst().orElse(null);
        }

        if (validOrder == null) {
            try {
                recordEntryLog(null, null, touristId, matchedFace, score, dto.getGateNo(), false,
                        "人脸验证成功但未找到有效订单", captureImagePath);
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
            }
            return FaceEntryVO.builder()
                    .success(false)
                    .score(score)
                    .message("人脸验证成功，但未找到今日有效订单（请先购票并录入人脸）")
                    .gateNo(dto.getGateNo())
                    .build();
        }

        // ========== 先查入园记录，判断出园/在园状态 ==========
        EntryLog existingEntry = entryLogMapper.selectOne(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, validOrder.getId())
                        .eq(EntryLog::getFaceDataId, matchedFace.getId())
                        .eq(EntryLog::getStatus, 1)
        );

        // 情况1：已出园 → 友好拒绝（优先于状态检查）
        if (existingEntry != null && existingEntry.getExitTime() != null) {
            log.warn("已出园游客再次扫脸被拦截: orderId={}, faceDataId={}, realName={}, exitTime={}",
                    validOrder.getId(), matchedFace.getId(), matchedFace.getRealName(), existingEntry.getExitTime());
            return FaceEntryVO.builder()
                    .success(false)
                    .score(score)
                    .message("该门票已出园，无法再次入园")
                    .gateNo(dto.getGateNo())
                    .captureImagePath(captureImagePath)
                    .build();
        }

        // 情况2：已在园内 → 出园
        if (existingEntry != null && existingEntry.getExitTime() == null) {
            return handleFaceExit(existingEntry, matchedFace, score, dto.getGateNo(), captureImagePath, validOrder.getId());
        }

        // ========== 无入园记录 → 首次入园，需验证状态和日期 ==========
        // 验证订单状态：1=已支付（可入园），其他拒绝
        if (validOrder.getStatus() != 1) {
            try {
                recordEntryLog(null, null, touristId, matchedFace, score, dto.getGateNo(), false,
                        "订单状态异常（未支付或已退款）", captureImagePath);
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
            }
            return FaceEntryVO.builder()
                    .success(false)
                    .score(score)
                    .message("订单状态异常，请联系客服")
                    .gateNo(dto.getGateNo())
                    .build();
        }
        if (!today.equals(validOrder.getVisitDate())) {
            try {
                recordEntryLog(null, null, touristId, matchedFace, score, dto.getGateNo(), false,
                        "订单游览日期不是今天", captureImagePath);
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
            }
            return FaceEntryVO.builder()
                    .success(false)
                    .score(score)
                    .message("您的订单游览日期为 " + validOrder.getVisitDate() + "，非今日入园")
                    .gateNo(dto.getGateNo())
                    .build();
        }

        // 获取订单详情
        OrderItem orderItem = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, validOrder.getId())
        ).stream().findFirst().orElse(null);

        // 记录入园成功（数据库唯一索引 uk_order_face_entry 兜底防并发）
        try {
            recordEntryLog(validOrder.getId(), orderItem != null ? orderItem.getId() : null,
                    touristId, matchedFace, score, dto.getGateNo(), true, null, captureImagePath);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("并发入园拦截(唯一索引): orderId={}, faceDataId={}", validOrder.getId(), matchedFace.getId());
            return FaceEntryVO.builder()
                    .success(false)
                    .score(score)
                    .message("该门票已使用，请勿重复入园")
                    .gateNo(dto.getGateNo())
                    .build();
        }

        // 使用匹配到的 FaceData 中的 realName，这才是票使用者（刷脸人）的真实姓名
        // 不能用 tourist.getRealName()，因为同一个下单人可能为多人买票
        String userName = matchedFace.getRealName() != null ? matchedFace.getRealName() : "游客";

        log.info("票务人脸核验入园成功: userId={}, touristId={}, faceDataId={}, orderId={}, score={}, gateNo={}",
                matchedFace.getUserId(), touristId, matchedFace.getId(), validOrder.getId(), score, dto.getGateNo());

        return FaceEntryVO.builder()
                .success(true)
                .score(score)
                .message("验证通过，欢迎入园！")
                .userName(userName)
                .action("ENTRY")
                .entryTime(LocalDateTime.now())
                .gateNo(dto.getGateNo())
                .captureImagePath(captureImagePath)
                .build();
    }

    /**
     * 处理出园：设置 exitTime，检查并更新订单出园状态
     */
    private FaceEntryVO handleFaceExit(EntryLog entryLog, FaceData matchedFace, BigDecimal score,
                                        String gateNo, String captureImagePath, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        entryLog.setExitTime(now);
        if (gateNo != null) {
            entryLog.setGateNo(gateNo);
        }
        entryLogMapper.updateById(entryLog);

        log.info("票务人脸出园成功: orderId={}, faceDataId={}, realName={}, gateNo={}",
                orderId, matchedFace.getId(), matchedFace.getRealName(), gateNo);

        // 检查该订单是否所有人均已出园，是则更新订单状态
        updateOrderExitStatus(orderId);

        String userName = matchedFace.getRealName() != null ? matchedFace.getRealName() : "游客";
        return FaceEntryVO.builder()
                .success(true)
                .score(score)
                .message("出园成功，欢迎再次光临！")
                .userName(userName)
                .action("EXIT")
                .exitTime(now)
                .gateNo(gateNo)
                .captureImagePath(captureImagePath)
                .build();
    }

    /**
     * 检查订单是否所有人均已出园，若是则更新订单状态为 ORDER_STATUS_EXITED
     */
    private void updateOrderExitStatus(Long orderId) {
        Long totalEntries = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, orderId)
                        .eq(EntryLog::getStatus, 1)
        );
        Long exitedEntries = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, orderId)
                        .eq(EntryLog::getStatus, 1)
                        .isNotNull(EntryLog::getExitTime)
        );
        if (totalEntries.longValue() == exitedEntries.longValue()) {
            orderMapper.update(null, new LambdaUpdateWrapper<TicketOrder>()
                    .eq(TicketOrder::getId, orderId)
                    .set(TicketOrder::getStatus, ORDER_STATUS_EXITED)
            );
            log.info("订单全部出园，状态更新为已出园: orderId={}, total={}, exited={}",
                    orderId, totalEntries, exitedEntries);
        }
    }

    @Override
    @Transactional
    public void cleanExpiredFaces() {
        LocalDateTime now = LocalDateTime.now();
        // 只清理有过期时间的票务人脸，不清理内部通道人脸（expireTime 为 null）
        LambdaQueryWrapper<FaceData> wrapper = new LambdaQueryWrapper<FaceData>()
                .eq(FaceData::getStatus, 1)
                .isNotNull(FaceData::getExpireTime)
                .lt(FaceData::getExpireTime, now);

        faceDataMapper.selectList(wrapper).forEach(face -> {
            try {
                if (StrUtil.isNotBlank(face.getBaiduFaceToken())
                        && StrUtil.isNotBlank(face.getBaiduGroupId())) {
                    baiduFaceApiClient.deleteFace(face.getBaiduFaceToken(), face.getBaiduGroupId());
                }
            } catch (Exception e) {
                log.warn("百度人脸删除失败（不影响本地清理）: faceToken={}", face.getBaiduFaceToken(), e);
            }

            try {
                if (StrUtil.isNotBlank(face.getFaceImagePath())) {
                    Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                    Path imagePath = basePath.resolve(
                            face.getFaceImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(imagePath);
                }
            } catch (Exception e) {
                log.warn("删除本地人脸图片失败: {}", face.getFaceImagePath(), e);
            }

            face.setStatus(0);
            faceDataMapper.updateById(face);
        });

        log.info("过期人脸数据清理完成");
    }

    @Override
    public FaceData getMyFace(Long userId) {
        // 个人中心只返回内部通道人脸（orderId 为 null）
        return faceDataMapper.selectOne(
                new LambdaQueryWrapper<FaceData>()
                        .eq(FaceData::getUserId, userId)
                        .isNull(FaceData::getOrderId)
                        .eq(FaceData::getStatus, 1)
                        .orderByDesc(FaceData::getId)
                        .last("LIMIT 1")
        );
    }

    @Override
    @Transactional
    public void deleteMyFace(Long userId) {
        // 只删除内部通道人脸（orderId 为 null）
        FaceData face = getMyFace(userId);
        if (face == null) {
            throw new BusinessException("您未录入内部通道人脸");
        }

        try {
            if (StrUtil.isNotBlank(face.getBaiduFaceToken())
                    && StrUtil.isNotBlank(face.getBaiduGroupId())) {
                baiduFaceApiClient.deleteFace(face.getBaiduFaceToken(), face.getBaiduGroupId());
            }
        } catch (Exception e) {
            log.warn("百度人脸删除失败: {}", face.getBaiduFaceToken(), e);
        }

        try {
            if (StrUtil.isNotBlank(face.getFaceImagePath())) {
                Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                Path imagePath = basePath.resolve(
                        face.getFaceImagePath().replace("/uploads/", ""));
                Files.deleteIfExists(imagePath);
            }
        } catch (Exception e) {
            log.warn("删除本地人脸图片失败: {}", face.getFaceImagePath(), e);
        }

        faceDataMapper.deleteById(face.getId());

        Tourist tourist = touristMapper.selectOne(
                new LambdaQueryWrapper<Tourist>().eq(Tourist::getUserId, userId)
        );
        if (tourist != null) {
            tourist.setFaceStatus(0);
            tourist.setBaiduFaceToken(null);
            touristMapper.updateById(tourist);
        }

        log.info("用户内部通道人脸已删除: userId={}, faceDataId={}", userId, face.getId());
    }

    @Override
    public BaiduFaceApiClient.FaceMatchResult compareFaces(String imageBase641, String imageBase642) {
        return baiduFaceApiClient.matchFaces(imageBase641, imageBase642);
    }

    @Override
    @Transactional
    public void exitPark(String orderNo) {
        TicketOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 已入园或已支付（部分入园）的订单才能出园
        if (order.getStatus() != ORDER_STATUS_ENTERED && order.getStatus() != 1) {
            throw new BusinessException("该订单当前状态无法执行出园操作");
        }

        // 查找该订单所有已入园但未出园的记录
        List<EntryLog> entryLogs = entryLogMapper.selectList(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getStatus, 1)
                        .isNull(EntryLog::getExitTime)
        );

        if (entryLogs.isEmpty()) {
            throw new BusinessException("该订单没有在园记录，无法出园");
        }

        LocalDateTime now = LocalDateTime.now();
        // 所有在园人员全部出园
        for (EntryLog log : entryLogs) {
            log.setExitTime(now);
            entryLogMapper.updateById(log);
        }
        log.info("订单 {} 全部人员出园，共 {} 人", orderNo, entryLogs.size());

        // 检查是否该订单所有人均已出园，若是则更新订单状态为已出园
        Long totalEntries = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getStatus, 1)
        );
        Long exitedEntries = entryLogMapper.selectCount(
                new LambdaQueryWrapper<EntryLog>()
                        .eq(EntryLog::getOrderId, order.getId())
                        .eq(EntryLog::getStatus, 1)
                        .isNotNull(EntryLog::getExitTime)
        );
        if (totalEntries.longValue() == exitedEntries.longValue()) {
            orderMapper.update(null, new LambdaUpdateWrapper<TicketOrder>()
                    .eq(TicketOrder::getId, order.getId())
                    .set(TicketOrder::getStatus, ORDER_STATUS_EXITED)
            );
            log.info("订单全部出园，状态更新为已出园: orderId={}", order.getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerGroupFaces(Long groupOrderId, Long ticketOrderId) {
        GroupOrder groupOrder = groupOrderMapper.selectById(groupOrderId);
        if (groupOrder == null) {
            throw new BusinessException("团体订单不存在");
        }

        // 查询所有有照片但尚未注册成功的成员（faceStatus != 1 避免重复注册）
        List<GroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<GroupMember>()
                        .eq(GroupMember::getGroupOrderId, groupOrderId)
                        .isNotNull(GroupMember::getFaceImagePath)
                        .ne(GroupMember::getFaceImagePath, "")
                        .ne(GroupMember::getFaceStatus, 1)
        );

        if (members.isEmpty()) {
            log.info("团体订单[{}]没有已录入人脸的成员，跳过批量注册", groupOrderId);
            // 标记完成
            updateProgress(groupOrderId, "COMPLETED", members.size(), 0, 0, 0, null, null);
            return;
        }

        // 初始化进度
        updateProgress(groupOrderId, "RUNNING", members.size(), 0, 0, 0, "准备开始...", null);

        // 查找/创建主用户的游客记录
        Long userId = groupOrder.getUserId();
        Tourist tourist = touristMapper.selectOne(
                new LambdaQueryWrapper<Tourist>().eq(Tourist::getUserId, userId)
        );
        if (tourist == null) {
            SysUser sysUser = sysUserMapper.selectById(userId);
            tourist = new Tourist();
            tourist.setUserId(userId);
            tourist.setRealName(sysUser != null && sysUser.getRealName() != null
                    ? sysUser.getRealName() : groupOrder.getContactName());
            tourist.setPhone(groupOrder.getContactPhone());
            tourist.setFaceStatus(0);
            touristMapper.insert(tourist);
            log.info("团体注册自动创建游客记录: userId={}, touristId={}", userId, tourist.getId());
        }

        String groupId = buildGroupId(groupOrder.getVisitDate());
        LocalDateTime expireTime = groupOrder.getVisitDate().plusDays(1).atTime(23, 59, 59);
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();

        int successCount = 0;
        int failedCount = 0;
        for (int i = 0; i < members.size(); i++) {
            GroupMember member = members.get(i);
            String imagePath = member.getFaceImagePath();
            if (imagePath == null || imagePath.isBlank()) continue;

            // 更新当前处理人名
            updateProgress(groupOrderId, "RUNNING", members.size(),
                    successCount + failedCount, successCount, failedCount,
                    member.getRealName(), null);

            Path filePath = basePath.resolve(imagePath.replace("/uploads/", ""));
            if (!Files.exists(filePath)) {
                log.warn("团体成员人脸照片不存在，跳过: realName={}, path={}", member.getRealName(), filePath);
                failedCount++;
                continue;
            }

            try {
                byte[] imageBytes = Files.readAllBytes(filePath);
                String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

                String baiduUserId = buildUserId(tourist.getId(), ticketOrderId);

                // 调用百度API前短暂休眠，避免触发QPS限流
                if (successCount > 0) {
                    try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }

                // 注册到百度人脸库
                BaiduFaceApiClient.FaceRegisterResult apiResult =
                        baiduFaceApiClient.registerFace(imageBase64, groupId, baiduUserId);

                // 写入 face_data 表
                FaceData faceData = new FaceData();
                faceData.setTouristId(tourist.getId());
                faceData.setUserId(userId);
                faceData.setOrderId(ticketOrderId);
                faceData.setRealName(member.getRealName());
                faceData.setPhone(member.getPhone());
                faceData.setBaiduFaceToken(apiResult.getFaceToken());
                faceData.setBaiduUserId(baiduUserId);
                faceData.setBaiduGroupId(groupId);
                faceData.setFaceImagePath(imagePath);
                faceData.setExpireTime(expireTime);
                faceData.setStatus(1);

                // 获取质量分
                try {
                    Double qualityScore = apiResult.getQualityScore();
                    if (qualityScore == null) {
                        qualityScore = baiduFaceApiClient.detectFaceQuality(imageBase64);
                    }
                    if (qualityScore != null) {
                        faceData.setQualityScore(BigDecimal.valueOf(qualityScore)
                                .setScale(2, java.math.RoundingMode.HALF_UP));
                    }
                } catch (Exception e) {
                    log.warn("获取团体人脸质量分失败: realName={}", member.getRealName(), e);
                }

                faceDataMapper.insert(faceData);
                successCount++;
                // 更新团体成员人脸状态为已录入
                groupMemberMapper.update(null, new LambdaUpdateWrapper<GroupMember>()
                        .eq(GroupMember::getId, member.getId())
                        .set(GroupMember::getFaceStatus, 1)
                        .set(GroupMember::getFailReason, null)
                );
                log.info("团体成员人脸注册成功: realName={}, faceToken={}, groupId={}",
                        member.getRealName(), apiResult.getFaceToken(), groupId);
            } catch (Exception e) {
                log.error("团体成员人脸注册失败: realName={}", member.getRealName(), e);
                failedCount++;
                // 更新团体成员人脸状态为失败，记录失败原因
                String reason = e.getMessage();
                if (reason != null && reason.length() > 500) {
                    reason = reason.substring(0, 497) + "...";
                }
                groupMemberMapper.update(null, new LambdaUpdateWrapper<GroupMember>()
                        .eq(GroupMember::getId, member.getId())
                        .set(GroupMember::getFaceStatus, 2)
                        .set(GroupMember::getFailReason, reason)
                );
                // 单个成员失败不中断整体，继续处理下一个
            }
        }

        // 最终状态
        String finalStatus = (failedCount == members.size() && members.size() > 0) ? "FAILED" : "COMPLETED";
        updateProgress(groupOrderId, finalStatus, members.size(),
                successCount + failedCount, successCount, failedCount,
                "录入完成", null);

        log.info("团体订单[{}]批量人脸注册完成: 共{}人, 成功{}人, 失败{}人", groupOrderId, members.size(), successCount, failedCount);
    }

    /** 进度缓存 key 前缀 */
    private static final String PROGRESS_CACHE_PREFIX = "group-face-progress:";

    /**
     * 异步批量注册团体人脸（不阻塞支付 HTTP 响应）
     */
    @Override
    @Async
    public void registerGroupFacesAsync(Long groupOrderId, Long ticketOrderId) {
        log.info("异步人脸注册开始: groupOrderId={}, ticketOrderId={}", groupOrderId, ticketOrderId);
        try {
            registerGroupFaces(groupOrderId, ticketOrderId);
        } catch (Exception e) {
            log.error("异步人脸注册异常: groupOrderId={}", groupOrderId, e);
            updateProgress(groupOrderId, "FAILED", 0, 0, 0, 0, null,
                    e.getMessage() != null ? e.getMessage() : "未知错误");
        }
    }

    /**
     * 查询团体人脸注册进度
     */
    @Override
    public GroupFaceProgressVO getGroupFaceProgress(Long groupOrderId) {
        String key = PROGRESS_CACHE_PREFIX + groupOrderId;
        Object cached = cacheStore.get(key);
        if (cached instanceof GroupFaceProgressVO) {
            return (GroupFaceProgressVO) cached;
        }
        // 返回初始状态
        return GroupFaceProgressVO.builder()
                .groupOrderId(groupOrderId)
                .status("NOT_STARTED")
                .totalMembers(0)
                .completedCount(0)
                .successCount(0)
                .failedCount(0)
                .build();
    }

    /**
     * 更新进度到缓存
     */
    private void updateProgress(Long groupOrderId, String status, int totalMembers,
                                int completedCount, int successCount, int failedCount,
                                String currentName, String errorMessage) {
        try {
            GroupFaceProgressVO progress = GroupFaceProgressVO.builder()
                    .groupOrderId(groupOrderId)
                    .status(status)
                    .totalMembers(totalMembers)
                    .completedCount(completedCount)
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .currentName(currentName)
                    .errorMessage(errorMessage)
                    .startTime(System.currentTimeMillis())
                    .build();
            cacheStore.set(PROGRESS_CACHE_PREFIX + groupOrderId, progress);
        } catch (Exception e) {
            log.debug("更新人脸注册进度失败: groupOrderId={}", groupOrderId, e);
        }
    }

    @Override
    @Transactional
    public void reRegisterGroupMemberFace(Long memberId, String imageBase64) {
        // 1. 查找成员
        GroupMember member = groupMemberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException("成员不存在");
        }

        // 2. 查找团体订单
        GroupOrder groupOrder = groupOrderMapper.selectById(member.getGroupOrderId());
        if (groupOrder == null) {
            throw new BusinessException("团体订单不存在");
        }
        if (groupOrder.getStatus() != 3) {
            throw new BusinessException("订单未支付，无法重录人脸");
        }

        // 3. 查找关联的票务订单
        TicketOrder ticketOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getGroupOrderId, groupOrder.getId())
                        .eq(TicketOrder::getStatus, 1)
        );
        if (ticketOrder == null) {
            throw new BusinessException("关联的票务订单不存在");
        }

        // 检查游览日期是否已过期
        if (ticketOrder.getVisitDate() != null && ticketOrder.getVisitDate().isBefore(LocalDate.now())) {
            throw new BusinessException("该订单游览日期已过期（" + ticketOrder.getVisitDate() + "），无法重录人脸");
        }

        // 4. 查找/创建游客记录
        Long userId = groupOrder.getUserId();
        Tourist tourist = touristMapper.selectOne(
                new LambdaQueryWrapper<Tourist>().eq(Tourist::getUserId, userId)
        );
        if (tourist == null) {
            SysUser sysUser = sysUserMapper.selectById(userId);
            tourist = new Tourist();
            tourist.setUserId(userId);
            tourist.setRealName(sysUser != null && sysUser.getRealName() != null
                    ? sysUser.getRealName() : groupOrder.getContactName());
            tourist.setPhone(groupOrder.getContactPhone());
            tourist.setFaceStatus(0);
            touristMapper.insert(tourist);
            log.info("重录人脸自动创建游客记录: userId={}, touristId={}", userId, tourist.getId());
        }

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();

        // 5. 保存新照片到本地（使用真实姓名生成可读文件名）
        String newImagePath = saveFaceImage(imageBase64,
                "member_" + memberId + "_" + System.currentTimeMillis(), member.getRealName());

        // 6. 删除成员旧照片
        if (StrUtil.isNotBlank(member.getFaceImagePath())) {
            try {
                Path oldPath = basePath.resolve(
                        member.getFaceImagePath().replace("/uploads/", ""));
                Files.deleteIfExists(oldPath);
            } catch (Exception e) {
                log.warn("删除成员旧照片失败: path={}", member.getFaceImagePath(), e);
            }
        }

        // 7. 如果该成员之前已注册成功过，清理旧的 FaceData（百度端 + 本地文件 + 数据库）
        FaceData oldFaceData = faceDataMapper.selectOne(
                new LambdaQueryWrapper<FaceData>()
                        .eq(FaceData::getOrderId, ticketOrder.getId())
                        .eq(FaceData::getRealName, member.getRealName())
                        .eq(FaceData::getStatus, 1)
        );
        if (oldFaceData != null) {
            try {
                if (StrUtil.isNotBlank(oldFaceData.getBaiduFaceToken())
                        && StrUtil.isNotBlank(oldFaceData.getBaiduGroupId())) {
                    baiduFaceApiClient.deleteFace(oldFaceData.getBaiduFaceToken(),
                            oldFaceData.getBaiduGroupId());
                }
            } catch (Exception e) {
                log.warn("删除旧百度人脸失败: faceToken={}", oldFaceData.getBaiduFaceToken(), e);
            }
            try {
                if (StrUtil.isNotBlank(oldFaceData.getFaceImagePath())) {
                    Path oldImgPath = basePath.resolve(
                            oldFaceData.getFaceImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(oldImgPath);
                }
            } catch (Exception e) {
                log.warn("删除旧FaceData图片失败: path={}", oldFaceData.getFaceImagePath(), e);
            }
            faceDataMapper.deleteById(oldFaceData.getId());
        }

        // 8. 调用百度API注册人脸
        String groupId = buildGroupId(groupOrder.getVisitDate());
        LocalDateTime expireTime = groupOrder.getVisitDate().plusDays(1).atTime(23, 59, 59);
        String baiduUserId = buildUserId(tourist.getId(), ticketOrder.getId());

        BaiduFaceApiClient.FaceRegisterResult apiResult;
        try {
            apiResult = baiduFaceApiClient.registerFace(imageBase64, groupId, baiduUserId);
        } catch (Exception e) {
            log.error("团体成员人脸重录失败(百度API): memberId={}, realName={}", memberId,
                    member.getRealName(), e);
            String reason = e.getMessage();
            if (reason != null && reason.length() > 500) {
                reason = reason.substring(0, 497) + "...";
            }
            // 更新成员状态为失败，但保留新照片路径供后续重试
            groupMemberMapper.update(null, new LambdaUpdateWrapper<GroupMember>()
                    .eq(GroupMember::getId, memberId)
                    .set(GroupMember::getFaceStatus, 2)
                    .set(GroupMember::getFailReason, reason)
                    .set(GroupMember::getFaceImagePath, newImagePath)
            );
            throw new BusinessException("人脸注册失败: " + e.getMessage());
        }

        // 9. 写入 face_data 表
        FaceData faceData = new FaceData();
        faceData.setTouristId(tourist.getId());
        faceData.setUserId(userId);
        faceData.setOrderId(ticketOrder.getId());
        faceData.setRealName(member.getRealName());
        faceData.setPhone(member.getPhone());
        faceData.setBaiduFaceToken(apiResult.getFaceToken());
        faceData.setBaiduUserId(baiduUserId);
        faceData.setBaiduGroupId(groupId);
        faceData.setFaceImagePath(newImagePath);
        faceData.setExpireTime(expireTime);
        faceData.setStatus(1);

        // 获取质量分
        try {
            Double qualityScore = apiResult.getQualityScore();
            if (qualityScore == null) {
                qualityScore = baiduFaceApiClient.detectFaceQuality(imageBase64);
            }
            if (qualityScore != null) {
                faceData.setQualityScore(BigDecimal.valueOf(qualityScore)
                        .setScale(2, java.math.RoundingMode.HALF_UP));
            }
        } catch (Exception e) {
            log.warn("获取重录人脸质量分失败: realName={}", member.getRealName(), e);
        }

        faceDataMapper.insert(faceData);

        // 10. 更新成员状态为成功
        groupMemberMapper.update(null, new LambdaUpdateWrapper<GroupMember>()
                .eq(GroupMember::getId, memberId)
                .set(GroupMember::getFaceStatus, 1)
                .set(GroupMember::getFailReason, null)
                .set(GroupMember::getFaceImagePath, newImagePath)
        );

        log.info("团体成员人脸重录成功: memberId={}, realName={}, faceToken={}, groupId={}",
                memberId, member.getRealName(), apiResult.getFaceToken(), groupId);
    }

    @Override
    public void cleanGroupOrderFaces(Long ticketOrderId) {
        List<FaceData> faces = faceDataMapper.selectList(
                new LambdaQueryWrapper<FaceData>().eq(FaceData::getOrderId, ticketOrderId));
        for (FaceData face : faces) {
            // 删除百度端人脸
            try {
                if (StrUtil.isNotBlank(face.getBaiduFaceToken()) && StrUtil.isNotBlank(face.getBaiduGroupId())) {
                    baiduFaceApiClient.deleteFace(face.getBaiduFaceToken(), face.getBaiduGroupId());
                }
            } catch (Exception e) {
                log.warn("删除百度人脸失败: faceToken={}", face.getBaiduFaceToken(), e);
            }
            // 删除本地图片
            try {
                if (StrUtil.isNotBlank(face.getFaceImagePath())) {
                    Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
                    Path imagePath = basePath.resolve(face.getFaceImagePath().replace("/uploads/", ""));
                    Files.deleteIfExists(imagePath);
                }
            } catch (Exception e) {
                log.warn("删除人脸图片失败: path={}", face.getFaceImagePath(), e);
            }
            faceDataMapper.deleteById(face.getId());
        }
        log.info("已清理团体订单关联人脸: ticketOrderId={}, 共{}条", ticketOrderId, faces.size());
    }

    // ==================== 私有方法 ====================

    /**
     * 在指定分组列表中搜索人脸
     */
    private BaiduFaceApiClient.FaceSearchResult searchFaceInGroups(String imageBase64, String groupIdList) {
        try {
            return baiduFaceApiClient.searchFace(imageBase64, groupIdList);
        } catch (Exception e) {
            log.warn("人脸搜索失败: groups={}", groupIdList, e);
            return null;
        }
    }

    /**
     * 构建百度人脸库分组ID
     */
    private String buildGroupId(LocalDate date) {
        return GROUP_PREFIX + "_" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 构建百度唯一 user_id（每张人脸一个独立 userId，同一订单不同人不会冲突）
     */
    private String buildUserId(Long touristId, Long orderId) {
        // touristId_orderId_随机后缀 → 保证每张人脸在百度库中有唯一标识
        return USER_ID_PREFIX + "_" + touristId + "_" + (orderId != null ? orderId : 0)
                + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 从百度返回的 user_id 解析 touristId（兼容新旧格式）
     * 旧格式: tourist_123
     * 新格式: tourist_123_456_abc12345
     */
    private Long parseTouristId(String userId) {
        if (StrUtil.isBlank(userId) || !userId.startsWith(USER_ID_PREFIX + "_")) {
            return null;
        }
        // 去掉前缀 "tourist_"
        String suffix = userId.substring(USER_ID_PREFIX.length() + 1);
        // touristId 始终是第二个下划线之前的部分（新格式）或整个 suffix（旧格式）
        String touristIdStr = suffix.split("_")[0];
        try {
            return Long.valueOf(touristIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将 Base64 图片保存到本地
     * @param imageBase64 图片 base64 数据
     * @param faceToken 标识（注册时传 faceToken，抓拍时传 "capture_{timestamp}"）
     * @param realName 真实姓名（注册时传入，用于生成可读文件名；抓拍时传 null）
     */
    private String saveFaceImage(String imageBase64, String faceToken, String realName) {
        try {
            String relativeDir = "/uploads/face/";
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize().resolve("face");
            Files.createDirectories(dir);

            // 文件名: face_{realName}_{yyyyMMdd}.jpg (注册照) / capture_{timestamp}.jpg (闸机抓拍)
            String fileName;
            if (faceToken.startsWith("capture_")) {
                fileName = faceToken + ".jpg";
            } else if (StrUtil.isNotBlank(realName)) {
                String safeName = realName.replaceAll("[\\\\/:*?\"<>|]", "_");
                String dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                fileName = "face_" + safeName + "_" + dateStr + ".jpg";
            } else {
                fileName = "face_" + java.util.UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            }
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Files.write(dir.resolve(fileName), imageBytes);

            return relativeDir + fileName;
        } catch (Exception e) {
            log.warn("保存人脸图片失败: faceToken={}", faceToken, e);
            return null;
        }
    }

    /** 订单已入园状态码 */
    private static final int ORDER_STATUS_ENTERED = 5;
    /** 订单已出园状态码 */
    private static final int ORDER_STATUS_EXITED = 6;

    private void recordEntryLog(Long orderId, Long orderItemId, Long touristId,
                                FaceData faceData, BigDecimal score, String gateNo,
                                boolean success, String failReason, String captureImagePath) {
        EntryLog entryLog = new EntryLog();
        entryLog.setOrderId(orderId != null ? orderId : -1L);
        entryLog.setOrderItemId(orderItemId != null ? orderItemId : -1L);
        entryLog.setUserId(faceData != null ? faceData.getUserId() : -1L);
        entryLog.setTouristId(touristId != null ? touristId : -1L);
        entryLog.setFaceDataId(faceData != null ? faceData.getId() : null);
        entryLog.setCaptureImagePath(captureImagePath);
        entryLog.setCompareScore(score);
        entryLog.setEntryTime(LocalDateTime.now());
        entryLog.setGateNo(gateNo != null ? gateNo : "A01");
        entryLog.setStatus(success ? 1 : 0);
        entryLog.setFailReason(failReason);
        entryLogMapper.insert(entryLog);

        // 入园成功后，检查该订单是否全部人已入园
        if (success && orderId != null && orderId > 0) {
            try {
                Long faceDataCount = faceDataMapper.selectCount(
                        new LambdaQueryWrapper<FaceData>()
                                .eq(FaceData::getOrderId, orderId)
                                .eq(FaceData::getStatus, 1)
                );
                Long entryCount = entryLogMapper.selectCount(
                        new LambdaQueryWrapper<EntryLog>()
                                .eq(EntryLog::getOrderId, orderId)
                                .eq(EntryLog::getStatus, 1)
                );
                if (faceDataCount > 0 && faceDataCount.longValue() == entryCount.longValue()) {
                    orderMapper.update(null, new LambdaUpdateWrapper<TicketOrder>()
                            .eq(TicketOrder::getId, orderId)
                            .set(TicketOrder::getStatus, ORDER_STATUS_ENTERED)
                    );
                    log.info("订单全部入园，状态更新为已入园: orderId={}, faceDataCount={}, entryCount={}",
                            orderId, faceDataCount, entryCount);
                }
            } catch (Exception e) {
                log.warn("更新订单入园状态失败: orderId={}", orderId, e);
            }
        }
    }
}
