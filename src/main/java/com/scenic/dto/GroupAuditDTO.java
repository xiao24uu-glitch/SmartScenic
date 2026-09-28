package com.scenic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupAuditDTO {
    @NotNull(message = "团体订单ID不能为空")
    private Long groupOrderId;
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    private String remark;
}
