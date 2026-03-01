package com.openiot.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.tenant.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     */
    @Select("""
        SELECT r.* FROM sys_role r
        INNER JOIN sys_user_role ur ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND r.delete_flag = '0'
          AND r.status = '1'
        ORDER BY r.sort_order
    """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);
}
