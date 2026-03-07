package com.openiot.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.data.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {
}
