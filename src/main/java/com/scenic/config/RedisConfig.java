package com.scenic.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(new FastJson2RedisSerializer());
        template.setHashValueSerializer(new FastJson2RedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 基于 FastJSON2 的 Redis 序列化器
     */
    public static class FastJson2RedisSerializer implements RedisSerializer<Object> {

        public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

        @Override
        public byte[] serialize(Object obj) throws SerializationException {
            if (obj == null) {
                return new byte[0];
            }
            return JSON.toJSONString(obj, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return JSON.parseObject(new String(bytes, DEFAULT_CHARSET), Object.class, JSONReader.Feature.SupportAutoType);
        }
    }
}
