package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ticket_order")
public class TicketOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long scenicId;
    private LocalDate visitDate;
    private LocalDate pendingVisitDate;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer payType;
    private LocalDateTime payTime;
    private String payTradeNo;
    private Integer isGroup;
    private Long groupOrderId;
    private Integer status;
    private Long couponId;
    private BigDecimal discountAmount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
