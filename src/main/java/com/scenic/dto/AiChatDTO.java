package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatDTO {
    /** 会话ID，为空则创建新会话 */
    private String sessionId;
    /** 用户消息 */
    @NotBlank(message = "消息内容不能为空")
    private String message;
}
