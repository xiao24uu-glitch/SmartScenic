package com.scenic.security;

import com.scenic.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    /** JWT 密钥最小字节数（HS256 要求） */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = buildKey(jwtProperties.getSecret());
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            log.warn("未配置 jwt.secret（建议通过环境变量 JWT_SECRET 注入），已生成一次性随机密钥，"
                    + "应用重启后原有 Token 将全部失效；生产环境请务必显式配置至少 {} 字节的密钥", MIN_SECRET_BYTES);
            return Keys.hmacShaKeyFor(random);
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 长度不足 " + MIN_SECRET_BYTES
                    + " 字节，请通过环境变量 JWT_SECRET 配置更长的随机密钥");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null, null);
    }

    public String generateToken(Long userId, String username, Long currentRoleId, Integer roleLevel) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        if (currentRoleId != null) {
            claims.put("currentRoleId", currentRoleId);
        }
        if (roleLevel != null) {
            claims.put("roleLevel", roleLevel);
        }
        return createToken(claims, username, jwtProperties.getExpiration());
    }

    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, username, jwtProperties.getRefreshExpiration());
    }

    public Long getCurrentRoleId(String token) {
        Claims claims = parseToken(token);
        Object val = claims.get("currentRoleId");
        return val != null ? ((Number) val).longValue() : null;
    }

    public Integer getRoleLevel(String token) {
        Claims claims = parseToken(token);
        Object val = claims.get("roleLevel");
        return val != null ? (Integer) val : null;
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
