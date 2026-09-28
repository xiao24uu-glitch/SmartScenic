package com.scenic.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地缓存组件，开发环境替代 Redis。
 * 应用重启后缓存自动清空，与 Redis 行为一致。
 */
@Slf4j
@Component
public class LocalCacheStore {

    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .removalListener((key, value, cause) ->
                    log.debug("本地缓存过期: key={}, cause={}", key, cause))
            .build();

    /**
     * 写入缓存
     */
    public void set(String key, Object value) {
        cache.put(key, value);
    }

    /**
     * 读取缓存
     */
    public Object get(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 删除单个缓存
     */
    public void delete(String key) {
        cache.invalidate(key);
    }

    /**
     * 按前缀批量删除
     */
    public void deleteByPrefix(String prefix) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cache.invalidateAll();
    }
}
