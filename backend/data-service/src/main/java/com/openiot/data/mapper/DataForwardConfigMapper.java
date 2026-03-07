package com.openiot.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.data.entity.DataForwardConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据转发配置Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface DataForwardConfigMapper extends BaseMapper<DataForwardConfig> {
}
