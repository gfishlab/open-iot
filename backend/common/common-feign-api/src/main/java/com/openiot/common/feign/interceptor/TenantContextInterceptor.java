package com.openiot.common.feign.interceptor;

import com.openiot.common.security.context.TenantContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/**
 * Feign 请求拦截器 - 自动传递租户上下文
 * <p>
 * 从 TenantContext（TransmittableThreadLocal）读取当前租户信息，
 * 注入到 Feign 请求头中，下游服务的 TenantContextFilter 自动解析。
 * </p>
 */
@Component
public class TenantContextInterceptor implements RequestInterceptor {

    /** 请求头常量 */
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    public void apply(RequestTemplate template) {
        TenantContext.TenantInfo tenant = TenantContext.getTenant();
        if (tenant == null) {
            return;
        }

        // 注入租户ID
        if (tenant.getTenantId() != null) {
            template.header(HEADER_TENANT_ID, tenant.getTenantId());
        }

        // 注入用户ID
        if (tenant.getUserId() != null) {
            template.header(HEADER_USER_ID, tenant.getUserId());
        }

        // 注入用户角色
        if (tenant.getRole() != null) {
            template.header(HEADER_USER_ROLE, tenant.getRole());
        }
    }
}
