package com.scenic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 1-满减券，2-折扣券 */
    private Integer type;
    /** 使用门槛(满多少元可用) */
    private BigDecimal threshold;
    /** 优惠值(满减券为金额，折扣券为折扣率如0.85) */
    private BigDecimal discountValue;
    /** 最大优惠金额(折扣券专用) */
    private BigDecimal maxDiscount;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer usedCount;
    /** 每人限领数量 */
    private Integer perUserLimit;
    /** 有效期天数 */
    private Integer validDays;
    /** 0-停用 1-启用 */
    private Integer status;
    /** 新用户订单上限，-1不限，N表示用户最多N笔已支付订单时可领 */
    private Integer maxOrderCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
