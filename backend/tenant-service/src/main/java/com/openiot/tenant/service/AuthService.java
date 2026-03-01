package com.openiot.tenant.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.common.security.context.TenantContext;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PermissionService permissionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return Token
     */
    public String login(String username, String password) {
        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 检查状态
        if ("0".equals(user.getStatus())) {
            throw BusinessException.unauthorized("账户已禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }

        // 登录
        StpUtil.login(user.getId());

        // 存储会话信息
        StpUtil.getSession().set("tenantId", user.getTenantId() != null ? String.valueOf(user.getTenantId()) : null);
        StpUtil.getSession().set("username", user.getUsername());

        // 从数据库动态加载角色和权限（支持多角色）
        List<String> roles = permissionService.getUserRoles(user.getId());
        List<String> permissions = permissionService.getUserPermissions(user.getId());

        StpUtil.getSession().set("roles", roles);
        StpUtil.getSession().set("permissions", permissions);

        // 兼容旧代码：保留单个 role 字段（取第一个角色）
        String primaryRole = roles.isEmpty() ? null : roles.get(0);
        StpUtil.getSession().set("role", primaryRole);

        log.info("用户登录成功: {} (roles={}, permissions={})", username, roles, permissions.size());

        return StpUtil.getTokenValue();
    }

    /**
     * 用户登出
     */
    public void logout() {
        if (!StpUtil.isLogin()) {
            return;
        }
        String username = (String) StpUtil.getSession().get("username");
        Long userId = StpUtil.getLoginIdAsLong();

        // 清除权限缓存
        permissionService.clearUserPermissionCache(userId);

        StpUtil.logout();
        log.info("用户登出: {}", username);
    }

    /**
     * 获取当前用户信息
     */
    public TenantContext.TenantInfo getCurrentUser() {
        if (!StpUtil.isLogin()) {
            return null;
        }

        TenantContext.TenantInfo info = new TenantContext.TenantInfo();
        info.setUserId(String.valueOf(StpUtil.getLoginId()));
        info.setTenantId((String) StpUtil.getSession().get("tenantId"));
        info.setRole((String) StpUtil.getSession().get("role"));
        info.setUsername((String) StpUtil.getSession().get("username"));
        return info;
    }
}
