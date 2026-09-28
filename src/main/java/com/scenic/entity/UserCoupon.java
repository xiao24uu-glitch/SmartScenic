package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    /** 1-满减券，2-折扣券 */
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private Long orderId;
    /** 0-未使用 1-已使用 2-已过期 */
    private Integer status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
}
