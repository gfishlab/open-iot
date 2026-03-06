package com.openiot.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.rule.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {
}
