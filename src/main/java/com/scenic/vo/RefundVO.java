package com.scenic.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundVO {
    private Long id;
    private Long orderId;
    private String orderNo;
    private String refundNo;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private String statusText;
    private String auditUserName;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
}
