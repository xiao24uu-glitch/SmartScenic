package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long ticketTypeId;
    private Integer quantity;
    private BigDecimal unitPrice;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
