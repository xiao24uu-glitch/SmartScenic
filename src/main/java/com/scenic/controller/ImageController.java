package com.scenic.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片代理控制器
 * <p>
 * 解决 Windows 环境下 Spring 静态资源映射无法正确处理中文 URL 编码的问题。
 * 通过 Controller 手动读取文件并返回，绕过 ResourceHandlerRegistry 的编码缺陷。
 * </p>
 */
@Slf4j
@RestController
public class ImageController {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    /**
     * 代理访问 uploads 目录下的图片文件
     * <p>
     * 前端仍使用 /uploads/xxx.jpg 这样的路径访问，
     * 此 Controller 会拦截请求，手动解码路径并从磁盘读取文件返回。
     * </p>
     */
    @GetMapping("/uploads/**")
    public void serveImage(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.debug("ImageController 收到请求: {}", requestURI);

        // 截取 /uploads/ 之后的部分作为相对路径
        String relativePath = requestURI.substring("/uploads/".length());
        // URL 解码（处理中文等特殊字符）
        String decodedPath;
        try {
            decodedPath = java.net.URLDecoder.decode(relativePath, "UTF-8");
        } catch (Exception e) {
            log.warn("URL解码失败: {}", relativePath);
            decodedPath = relativePath;
        }

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(decodedPath).normalize();

        // 安全检查：防止目录遍历攻击
        if (!filePath.startsWith(basePath)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 根据文件扩展名设置 Content-Type
        String contentType = determineContentType(filePath.toString());
        response.setContentType(contentType);
        response.setContentLengthLong(filePath.toFile().length());

        // 设置缓存策略（图片可缓存1小时）
        response.setHeader("Cache-Control", "public, max-age=3600");

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            log.error("输出图片流失败: {}", filePath, e);
        }
    }

    private String determineContentType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lower.endsWith(".ico")) {
            return "image/x-icon";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
