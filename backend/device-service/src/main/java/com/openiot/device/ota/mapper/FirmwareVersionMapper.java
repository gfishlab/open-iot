package com.openiot.device.ota.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.ota.entity.FirmwareVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 固件版本 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface FirmwareVersionMapper extends BaseMapper<FirmwareVersion> {
}
