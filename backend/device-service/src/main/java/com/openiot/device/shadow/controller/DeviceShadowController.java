package com.openiot.device.shadow.controller;

import com.openiot.common.core.result.ApiResponse;
import com.openiot.device.shadow.service.DeviceShadowService;
import com.openiot.device.shadow.vo.DesiredUpdateVO;
import com.openiot.device.shadow.vo.DeviceShadowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 设备影子控制器
 * <p>
 * 提供设备影子的查询和期望属性更新接口。
 * 设备影子用于存储设备的 reported（上报）和 desired（期望）属性，
 * 支持设备离线时的属性下发和状态同步。
 * </p>
 *
 * @author open-iot
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "设备影子", description = "设备影子查询与期望属性更新接口")
public class DeviceShadowController {

    private final DeviceShadowService deviceShadowService;

    /**
     * 获取设备影子
     * <p>
     * 返回设备的 reported、desired、delta 及版本号等信息。
     * 优先从 Redis 缓存读取，缓存未命中时回退到数据库查询。
     * </p>
     *
     * @param deviceId 设备ID
     * @return 设备影子 VO
     */
    @GetMapping("/{deviceId}/shadow")
    @Operation(summary = "获取设备影子", description = "查询设备的 reported、desired、delta 属性及版本号")
    public ApiResponse<DeviceShadowVO> getShadow(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId) {

        log.info("获取设备影子: deviceId={}", deviceId);

        DeviceShadowVO shadow = deviceShadowService.getShadow(deviceId);
        return ApiResponse.success(shadow);
    }

    /**
     * 更新期望属性（desired）
     * <p>
     * 使用乐观锁防止并发冲突，客户端需要传入当前版本号。
     * 如果版本冲突（其他请求已修改），返回 409 Conflict。
     * 更新成功后，若 delta 非空，会自动推送 SHADOW_DELTA 事件到 Kafka。
     * </p>
     *
     * @param deviceId 设备ID
     * @param vo       期望属性更新请求（包含 desired JSON 和版本号）
     * @return 更新后的设备影子 VO
     */
    @PutMapping("/{deviceId}/shadow/desired")
    @Operation(summary = "更新期望属性", description = "更新设备影子的 desired 属性，使用乐观锁防止并发冲突")
    public ApiResponse<DeviceShadowVO> updateDesired(
            @Parameter(description = "设备ID") @PathVariable(name = "deviceId") Long deviceId,
            @Valid @RequestBody DesiredUpdateVO vo) {

        log.info("更新设备期望属性: deviceId={}, version={}", deviceId, vo.getVersion());

        DeviceShadowVO shadow = deviceShadowService.updateDesired(deviceId, vo);
        return ApiResponse.success("期望属性更新成功", shadow);
    }
}
