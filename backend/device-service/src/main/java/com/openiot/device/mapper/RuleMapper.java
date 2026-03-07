package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.Rule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {
}
