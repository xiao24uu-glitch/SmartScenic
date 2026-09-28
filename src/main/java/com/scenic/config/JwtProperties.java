package com.scenic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * JWT 签名密钥，长度至少 32 字节。
     * 请通过环境变量 JWT_SECRET 注入，不要写死在代码或配置文件中。
     */
    private String secret;
    private Long expiration = 7200000L;
    private Long refreshExpiration = 604800000L;
}
