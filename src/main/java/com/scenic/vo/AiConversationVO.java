package com.scenic.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiConversationVO {
    private Long id;
    private Long userId;
    private String sessionId;
    private String question;
    private String answer;
    private String intent;
    private Integer tokensUsed;
    private Integer feedback;
    private LocalDateTime createTime;
    /** 用户名（从 sys_user 关联） */
    private String username;
}
