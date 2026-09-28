package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tourist")
public class Tourist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String realName;
    private String idCard;
    private String phone;
    private Integer faceStatus;
    private String baiduFaceToken;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
