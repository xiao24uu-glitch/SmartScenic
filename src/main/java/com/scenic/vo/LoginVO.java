package com.scenic.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginVO {
    private String token;
    private String refreshToken;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String roleName;
    private String roleCode;
    private Integer roleLevel;
    /** 所有拥有的角色列表，用于角色切换 */
    private List<RoleInfo> availableRoles;
    /** 当前生效的角色ID */
    private Long currentRoleId;

    @Data
    @Builder
    public static class RoleInfo {
        private Long roleId;
        private String roleName;
        private String roleCode;
        private Integer roleLevel;
    }
}
