package com.openiot.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.data.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
