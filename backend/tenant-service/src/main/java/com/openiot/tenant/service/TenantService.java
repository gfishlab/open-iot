package com.openiot.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.tenant.entity.Tenant;
import com.openiot.tenant.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService extends ServiceImpl<TenantMapper, Tenant> {

    /**
     * 创建租户
     */
    public Tenant createTenant(Tenant tenant) {
        // 检查租户编码是否已存在
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getTenantCode, tenant.getTenantCode());
        if (this.count(wrapper) > 0) {
            throw BusinessException.badRequest("租户编码已存在: " + tenant.getTenantCode());
        }

        // 设置默认值
        if (tenant.getStatus() == null) {
            tenant.setStatus("1");
        }

        this.save(tenant);
        log.info("创建租户成功: {}", tenant.getTenantCode());
        return tenant;
    }

    /**
     * 根据编码查询租户
     */
    public Tenant getByCode(String tenantCode) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getTenantCode, tenantCode);
        return this.getOne(wrapper);
    }

    /**
     * 分页查询租户
     */
    public Page<Tenant> page(int pageNum, int pageSize, String status) {
        Page<Tenant> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Tenant::getStatus, status);
        }
        wrapper.orderByDesc(Tenant::getCreateTime);
        return this.page(page, wrapper);
    }

    /**
     * 更新租户状态
     */
    public void updateStatus(Long id, String status) {
        Tenant tenant = this.getById(id);
        if (tenant == null) {
            throw BusinessException.notFound("租户不存在: " + id);
        }
        tenant.setStatus(status);
        this.updateById(tenant);
        log.info("更新租户状态: {} -> {}", tenant.getTenantCode(), status);
    }
}
