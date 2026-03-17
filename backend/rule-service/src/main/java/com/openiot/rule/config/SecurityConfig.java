package com.openiot.rule.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.openiot.common.security.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.List;

/**
 * 安全配置类
 * 注册租户上下文过滤器，从请求头或 Sa-Token Session 中提取租户信息
 */
@Slf4j
@Configuration
public class SecurityConfig {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * 注册租户上下文过滤器
     */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.addUrlPatterns("/*");
        registration.setName("tenantContextFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * 租户上下文过滤器
     * 支持从 HTTP Headers 或 Sa-Token Session 读取租户信息
     */
    @Slf4j
    public static class TenantContextFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            try {
                // 1. 优先从请求头提取租户信息（由网关注入）
                String tenantId = httpRequest.getHeader(TENANT_ID_HEADER);
                String userId = httpRequest.getHeader(USER_ID_HEADER);
                String role = httpRequest.getHeader(USER_ROLE_HEADER);

                // 2. 如果 headers 为空，尝试从 Sa-Token Session 读取
                if ((tenantId == null || tenantId.isEmpty()) && (userId == null || userId.isEmpty())) {
                    String token = httpRequest.getHeader(AUTHORIZATION_HEADER);
                    if (token != null && !token.isEmpty()) {
                        try {
                            Object loginId = StpUtil.getLoginIdByToken(token);
                            if (loginId != null) {
                                SaSession session = StpUtil.getSessionByLoginId(loginId, false);
                                if (session != null) {
                                    Object sessionTenantId = session.get("tenantId");
                                    Object sessionUserId = session.get("userId");
                                    Object sessionRole = session.get("roles");

                                    if (sessionTenantId != null) {
                                        tenantId = String.valueOf(sessionTenantId);
                                    }
                                    if (sessionUserId != null) {
                                        userId = String.valueOf(sessionUserId);
                                    }
                                    if (sessionRole != null) {
                                        if (sessionRole instanceof List) {
                                            @SuppressWarnings("unchecked")
                                            List<String> roles = (List<String>) sessionRole;
                                            role = roles.isEmpty() ? null : roles.get(0);
                                        } else {
                                            role = String.valueOf(sessionRole);
                                        }
                                    }

                                    log.debug("从 Sa-Token Session 读取租户上下文: tenantId={}, userId={}, role={}",
                                            tenantId, userId, role);
                                }
                            }
                        } catch (Exception e) {
                            log.debug("读取 Sa-Token Session 失败: {}", e.getMessage());
                        }
                    }
                }

                // 3. 设置租户上下文
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
                TenantContext.clear();
            }
        }
    }
}
