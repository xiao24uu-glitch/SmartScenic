package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund")
public class Refund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String refundNo;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
