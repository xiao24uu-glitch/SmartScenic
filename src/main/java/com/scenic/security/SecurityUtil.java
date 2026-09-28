package com.scenic.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SecurityUtil {

    public Long getCurrentUserId() {
        return getUserId();
    }

    public String getCurrentUsername() {
        return getUsername();
    }

    /** 判断当前用户是否为管理员（ADMIN或MANAGER） */
    public static boolean isAdminOrManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) return false;
        return authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_MANAGER".equals(a.getAuthority()));
    }

    /** 静态获取当前用户ID */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /** 静态获取当前用户名 */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return (String) authentication.getDetails();
        }
        return null;
    }
}
