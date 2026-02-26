package com.openiot.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.tenant.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
