package com.openiot.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.rule.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
