package com.scenic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TravelogueDTO {
    @NotNull(message = "订单号不能为空")
    private String orderNo;
    /** 游记风格：literary-文艺风 humor-幽默风 simple-简洁风 */
    private String style;
}
