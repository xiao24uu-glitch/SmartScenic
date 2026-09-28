package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayDTO {
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;
    @NotNull(message = "支付结果不能为空")
    private Boolean success;
}
