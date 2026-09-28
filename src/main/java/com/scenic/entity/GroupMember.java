package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("group_member")
public class GroupMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupOrderId;
    private String realName;
    private String idCard;
    private String phone;
    private Long userId;
    private String faceImagePath;
    private Integer faceStatus;
    /** 人脸注册失败原因 */
    private String failReason;
    private Integer entryStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
