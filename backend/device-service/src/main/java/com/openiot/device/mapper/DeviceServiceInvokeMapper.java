package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.DeviceServiceInvoke;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备服务调用 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface DeviceServiceInvokeMapper extends BaseMapper<DeviceServiceInvoke> {
}
