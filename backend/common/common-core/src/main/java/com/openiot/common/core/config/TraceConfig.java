package com.openiot.common.core.config;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪配置
 * 为所有请求添加 Trace ID，支持分布式链路追踪
 */
@Slf4j
@Configuration
@ConditionalOnClass(Tracer.class)
public class TraceConfig {

    /**
     * 请求链路追踪过滤器
     * 为每个请求自动生成/传递 Trace ID
     */
    @Bean
    public TraceFilter traceFilter(Tracer tracer) {
        return new TraceFilter(tracer);
    }

    /**
     * 链路追踪过滤器
     */
    @RequiredArgsConstructor
    public static class TraceFilter extends OncePerRequestFilter {

        private final Tracer tracer;
        private static final String TRACE_ID_HEADER = "X-Trace-Id";
        private static final String TENANT_ID_HEADER = "X-Tenant-Id";

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            // 获取或创建 Trace ID
            String traceId = null;
            if (tracer.currentSpan() != null) {
                traceId = tracer.currentSpan().context().traceId();
            }

            // 设置响应头
            if (traceId != null) {
                response.setHeader(TRACE_ID_HEADER, traceId);
            }

            // 记录请求日志（包含 traceId）
            long startTime = System.currentTimeMillis();
            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                if (log.isDebugEnabled()) {
                    log.debug("[{}] {} {} - {}ms",
                            traceId,
                            request.getMethod(),
                            request.getRequestURI(),
                            duration);
                }
            }
        }
    }
}
