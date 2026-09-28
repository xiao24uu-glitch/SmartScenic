package com.scenic.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片服务过滤器
 * <p>
 * 拦截 /uploads/** 请求，手动读取磁盘文件并返回。
 * 解决 Windows 下 Spring ResourceHandler 对中文 URL 编码路径处理失败的问题。
 * </p>
 */
@Slf4j
@Component
@Order(-100)
public class ImageServeFilter implements Filter {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        // 只处理 /uploads/ 路径
        if (!requestURI.startsWith("/uploads/") || !"GET".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // 提取 /uploads/ 后面的相对路径
        String relativePath = requestURI.substring("/uploads/".length());
        // URL 解码（处理中文等特殊字符）
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("URL 解码失败: {}", relativePath);
            decodedPath = relativePath;
        }

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(decodedPath).normalize();

        // 安全检查：防止目录遍历
        if (!filePath.startsWith(basePath)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 文件不存在 → 尝试智能匹配（兼容旧数据中 UUID 命名与手机号命名不一致的问题）
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            Path resolved = tryResolveGroupFacePath(basePath, decodedPath);
            if (resolved != null) {
                filePath = resolved;
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        // CORS（跨域支持前端 Vite 开发服务器）
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");

        // Content-Type
        response.setContentType(detectContentType(filePath.toString()));
        response.setContentLengthLong(Files.size(filePath));
        response.setHeader("Cache-Control", "public, max-age=3600");

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            log.error("输出图片流失败: {}", filePath, e);
        }
    }

    /**
     * 尝试解析团体人脸照片的实际路径。
     * <p>
     * 场景：face_data 表中可能存储了旧版 UUID 命名的路径
     * （如 张三_d08d5dcd.jpg），但磁盘文件使用手机号命名
     * （如 张三_13800010001.jpg）。本方法通过目录扫描
     * 找到以「人名_」开头的实际文件。
     * </p>
     *
     * @return 匹配到的文件路径，未找到则返回 null
     */
    private Path tryResolveGroupFacePath(Path basePath, String decodedPath) {
        try {
            // 只处理 group-face 目录下的文件
            String normalized = decodedPath.replace('\\', '/');
            if (!normalized.contains("/group-face/")) {
                return null;
            }

            int lastSlash = normalized.lastIndexOf('/');
            String dirPart = normalized.substring(0, lastSlash);
            String fileName = normalized.substring(lastSlash + 1);

            // 提取文件名中的人名部分（如 "张三_d08d5dcd50be40b89320cd537f6fc45c.jpg" → "张三_"）
            int underscoreIdx = fileName.indexOf('_');
            if (underscoreIdx <= 0) return null;
            String namePrefix = fileName.substring(0, underscoreIdx + 1); // "张三_"

            Path dirPath = basePath.resolve(dirPart).normalize();
            if (!dirPath.startsWith(basePath) || !Files.isDirectory(dirPath)) {
                return null;
            }

            // 遍历目录，找到以 "人名_" 开头的文件
            try (var stream = Files.list(dirPath)) {
                return stream
                        .filter(f -> {
                            String fname = f.getFileName().toString();
                            return fname.startsWith(namePrefix) && isImageFile(fname);
                        })
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.debug("智能路径匹配失败: {}", decodedPath, e);
            return null;
        }
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".webp") || lower.endsWith(".svg");
    }

    private String detectContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF_VALUE;
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
