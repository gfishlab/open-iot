package com.openiot.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.entity.Device;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.DeviceMapper;
import com.openiot.device.service.DeviceService;
import com.openiot.device.service.ProductDetailVO;
import com.openiot.device.service.ProductService;
import com.openiot.device.service.ProductStatisticsVO;
import com.openiot.device.vo.ProductCreateVO;
import com.openiot.device.vo.ProductUpdateVO;
import com.openiot.device.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品控制器
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "产品管理", description = "产品 CRUD 接口")
public class ProductController {

    private final ProductService productService;
    private final DeviceMapper deviceMapper;

    /**
     * 创建产品
     */
    @PostMapping
    @Operation(summary = "创建产品", description = "创建新产品并生成产品密钥")
    public ApiResponse<ProductVO> createProduct(@Valid @RequestBody ProductCreateVO vo) {
        log.info("创建产品请求: {}", vo.getProductName());
        Product product = productService.createProduct(vo);
        ProductVO productVO = convertToVO(product);
        return ApiResponse.success(productVO);
    }

    /**
     * 根据ID查询产品
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询产品详情", description = "根据产品ID查询产品详细信息")
    public ApiResponse<ProductVO> getProductById(
        @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {
        log.info("查询产品详情: id={}", id);
        Product product = productService.getProductById(id);
        ProductVO productVO = convertToVO(product);

        // 查询关联设备数量
        int deviceCount = productService.getDeviceCount(id);
        productVO.setDeviceCount(deviceCount);

        return ApiResponse.success(productVO);
    }

    /**
     * 更新产品
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新产品", description = "更新产品信息")
    public ApiResponse<ProductVO> updateProduct(
        @Parameter(description = "产品ID") @PathVariable(name = "id") Long id,
        @Valid @RequestBody ProductUpdateVO vo) {
        log.info("更新产品请求: id={}, name={}", id, vo.getProductName());
        Product product = productService.updateProduct(id, vo);
        ProductVO productVO = convertToVO(product);
        return ApiResponse.success(productVO);
    }

    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除产品", description = "删除产品（检查关联设备）")
    public ApiResponse<Void> deleteProduct(
        @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {
        log.info("删除产品请求: id={}", id);
        productService.deleteProduct(id);
        return ApiResponse.success(null);
    }

    /**
     * 分页查询产品列表
     */
    @GetMapping
    @Operation(summary = "分页查询产品", description = "支持按名称、协议类型、状态过滤")
    public ApiResponse<Page<ProductVO>> getProductList(
        @Parameter(description = "页码") @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
        @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
        @Parameter(description = "产品名称") @RequestParam(name = "productName", required = false) String productName,
        @Parameter(description = "协议类型") @RequestParam(name = "protocolType", required = false) String protocolType,
        @Parameter(description = "状态") @RequestParam(name = "status", required = false) String status) {

        log.info("查询产品列表: pageNum={}, pageSize={}, name={}, protocol={}, status={}",
            pageNum, pageSize, productName, protocolType, status);

        Page<Product> productPage = productService.getProductList(
            pageNum, pageSize, productName, protocolType, status);

        // 转换为 VO
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductVO> voList = productPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        // 批量查询设备数量
        List<Long> productIds = voList.stream()
            .map(ProductVO::getId)
            .collect(Collectors.toList());

        if (!productIds.isEmpty()) {
            Map<Long, Long> deviceCountMap = batchQueryDeviceCount(productIds);
            voList.forEach(vo -> vo.setDeviceCount(deviceCountMap.getOrDefault(vo.getId(), 0L).intValue()));
        }

        voPage.setRecords(voList);
        return ApiResponse.success(voPage);
    }

    /**
     * 查询产品关联的设备列表
     */
    @GetMapping("/{id}/devices")
    @Operation(summary = "查询产品设备", description = "查询指定产品关联的所有设备")
    public ApiResponse<List<Device>> getProductDevices(
        @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {

        log.info("查询产品设备列表: productId={}", id);

        // 验证产品存在
        productService.getProductById(id);

        // 查询关联设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getProductId, id)
               .orderByDesc(Device::getCreateTime);

        List<Device> devices = deviceMapper.selectList(wrapper);
        return ApiResponse.success(devices);
    }

    /**
     * 转换 Product 到 ProductVO
     */
    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    /**
     * 批量查询设备数量
     */
    private Map<Long, Long> batchQueryDeviceCount(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return new HashMap<>();
        }

        // 使用 SQL 聚合查询设备数量
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Device::getProductId, productIds);
        wrapper.select(Device::getProductId);

        List<Device> devices = deviceMapper.selectList(wrapper);

        // 统计每个产品的设备数量
        Map<Long, Long> countMap = devices.stream()
            .collect(Collectors.groupingBy(Device::getProductId, Collectors.counting()));

        return countMap;
    }

    /**
     * 查询产品详情（包含统计信息）
     */
    @GetMapping("/{id}/detail")
    @Operation(summary = "查询产品详情", description = "查询产品详细信息及统计")
    public ApiResponse<ProductDetailVO> getProductDetail(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {

        log.info("查询产品详情: productId={}", id);

        ProductDetailVO detail = productService.getProductDetail(id);
        return ApiResponse.success(detail);
    }

    /**
     * 查询产品设备分页列表
     */
    @GetMapping("/{id}/devices/page")
    @Operation(summary = "查询产品设备（分页）", description = "分页查询产品关联的设备")
    public ApiResponse<Page<Device>> getProductDevicesPage(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id,
            @Parameter(description = "页码") @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {

        log.info("查询产品设备列表: productId={}, pageNum={}, pageSize={}", id, pageNum, pageSize);

        Page<Device> devices = productService.getProductDevices(id, pageNum, pageSize);
        return ApiResponse.success(devices);
    }

    /**
     * 更新产品状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新产品状态", description = "启用或禁用产品")
    public ApiResponse<Void> updateStatus(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id,
            @RequestBody StatusUpdateRequest request) {

        log.info("更新产品状态: productId={}, status={}", id, request.getStatus());

        productService.updateStatus(id, request.getStatus());
        return ApiResponse.success("产品状态更新成功", null);
    }

    /**
     * 查询产品统计
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "产品统计", description = "查询产品设备统计信息")
    public ApiResponse<ProductStatisticsVO> getStatistics(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {

        log.info("查询产品统计: productId={}", id);

        ProductStatisticsVO statistics = productService.getStatistics(id);
        return ApiResponse.success(statistics);
    }

    /**
     * 状态更新请求
     */
    @Data
    public static class StatusUpdateRequest {
        private String status;  // 1-启用，0-禁用
    }

    /**
     * 获取产品物模型
     */
    @GetMapping("/{id}/thing-model")
    @Operation(summary = "获取物模型", description = "获取产品的物模型定义")
    public ApiResponse<Object> getThingModel(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id) {

        log.info("获取产品物模型: productId={}", id);

        Product product = productService.getProductById(id);
        return ApiResponse.success(product.getThingModel());
    }

    /**
     * 更新产品物模型
     */
    @PutMapping("/{id}/thing-model")
    @Operation(summary = "更新物模型", description = "更新产品的物模型定义")
    public ApiResponse<Void> updateThingModel(
            @Parameter(description = "产品ID") @PathVariable(name = "id") Long id,
            @RequestBody JsonNode thingModel) {

        log.info("更新产品物模型: productId={}", id);

        productService.updateThingModel(id, thingModel);
        return ApiResponse.success("物模型更新成功", null);
    }
}
