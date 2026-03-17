package com.openiot.common.feign.client;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.feign.dto.DeviceDTO;
import com.openiot.common.feign.dto.DeviceShadowDTO;
import com.openiot.common.feign.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 设备服务 Feign 客户端
 * <p>
 * 用于跨服务调用 device-service 的查询接口。
 * 通过 Nacos 服务发现自动路由到 device-service 实例。
 * </p>
 */
@FeignClient(name = "device-service", contextId = "deviceFeignClient")
public interface DeviceFeignClient {

    /**
     * 查询设备信息
     *
     * @param deviceId 设备ID
     * @return 设备详情
     */
    @GetMapping("/api/v1/devices/{deviceId}")
    ApiResponse<DeviceDTO> getDevice(@PathVariable(name = "deviceId") Long deviceId);

    /**
     * 查询产品信息
     *
     * @param productId 产品ID
     * @return 产品详情
     */
    @GetMapping("/api/v1/products/{productId}")
    ApiResponse<ProductDTO> getProduct(@PathVariable(name = "productId") Long productId);

    /**
     * 查询设备影子
     *
     * @param deviceId 设备ID
     * @return 设备影子详情
     */
    @GetMapping("/api/v1/devices/{deviceId}/shadow")
    ApiResponse<DeviceShadowDTO> getDeviceShadow(@PathVariable(name = "deviceId") Long deviceId);
}
