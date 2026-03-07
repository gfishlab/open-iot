package com.openiot.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.data.entity.DataForwardLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据转发日志Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface DataForwardLogMapper extends BaseMapper<DataForwardLog> {
}
