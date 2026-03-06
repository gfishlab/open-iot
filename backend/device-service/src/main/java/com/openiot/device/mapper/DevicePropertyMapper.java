package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.DeviceProperty;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备属性 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface DevicePropertyMapper extends BaseMapper<DeviceProperty> {
}
