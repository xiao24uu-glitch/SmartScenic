package com.scenic.controller;

import com.scenic.common.Result;
import com.scenic.dto.AiChatDTO;
import com.scenic.dto.TravelogueDTO;
import com.scenic.dto.TravelogueDownloadDTO;
import com.scenic.security.SecurityUtil;
import com.scenic.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "AI智能助手")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "AI助手对话（SSE流式）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        return aiAssistantService.chat(userId, dto);
    }

    @Operation(summary = "AI游记生成（SSE流式）")
    @PostMapping(value = "/travelogue", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateTravelogue(@Valid @RequestBody TravelogueDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        return aiAssistantService.generateTravelogue(userId, dto);
    }

    @Operation(summary = "游记下载（Word）")
    @PostMapping("/travelogue/download")
    public ResponseEntity<byte[]> downloadTravelogue(@Valid @RequestBody TravelogueDownloadDTO dto) {
        securityUtil.getCurrentUserId(); // 仅校验登录
        byte[] fileBytes = aiAssistantService.downloadTravelogue(dto);
        String filename = URLEncoder.encode("游记_" + LocalDate.now(), StandardCharsets.UTF_8) + ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(fileBytes);
    }

    @Operation(summary = "提交AI回答评价")
    @PostMapping("/feedback")
    public Result<Void> submitFeedback(@RequestBody Map<String, Object> body) {
        Long userId = securityUtil.getCurrentUserId();
        Long conversationId = Long.valueOf(body.get("conversationId").toString());
        Integer feedback = (Integer) body.get("feedback");
        aiAssistantService.updateFeedback(conversationId, userId, feedback);
        return Result.success("评价提交成功", null);
    }
}
