package com.scenic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.common.exception.BusinessException;
import com.scenic.dto.LoginDTO;
import com.scenic.dto.RegisterDTO;
import com.scenic.dto.SwitchRoleDTO;
import com.scenic.entity.*;
import com.scenic.mapper.*;
import com.scenic.security.JwtUtil;
import com.scenic.security.SecurityUtil;
import com.scenic.service.AuthService;
import com.scenic.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final TouristMapper touristMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, loginDTO.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 加载用户所有角色，按 roleLevel 升序排列（level越小权限越高）
        List<SysRole> roles = sysRoleMapper.findRolesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException("用户未分配角色");
        }
        roles.sort(Comparator.comparingInt(SysRole::getRoleLevel));
        
        // 最高角色作为当前角色
        SysRole primaryRole = roles.get(0);
        Long effectiveRoleId = user.getCurrentRoleId() != null ? user.getCurrentRoleId() : primaryRole.getId();
        SysRole effectiveRole = roles.stream()
                .filter(r -> r.getId().equals(effectiveRoleId))
                .findFirst().orElse(primaryRole);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(),
                effectiveRole.getId(), effectiveRole.getRoleLevel());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 可切换的角色列表：用户拥有的所有角色（去重）
        List<LoginVO.RoleInfo> availableRoles = buildAvailableRoles(roles);

        return LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleName(effectiveRole.getRoleName())
                .roleCode(effectiveRole.getRoleCode())
                .roleLevel(effectiveRole.getRoleLevel())
                .currentRoleId(effectiveRole.getId())
                .availableRoles(availableRoles)
                .build();
    }

    @Override
    @Transactional
    public LoginVO switchRole(Long userId, SwitchRoleDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException("用户不存在或已禁用");
        }

        List<SysRole> roles = sysRoleMapper.findRolesByUserId(userId);
        if (roles.isEmpty()) {
            throw new BusinessException("用户未分配角色");
        }
        roles.sort(Comparator.comparingInt(SysRole::getRoleLevel));
        SysRole highestRole = roles.get(0);

        // 验证目标角色是否属于该用户
        SysRole targetRole = roles.stream()
                .filter(r -> r.getId().equals(dto.getRoleId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("无权切换到此角色"));

        // 更新数据库中的当前角色
        user.setCurrentRoleId(dto.getRoleId());
        sysUserMapper.updateById(user);

        // 生成新Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(),
                targetRole.getId(), targetRole.getRoleLevel());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        List<LoginVO.RoleInfo> availableRoles = buildAvailableRoles(roles);

        log.info("用户 {} 切换到角色: {} (角色ID: {})", user.getUsername(), targetRole.getRoleName(), targetRole.getId());

        return LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleName(targetRole.getRoleName())
                .roleCode(targetRole.getRoleCode())
                .roleLevel(targetRole.getRoleLevel())
                .currentRoleId(targetRole.getId())
                .availableRoles(availableRoles)
                .build();
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, registerDTO.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setAvatarUrl(registerDTO.getAvatarUrl());
        user.setStatus(1);
        sysUserMapper.insert(user);

        // 分配游客角色（按 roleCode 动态查询，避免硬编码 roleId）
        SysRole touristRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "TOURIST")
        );
        if (touristRole == null) {
            throw new BusinessException("系统角色配置异常，请联系管理员");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(touristRole.getId());
        sysUserRoleMapper.insert(userRole);

        // 创建游客记录
        Tourist tourist = new Tourist();
        tourist.setUserId(user.getId());
        tourist.setRealName(registerDTO.getRealName());
        tourist.setPhone(registerDTO.getPhone());
        tourist.setFaceStatus(0);
        touristMapper.insert(tourist);

        log.info("新用户注册成功: {}", user.getUsername());
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "Refresh Token无效或已过期");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户不存在或已禁用");
        }

        List<SysRole> roles = sysRoleMapper.findRolesByUserId(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(401, "用户未分配角色");
        }
        roles.sort(Comparator.comparingInt(SysRole::getRoleLevel));
        SysRole primaryRole = roles.get(0);

        Long effectiveRoleId = user.getCurrentRoleId() != null ? user.getCurrentRoleId() : primaryRole.getId();
        SysRole effectiveRole = roles.stream()
                .filter(r -> r.getId().equals(effectiveRoleId))
                .findFirst().orElse(primaryRole);

        String newToken = jwtUtil.generateToken(userId, username,
                effectiveRole.getId(), effectiveRole.getRoleLevel());
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        List<LoginVO.RoleInfo> availableRoles = buildAvailableRoles(roles);

        return LoginVO.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roleName(effectiveRole.getRoleName())
                .roleCode(effectiveRole.getRoleCode())
                .roleLevel(effectiveRole.getRoleLevel())
                .currentRoleId(effectiveRole.getId())
                .availableRoles(availableRoles)
                .build();
    }

    /** 构建可切换角色列表（按 level 升序，去重） */
    private List<LoginVO.RoleInfo> buildAvailableRoles(List<SysRole> roles) {
        return roles.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparingLong(SysRole::getId))),
                        ArrayList::new))
                .stream()
                .sorted(Comparator.comparingInt(SysRole::getRoleLevel))
                .map(r -> LoginVO.RoleInfo.builder()
                        .roleId(r.getId())
                        .roleName(r.getRoleName())
                        .roleCode(r.getRoleCode())
                        .roleLevel(r.getRoleLevel())
                        .build())
                .toList();
    }
}
