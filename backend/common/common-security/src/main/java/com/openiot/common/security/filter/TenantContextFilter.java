package com.openiot.common.security.filter;

import com.openiot.common.security.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * 从请求头中提取租户信息并设置到上下文
 */
@Slf4j
@Component
@Order(1)
public class TenantContextFilter implements Filter {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // 从请求头提取租户信息（由网关注入）
            String tenantId = httpRequest.getHeader(TENANT_ID_HEADER);
            String userId = httpRequest.getHeader(USER_ID_HEADER);
            String role = httpRequest.getHeader(USER_ROLE_HEADER);

            if (tenantId != null || userId != null) {
                TenantContext.TenantInfo info = new TenantContext.TenantInfo();
                info.setTenantId(tenantId);
                info.setUserId(userId);
                info.setRole(role);
                TenantContext.setTenant(info);

                log.debug("设置租户上下文: tenantId={}, userId={}, role={}", tenantId, userId, role);
            }

            chain.doFilter(request, response);
        } finally {
            // 清除上下文
            TenantContext.clear();
        }
    }
}
