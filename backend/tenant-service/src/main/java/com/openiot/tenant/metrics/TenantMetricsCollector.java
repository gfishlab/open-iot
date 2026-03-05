package com.openiot.tenant.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.entity.Tenant;
import com.openiot.tenant.mapper.SysUserMapper;
import com.openiot.tenant.mapper.TenantMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 租户与用户基础业务指标采集器。
 */
@Slf4j
@Component
public class TenantMetricsCollector {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;

    private final AtomicLong totalTenants = new AtomicLong(0);
    private final AtomicLong activeTenants = new AtomicLong(0);
    private final AtomicLong totalUsers = new AtomicLong(0);

    public TenantMetricsCollector(MeterRegistry meterRegistry,
                                  TenantMapper tenantMapper,
                                  SysUserMapper sysUserMapper) {
        this.tenantMapper = tenantMapper;
        this.sysUserMapper = sysUserMapper;

        Gauge.builder("openiot_tenant_total_count", totalTenants, AtomicLong::get)
                .description("Total number of tenants")
                .tag("service", "tenant-service")
                .register(meterRegistry);

        Gauge.builder("openiot_tenant_active_count", activeTenants, AtomicLong::get)
                .description("Number of active tenants")
                .tag("service", "tenant-service")
                .register(meterRegistry);

        Gauge.builder("openiot_user_total_count", totalUsers, AtomicLong::get)
                .description("Total number of users")
                .tag("service", "tenant-service")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 30000, initialDelay = 5000)
    public void updateMetrics() {
        try {
            long tenantTotal = tenantMapper.selectCount(null);
            long tenantActive = tenantMapper.selectCount(
                    new LambdaQueryWrapper<Tenant>().eq(Tenant::getStatus, "1")
            );
            long userTotal = sysUserMapper.selectCount(null);

            totalTenants.set(tenantTotal);
            activeTenants.set(tenantActive);
            totalUsers.set(userTotal);
        } catch (Exception e) {
            log.error("Failed to update tenant metrics", e);
        }
    }
}
