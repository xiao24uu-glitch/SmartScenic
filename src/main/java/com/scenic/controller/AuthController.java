package com.scenic.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.common.Result;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.LoginDTO;
import com.scenic.dto.RegisterDTO;
import com.scenic.dto.SwitchRoleDTO;
import com.scenic.entity.SysUser;
import com.scenic.entity.SysRole;
import com.scenic.mapper.SysUserMapper;
import com.scenic.mapper.SysRoleMapper;
import com.scenic.security.SecurityUtil;
import com.scenic.service.AuthService;
import com.scenic.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "认证管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功", null);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestParam String refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }

    @Operation(summary = "切换角色")
    @PostMapping("/switch-role")
    public Result<LoginVO> switchRole(@Valid @RequestBody SwitchRoleDTO dto) {
        Long userId = SecurityUtil.getUserId();
        return Result.success(authService.switchRole(userId, dto));
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile() {
        Long userId = SecurityUtil.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 获取用户角色
        List<SysRole> roles = sysRoleMapper.findRolesByUserId(userId);
        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("roles", roles);
        return Result.success(profile);
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (body.containsKey("username") && !body.get("username").equals(user.getUsername())) {
            // 检查用户名是否被占用
            Long count = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, body.get("username"))
            );
            if (count > 0) throw new BusinessException("该用户名已被使用");
            user.setUsername(body.get("username"));
        }
        if (body.containsKey("realName")) user.setRealName(body.get("realName"));
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        if (body.containsKey("email")) user.setEmail(body.get("email"));
        if (body.containsKey("avatarUrl")) {
            // 更换头像时，删除旧头像文件
            if (StrUtil.isNotBlank(user.getAvatarUrl())
                    && !user.getAvatarUrl().equals(body.get("avatarUrl"))) {
                deleteAvatarFile(user.getAvatarUrl());
            }
            user.setAvatarUrl(body.get("avatarUrl"));
        }
        sysUserMapper.updateById(user);
        return Result.success("个人信息更新成功", null);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            throw new BusinessException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("新密码长度至少6位");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return Result.success("密码修改成功", null);
    }

    @Operation(summary = "注册时上传头像(无需登录)")
    @PostMapping("/avatar/upload-temp")
    public Result<Map<String, String>> uploadAvatarTemp(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只允许上传图片文件");
        }

        try {
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve("avatars");
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = "temp_" + UUID.randomUUID().toString() + ext;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());

            String url = "/uploads/avatars/" + fileName;
            return Result.success("上传成功", Map.of("url", url));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "上传用户头像")
    @PostMapping("/avatar/upload")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只允许上传图片文件");
        }

        try {
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve("avatars");
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            // 查询用户以获取真实姓名，生成可读文件名: {realName}_头像.{ext}
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            String safeName = (StrUtil.isNotBlank(user.getRealName()) ? user.getRealName() : user.getUsername())
                    .replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = safeName + "_头像" + ext;
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());

            String url = "/uploads/avatars/" + fileName;

            // 删除旧头像文件并更新数据库
            if (StrUtil.isNotBlank(user.getAvatarUrl())) {
                deleteAvatarFile(user.getAvatarUrl());
            }
            user.setAvatarUrl(url);
            sysUserMapper.updateById(user);

            log.info("用户 {} 上传新头像: {}", userId, url);
            return Result.success("上传成功", Map.of("url", url));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地头像文件
     */
    private void deleteAvatarFile(String avatarPath) {
        if (StrUtil.isBlank(avatarPath)) return;
        try {
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(avatarPath.replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除旧头像文件失败: {}", avatarPath, e);
        }
    }
}
