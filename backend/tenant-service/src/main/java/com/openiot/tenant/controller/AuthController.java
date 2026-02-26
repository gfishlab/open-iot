package com.openiot.tenant.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.tenant.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());

        LoginResponse response = new LoginResponse();
        response.setToken(token);

        TenantContext.TenantInfo userInfo = authService.getCurrentUser();
        if (userInfo != null) {
            response.setUserId(Long.valueOf(userInfo.getUserId()));
            response.setUsername(userInfo.getUsername());
            response.setRole(userInfo.getRole());
            response.setTenantId(userInfo.getTenantId() != null ? Long.valueOf(userInfo.getTenantId()) : null);
        }

        return ApiResponse.success("登录成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("登出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<TenantContext.TenantInfo> getCurrentUser() {
        TenantContext.TenantInfo info = authService.getCurrentUser();
        if (info == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return ApiResponse.success(info);
    }

    /**
     * 登录请求
     */
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 登录响应
     */
    @Data
    public static class LoginResponse {
        private String token;
        private Long userId;
        private String username;
        private String realName;
        private String role;
        private Long tenantId;
    }
}
