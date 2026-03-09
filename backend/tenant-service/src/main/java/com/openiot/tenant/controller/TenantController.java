package com.openiot.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.tenant.entity.Tenant;
import com.openiot.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理控制器
 */
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * 创建租户
     */
    @PostMapping
    public ApiResponse<Tenant> create(@RequestBody Tenant tenant) {
        Tenant created = tenantService.createTenant(tenant);
        return ApiResponse.success("租户创建成功", created);
    }

    /**
     * 查询租户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Tenant> getById(@PathVariable(value = "id") Long id) {
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return ApiResponse.notFound("租户不存在");
        }
        return ApiResponse.success(tenant);
    }

    /**
     * 分页查询租户列表
     */
    @GetMapping
    public ApiResponse<Page<Tenant>> page(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status) {
        Page<Tenant> result = tenantService.page(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 更新租户
     */
    @PutMapping("/{id}")
    public ApiResponse<Tenant> update(@PathVariable(value = "id") Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        tenantService.updateById(tenant);
        return ApiResponse.success("租户更新成功", tenant);
    }

    /**
     * 禁用租户
     */
    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable(value = "id") Long id) {
        tenantService.updateStatus(id, "0");
        return ApiResponse.success("租户已禁用", null);
    }

    /**
     * 启用租户
     */
    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable(value = "id") Long id) {
        tenantService.updateStatus(id, "1");
        return ApiResponse.success("租户已启用", null);
    }
}
