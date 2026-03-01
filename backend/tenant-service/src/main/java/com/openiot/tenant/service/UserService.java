package com.openiot.tenant.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.entity.Tenant;
import com.openiot.tenant.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final TenantService tenantService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 创建用户
     */
    public SysUser createUser(SysUser user) {
        // 校验用户名唯一性
        if (!isUsernameAvailable(user.getUsername())) {
            throw BusinessException.badRequest("用户名已存在: " + user.getUsername());
        }

        // 校验租户存在性
        if (user.getTenantId() != null) {
            Tenant tenant = tenantService.getById(user.getTenantId());
            if (tenant == null) {
                throw BusinessException.badRequest("租户不存在: " + user.getTenantId());
            }
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 设置默认值
        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("1");
        }
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("TENANT_ADMIN");
        }

        this.save(user);
        log.info("创建用户成功: {} (tenantId={}, role={})", user.getUsername(), user.getTenantId(), user.getRole());
        return user;
    }

    /**
     * 更新用户
     */
    public SysUser updateUser(SysUser user) {
        SysUser existing = this.getById(user.getId());
        if (existing == null) {
            throw BusinessException.notFound("用户不存在: " + user.getId());
        }

        // 不允许修改用户名
        user.setUsername(existing.getUsername());

        // 如果传了新密码，则加密
        if (StringUtils.hasText(user.getPassword()) && !user.getPassword().equals(existing.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(existing.getPassword());
        }

        // 保留不可修改的字段
        user.setTenantId(existing.getTenantId());
        user.setRole(existing.getRole());

        this.updateById(user);
        log.info("更新用户成功: {}", user.getUsername());
        return user;
    }

    /**
     * 删除用户（逻辑删除）
     */
    public void deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在: " + id);
        }

        // 不允许删除自己
        if (StpUtil.isLogin() && StpUtil.getLoginIdAsLong() == id) {
            throw BusinessException.badRequest("不能删除自己");
        }

        this.removeById(id);
        log.info("删除用户成功: {}", user.getUsername());
    }

    /**
     * 更新用户状态
     */
    public void updateStatus(Long id, String status) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在: " + id);
        }

        // 不允许禁用自己
        if (StpUtil.isLogin() && StpUtil.getLoginIdAsLong() == id && "0".equals(status)) {
            throw BusinessException.badRequest("不能禁用自己");
        }

        user.setStatus(status);
        this.updateById(user);
        log.info("更新用户状态: {} -> {}", user.getUsername(), status);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在: " + id);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        log.info("重置用户密码: {}", user.getUsername());
    }

    /**
     * 分页查询用户
     */
    public Page<SysUser> page(int pageNum, int pageSize, Long tenantId, String role, String status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (tenantId != null) {
            wrapper.eq(SysUser::getTenantId, tenantId);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysUser::getStatus, status);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 查询租户下的所有用户
     */
    public List<SysUser> listByTenantId(Long tenantId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getTenantId, tenantId);
        wrapper.orderByDesc(SysUser::getCreateTime);
        return this.list(wrapper);
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return this.count(wrapper) == 0;
    }

    /**
     * 根据用户名查询
     */
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return this.getOne(wrapper);
    }
}
