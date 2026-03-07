package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.RuleCondition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则条件 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface RuleConditionMapper extends BaseMapper<RuleCondition> {
}
