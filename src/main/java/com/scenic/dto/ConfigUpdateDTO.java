package com.scenic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfigUpdateDTO {
    @NotBlank(message = "配置键不能为空")
    private String configKey;
    // configValue 允许为空字符串（如Logo可为空）
    private String configValue;
}
