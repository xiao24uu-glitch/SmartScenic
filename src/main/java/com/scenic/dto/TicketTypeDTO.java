package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketTypeDTO {
    @NotBlank(message = "票种名称不能为空")
    private String name;
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    @NotNull(message = "总库存不能为空")
    private Integer totalStock;
    @NotNull(message = "每日库存不能为空")
    private Integer dailyStock;
    private String description;
    private Integer isGroup;
    private Integer minGroupSize;
    /** 最大可预约天数，默认散客7、团体14 */
    private Integer maxBookingDays;
    private Integer status;
}
