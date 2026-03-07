package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
