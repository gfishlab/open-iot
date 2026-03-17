package com.openiot.common.feign.client;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.feign.dto.TenantDTO;
import com.openiot.common.feign.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 租户服务 Feign 客户端
 * <p>
 * 用于跨服务调用 tenant-service 的查询接口。
 * 通过 Nacos 服务发现自动路由到 tenant-service 实例。
 * </p>
 */
@FeignClient(name = "tenant-service", contextId = "tenantFeignClient")
public interface TenantFeignClient {

    /**
     * 查询租户信息
     *
     * @param tenantId 租户ID
     * @return 租户详情
     */
    @GetMapping("/api/v1/tenants/{tenantId}")
    ApiResponse<TenantDTO> getTenant(@PathVariable(name = "tenantId") Long tenantId);

    /**
     * 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @GetMapping("/api/v1/users/{userId}")
    ApiResponse<UserDTO> getUser(@PathVariable(name = "userId") Long userId);
}
