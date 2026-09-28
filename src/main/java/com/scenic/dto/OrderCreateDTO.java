package com.scenic.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDTO {
    @NotNull(message = "游览日期不能为空")
    private LocalDate visitDate;

    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemDTO> items;

    /** 用户选择的优惠券ID(user_coupon主键) */
    private Long userCouponId;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "票种ID不能为空")
        private Long ticketTypeId;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
