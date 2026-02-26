package com.openiot.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.tenant.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户 Mapper
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
