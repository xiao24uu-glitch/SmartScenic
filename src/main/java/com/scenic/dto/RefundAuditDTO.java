package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundAuditDTO {
    @NotBlank(message = "退款编号不能为空")
    private String refundNo;
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    private String remark;
}
