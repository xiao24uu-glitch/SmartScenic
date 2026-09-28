package com.scenic.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponVO {
    private Long userCouponId;      // user_coupon.id
    private Long couponId;          // coupon.id
    private String couponName;
    private Integer type;           // 1-满减 2-折扣
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private LocalDate validUntil;
    private String desc;            // 描述文字，如"满100减20"
    private BigDecimal discountAmount; // 实际优惠金额（根据订单金额计算）
}
