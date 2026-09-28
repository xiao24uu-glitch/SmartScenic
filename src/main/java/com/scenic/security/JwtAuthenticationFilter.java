package com.scenic.security;

import com.scenic.common.Result;
import com.scenic.entity.SysRole;
import com.scenic.mapper.SysRoleMapper;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SysRoleMapper sysRoleMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                Long currentRoleId = jwtUtil.getCurrentRoleId(token);

                // 加载用户所有角色
                List<SysRole> roles = sysRoleMapper.findRolesByUserId(userId);
                
                // 如果用户切换了角色，只使用当前选择的角色
                if (currentRoleId != null) {
                    roles = roles.stream()
                            .filter(r -> r.getId().equals(currentRoleId))
                            .toList();
                    // 如果切换的角色不在用户的角色列表中，保持全部角色
                    if (roles.isEmpty()) {
                        roles = sysRoleMapper.findRolesByUserId(userId);
                    }
                }
                
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.error("JWT认证失败: {}", e.getMessage());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(Result.unauthorized("Token无效或已过期")));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
