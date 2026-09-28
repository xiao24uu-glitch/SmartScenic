package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_conversation")
public class AiConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String sessionId;
    private String question;
    private String answer;
    private String intent;
    private Integer tokensUsed;
    private Integer feedback;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
