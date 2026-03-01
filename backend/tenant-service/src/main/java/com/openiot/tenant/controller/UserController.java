package com.openiot.tenant.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<SysUser> createUser(@RequestBody CreateUserRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setTenantId(request.getTenantId());
        user.setRole(request.getRole());

        SysUser created = userService.createUser(user);

        // 清除敏感信息
        created.setPassword(null);
        return ApiResponse.success("创建成功", created);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(request.getRealName());
        user.setPassword(request.getPassword());
        user.setStatus(request.getStatus());

        SysUser updated = userService.updateUser(user);

        updated.setPassword(null);
        return ApiResponse.success("更新成功", updated);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<SysUser> getUser(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    public ApiResponse<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        Page<SysUser> page = userService.page(pageNum, pageSize, tenantId, role, status);

        // 清除敏感信息
        page.getRecords().forEach(u -> u.setPassword(null));
        return ApiResponse.success(page);
    }

    /**
     * 查询租户下的所有用户
     */
    @GetMapping("/tenant/{tenantId}")
    public ApiResponse<List<SysUser>> listByTenant(@PathVariable Long tenantId) {
        List<SysUser> list = userService.listByTenantId(tenantId);
        list.forEach(u -> u.setPassword(null));
        return ApiResponse.success(list);
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return ApiResponse.success("状态更新成功", null);
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/password/reset")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return ApiResponse.success("密码重置成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse<SysUser> getCurrentUser() {
        if (!StpUtil.isLogin()) {
            return ApiResponse.error("未登录");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return ApiResponse.success(user);
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check-username")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ApiResponse.success(available);
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String realName;
        private Long tenantId;
        private String role;
    }

    @Data
    public static class UpdateUserRequest {
        private String realName;
        private String password;
        private String status;
    }

    @Data
    public static class StatusRequest {
        private String status;
    }

    @Data
    public static class ResetPasswordRequest {
        private String newPassword;
    }
}
