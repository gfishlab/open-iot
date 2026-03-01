package com.openiot.common.security.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限校验实现类
 * 从 Session 中获取用户的权限和角色列表
 *
 * 注意：角色和权限在用户登录时存入 Session
 * @see com.openiot.tenant.service.AuthService#login
 * @see com.openiot.tenant.service.PermissionService
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * Session 中的角色 Key（支持多角色）
     */
    private static final String SESSION_ROLES_KEY = "roles";

    /**
     * Session 中的权限列表 Key
     */
    private static final String SESSION_PERMISSIONS_KEY = "permissions";

    /**
     * 返回用户的权限码集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 权限码集合
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!StpUtil.isLogin()) {
            return new ArrayList<>();
        }

        try {
            Object permissions = StpUtil.getSession().get(SESSION_PERMISSIONS_KEY);
            if (permissions instanceof List) {
                return (List<String>) permissions;
            }
        } catch (Exception e) {
            // ignore
        }

        return new ArrayList<>();
    }

    /**
     * 返回用户的角色标识集合
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 角色标识集合
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!StpUtil.isLogin()) {
            return new ArrayList<>();
        }

        try {
            Object roles = StpUtil.getSession().get(SESSION_ROLES_KEY);
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        } catch (Exception e) {
            // ignore
        }

        return new ArrayList<>();
    }
}
