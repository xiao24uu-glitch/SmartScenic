package com.scenic.service;

import com.scenic.dto.AiChatDTO;
import com.scenic.dto.TravelogueDTO;
import com.scenic.dto.TravelogueDownloadDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiAssistantService {
    SseEmitter chat(Long userId, AiChatDTO dto);

    SseEmitter generateTravelogue(Long userId, TravelogueDTO dto);

    byte[] downloadTravelogue(TravelogueDownloadDTO dto);

    void updateFeedback(Long conversationId, Long userId, Integer feedback);
}
