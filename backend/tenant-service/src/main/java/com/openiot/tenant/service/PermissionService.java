package com.openiot.tenant.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.tenant.entity.SysPermission;
import com.openiot.tenant.entity.SysRole;
import com.openiot.tenant.mapper.SysPermissionMapper;
import com.openiot.tenant.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限校验服务
 * 基于严格 RBAC 模型，从数据库动态加载权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService extends ServiceImpl<SysPermissionMapper, SysPermission> {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 缓存 Key 前缀
     */
    private static final String CACHE_PREFIX_ROLE = "openiot:role:";
    private static final String CACHE_PREFIX_PERMISSION = "openiot:permission:";
    private static final String CACHE_PREFIX_USER_PERMISSIONS = "openiot:user:permissions:";
    private static final String CACHE_PREFIX_USER_ROLES = "openiot:user:roles:";

    /**
     * 缓存过期时间（小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 2;

    // ==================== 角色常量 ====================

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_TENANT_USER = "TENANT_USER";

    // ==================== 公共方法 ====================

    /**
     * 获取用户的角色列表（从缓存或数据库）
     */
    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 尝试从缓存获取
        String cacheKey = CACHE_PREFIX_USER_ROLES + userId;
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 从数据库查询
        List<SysRole> roles = roleMapper.selectByUserId(userId);
        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, roleCodes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return roleCodes;
    }

    /**
     * 获取用户的权限列表（从缓存或数据库）
     */
    @SuppressWarnings("unchecked")
    public List<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 尝试从缓存获取
        String cacheKey = CACHE_PREFIX_USER_PERMISSIONS + userId;
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 从数据库查询
        List<SysPermission> permissions = permissionMapper.selectByUserId(userId);
        List<String> permissionCodes = permissions.stream()
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toList());

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, permissionCodes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return permissionCodes;
    }

    /**
     * 获取用户的完整权限信息（包含角色和权限）
     */
    public UserPermissionInfo getUserPermissionInfo(Long userId) {
        UserPermissionInfo info = new UserPermissionInfo();
        info.setUserId(userId);
        info.setRoles(getUserRoles(userId));
        info.setPermissions(getUserPermissions(userId));
        return info;
    }

    /**
     * 清除用户权限缓存
     */
    public void clearUserPermissionCache(Long userId) {
        redisTemplate.delete(CACHE_PREFIX_USER_ROLES + userId);
        redisTemplate.delete(CACHE_PREFIX_USER_PERMISSIONS + userId);
        log.info("清除用户权限缓存: userId={}", userId);
    }

    /**
     * 清除角色相关缓存（角色权限变更时调用）
     */
    public void clearRoleCache(Long roleId) {
        // 清除角色本身的缓存
        redisTemplate.delete(CACHE_PREFIX_ROLE + roleId);

        // 注意：这里需要清除所有拥有该角色的用户的权限缓存
        // 实际生产环境可以使用消息广播或 Canal 监听 binlog
        log.warn("角色缓存已清除，但用户权限缓存需要手动刷新: roleId={}", roleId);
    }

    // ==================== 权限校验方法 ====================

    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRole(String role) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        List<String> roles = getUserRoles(StpUtil.getLoginIdAsLong());
        return roles.contains(role);
    }

    /**
     * 检查用户是否拥有任意一个指定角色
     */
    public boolean hasAnyRole(String... roles) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        List<String> userRoles = getUserRoles(StpUtil.getLoginIdAsLong());
        return Arrays.stream(roles).anyMatch(userRoles::contains);
    }

    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermission(String permission) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        List<String> permissions = getUserPermissions(StpUtil.getLoginIdAsLong());
        return permissions.contains(permission);
    }

    /**
     * 检查用户是否拥有任意一个指定权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        List<String> userPermissions = getUserPermissions(StpUtil.getLoginIdAsLong());
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    /**
     * 检查用户是否拥有全部指定权限
     */
    public boolean hasAllPermissions(String... permissions) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        List<String> userPermissions = getUserPermissions(StpUtil.getLoginIdAsLong());
        return Arrays.stream(permissions).allMatch(userPermissions::contains);
    }

    /**
     * 校验用户是否拥有指定角色，无则抛出异常
     */
    public void checkRole(String role) {
        if (!hasRole(role)) {
            throw BusinessException.forbidden("无权访问：需要角色 " + role);
        }
    }

    /**
     * 校验用户是否拥有指定权限，无则抛出异常
     */
    public void checkPermission(String permission) {
        if (!hasPermission(permission)) {
            throw BusinessException.forbidden("无权访问：需要权限 " + permission);
        }
    }

    // ==================== 便捷方法 ====================

    /**
     * 判断当前用户是否为平台管理员
     */
    public boolean isPlatformAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    /**
     * 判断当前用户是否为租户管理员
     */
    public boolean isTenantAdmin() {
        return hasRole(ROLE_TENANT_ADMIN);
    }

    /**
     * 获取当前用户的租户ID
     */
    public String getCurrentTenantId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return (String) StpUtil.getSession().get("tenantId");
    }

    /**
     * 校验当前用户是否可以访问指定租户的数据
     */
    public void checkTenantAccess(String targetTenantId) {
        if (isPlatformAdmin()) {
            // 平台管理员可以访问所有租户数据
            return;
        }

        String currentTenantId = getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(targetTenantId)) {
            log.warn("租户越权访问: currentTenant={}, targetTenant={}", currentTenantId, targetTenantId);
            throw BusinessException.forbidden("无权访问该租户数据");
        }
    }

    // ==================== 角色管理方法 ====================

    /**
     * 获取所有角色列表
     */
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getDeleteFlag, "0")
                        .orderByAsc(SysRole::getSortOrder)
        );
    }

    /**
     * 根据角色编码获取角色
     */
    public SysRole getRoleByCode(String roleCode) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, roleCode)
                        .eq(SysRole::getDeleteFlag, "0")
        );
    }

    // ==================== 内部类 ====================

    /**
     * 用户权限信息
     */
    @lombok.Data
    public static class UserPermissionInfo {
        private Long userId;
        private List<String> roles;
        private List<String> permissions;
    }
}
