package com.openiot.device.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.service.ThingModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 物模型控制器
 *
 * @author OpenIoT Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products/{productId}/thing-model")
@RequiredArgsConstructor
@Tag(name = "物模型管理", description = "产品物模型 CRUD 接口")
public class ThingModelController {

    private final ThingModelService thingModelService;

    /**
     * 保存产品物模型（JSON格式）
     */
    @PutMapping
    @Operation(summary = "保存物模型", description = "保存或更新产品的物模型定义")
    public ApiResponse<Void> saveThingModel(
            @Parameter(description = "产品ID") @PathVariable(name = "productId") Long productId,
            @RequestBody String thingModelJson) {

        log.info("保存物模型: productId={}", productId);
        thingModelService.saveThingModel(productId, thingModelJson);
        return ApiResponse.success("物模型保存成功", null);
    }

    /**
     * 查询产品物模型
     */
    @GetMapping
    @Operation(summary = "查询物模型", description = "查询产品的完整物模型定义")
    public ApiResponse<Map<String, Object>> getThingModel(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        log.info("查询物模型: productId={}", productId);

        JsonNode thingModel = thingModelService.getThingModel(productId);
        if (thingModel == null) {
            // 返回空物模型结构
            Map<String, Object> emptyModel = new HashMap<>();
            emptyModel.put("properties", thingModelService.getProperties(productId));
            emptyModel.put("events", thingModelService.getEvents(productId));
            emptyModel.put("services", thingModelService.getServices(productId));
            return ApiResponse.success(emptyModel);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("properties", thingModelService.getProperties(productId));
        result.put("events", thingModelService.getEvents(productId));
        result.put("services", thingModelService.getServices(productId));

        return ApiResponse.success(result);
    }

    /**
     * 删除产品物模型
     */
    @DeleteMapping
    @Operation(summary = "删除物模型", description = "删除产品的物模型定义")
    public ApiResponse<Void> deleteThingModel(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        log.info("删除物模型: productId={}", productId);
        thingModelService.deleteThingModel(productId);
        return ApiResponse.success("物模型删除成功", null);
    }

    /**
     * 查询产品属性列表
     */
    @GetMapping("/properties")
    @Operation(summary = "查询属性列表", description = "查询产品的属性定义列表")
    public ApiResponse<JsonNode> getProperties(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        log.info("查询属性列表: productId={}", productId);
        JsonNode properties = thingModelService.getProperties(productId);
        return ApiResponse.success(properties);
    }

    /**
     * 查询产品事件列表
     */
    @GetMapping("/events")
    @Operation(summary = "查询事件列表", description = "查询产品的事件定义列表")
    public ApiResponse<JsonNode> getEvents(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        log.info("查询事件列表: productId={}", productId);
        JsonNode events = thingModelService.getEvents(productId);
        return ApiResponse.success(events);
    }

    /**
     * 查询产品服务列表
     */
    @GetMapping("/services")
    @Operation(summary = "查询服务列表", description = "查询产品的服务定义列表")
    public ApiResponse<JsonNode> getServices(
            @Parameter(description = "产品ID") @PathVariable Long productId) {

        log.info("查询服务列表: productId={}", productId);
        JsonNode services = thingModelService.getServices(productId);
        return ApiResponse.success(services);
    }
}
