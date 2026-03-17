package com.openiot.device.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.common.security.context.TenantContext;
import com.openiot.device.entity.AlertRecord;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.service.AlertService;
import com.openiot.device.service.DeviceService;
import com.openiot.device.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表板控制器
 * 提供首页统计数据接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表板", description = "首页统计数据接口")
public class DashboardController {

    private final DeviceService deviceService;
    private final ProductService productService;
    private final AlertService alertService;

    /**
     * 获取首页统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据", description = "获取设备、产品、告警的统计数据")
    public ApiResponse<Map<String, Object>> getStatistics() {
        String tenantId = TenantContext.getTenantId();
        Map<String, Object> stats = new HashMap<>();

        try {
            // 设备统计
            long totalDevices = deviceService.lambdaQuery()
                    .eq(tenantId != null, Device::getTenantId, tenantId)
                    .count();
            // Device 实体没有 onlineStatus 字段，暂时用 lastActiveTime 不为空作为在线判断
            long onlineDevices = deviceService.lambdaQuery()
                    .eq(tenantId != null, Device::getTenantId, tenantId)
                    .isNotNull(Device::getLastActiveTime)
                    .count();

            // 产品统计
            long totalProducts = productService.lambdaQuery()
                    .eq(tenantId != null, Product::getTenantId, tenantId)
                    .count();
            long activeProducts = productService.lambdaQuery()
                    .eq(tenantId != null, Product::getTenantId, tenantId)
                    .eq(Product::getStatus, "1")
                    .count();

            // 告警统计
            long todayAlerts = alertService.lambdaQuery()
                    .eq(tenantId != null, AlertRecord::getTenantId, tenantId)
                    .count();
            long pendingAlerts = alertService.lambdaQuery()
                    .eq(tenantId != null, AlertRecord::getTenantId, tenantId)
                    .eq(AlertRecord::getStatus, "pending")
                    .count();

            stats.put("totalDevices", totalDevices);
            stats.put("onlineDevices", onlineDevices);
            stats.put("totalProducts", totalProducts);
            stats.put("activeProducts", activeProducts);
            stats.put("todayAlerts", todayAlerts);
            stats.put("pendingAlerts", pendingAlerts);

        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            stats.put("totalDevices", 0);
            stats.put("onlineDevices", 0);
            stats.put("totalProducts", 0);
            stats.put("activeProducts", 0);
            stats.put("todayAlerts", 0);
            stats.put("pendingAlerts", 0);
        }

        return ApiResponse.success(stats);
    }
}
