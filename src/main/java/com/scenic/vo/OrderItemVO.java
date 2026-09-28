package com.scenic.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long id;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
