package com.scenic.service;

import com.scenic.dto.LoginDTO;
import com.scenic.dto.RegisterDTO;
import com.scenic.dto.SwitchRoleDTO;
import com.scenic.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
    LoginVO refreshToken(String refreshToken);
    LoginVO switchRole(Long userId, SwitchRoleDTO dto);
}
