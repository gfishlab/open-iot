package com.openiot.device.ota.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.ota.entity.OtaUpgradeTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * OTA 升级任务 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface OtaUpgradeTaskMapper extends BaseMapper<OtaUpgradeTask> {
}
