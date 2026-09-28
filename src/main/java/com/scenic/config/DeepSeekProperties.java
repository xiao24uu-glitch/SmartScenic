package com.scenic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
