package com.openiot.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.tenant.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据用户ID查询权限列表（通过角色关联）
     */
    @Select("""
        SELECT DISTINCT p.* FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND p.delete_flag = '0'
          AND p.status = '1'
        ORDER BY p.sort_order
    """)
    List<SysPermission> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     */
    @Select("""
        SELECT p.* FROM sys_permission p
        INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
        WHERE rp.role_id = #{roleId}
          AND p.delete_flag = '0'
          AND p.status = '1'
        ORDER BY p.sort_order
    """)
    List<SysPermission> selectByRoleId(@Param("roleId") Long roleId);
}
