package com.scenic.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // 图片访问统一由 ImageController 处理（支持中文路径 URL 编码）
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 不再注册 /uploads/**，交给 ImageController 代理
    }
}
