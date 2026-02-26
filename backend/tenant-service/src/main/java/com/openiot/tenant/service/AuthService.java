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

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
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
        StpUtil.getSession().set("role", user.getRole());
        StpUtil.getSession().set("username", user.getUsername());

        log.info("用户登录成功: {} (role={})", username, user.getRole());

        return StpUtil.getTokenValue();
    }

    /**
     * 用户登出
     */
    public void logout() {
        String username = (String) StpUtil.getSession().get("username");
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
