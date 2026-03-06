package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.DeviceEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备事件 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface DeviceEventMapper extends BaseMapper<DeviceEvent> {
}
