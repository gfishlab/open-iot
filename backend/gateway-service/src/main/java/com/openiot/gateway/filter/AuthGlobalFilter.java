package com.openiot.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 认证全局过滤器
 *
 * <p>职责：
 * <ul>
 *   <li>验证 Token 有效性</li>
 *   <li>解析用户信息（tenantId, userId, role）</li>
 *   <li>注入身份 Header 传递给下游服务</li>
 * </ul>
 *
 * <p>放行路径：
 * <ul>
 *   <li>/api/v1/auth/login - 登录接口</li>
 *   <li>/api/v1/auth/register - 注册接口</li>
 *   <li>/actuator/** - 健康检查</li>
 * </ul>
 *
 * @see constitution.md VII. 认证边界与职责划分原则
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 排除不需要认证的路径
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            return unauthorized(exchange, "未授权访问");
        }

        try {
            // 验证 Token
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return unauthorized(exchange, "Token 无效或已过期");
            }

            // 获取用户信息（直接通过 loginId 获取 Session，不依赖上下文）
            String userId = String.valueOf(loginId);
            cn.dev33.satoken.session.SaSession session = StpUtil.getSessionByLoginId(loginId, false);
            if (session == null) {
                return unauthorized(exchange, "Session 不存在或已过期");
            }

            String tenantId = (String) session.get("tenantId");
            String role = (String) session.get("role");

            // 只注入有值的 header，避免空字符串传递到下游引发解析异常
            ServerHttpRequest.Builder requestBuilder = request.mutate()
                    .header("X-User-Id", userId);
            if (tenantId != null && !tenantId.isEmpty()) {
                requestBuilder.header("X-Tenant-Id", tenantId);
            }
            if (role != null && !role.isEmpty()) {
                requestBuilder.header("X-User-Role", role);
            }
            ServerHttpRequest newRequest = requestBuilder.build();

            log.debug("认证成功: path={}, tenantId={}, userId={}", path, tenantId, userId);
            return chain.filter(exchange.mutate().request(newRequest).build());

        } catch (Exception e) {
            log.warn("认证失败: {} - {}", path, e.getMessage());
            return unauthorized(exchange, "认证失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为排除路径（不需要认证）
     *
     * @param path 请求路径
     * @return true 表示放行，false 表示需要认证
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/api/v1/auth/login") ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/fallback/") ||
               path.startsWith("/error");
    }

    /**
     * 返回未授权响应
     *
     * @param exchange ServerWebExchange
     * @param message 错误消息
     * @return Mono<Void>
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setCode(401);
        errorResponse.setMsg(message);
        errorResponse.setTimestamp(System.currentTimeMillis());

        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 错误响应 VO
     */
    @Data
    public static class ErrorResponse {
        private Integer code;
        private String msg;
        private Long timestamp;
    }
}
