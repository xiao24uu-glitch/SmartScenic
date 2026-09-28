package com.scenic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baidu.face")
public class BaiduFaceProperties {
    private String appId;
    private String apiKey;
    private String secretKey;
    private Double threshold = 0.80;
}
