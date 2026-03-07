package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.ServiceCallRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务调用记录 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface ServiceCallRecordMapper extends BaseMapper<ServiceCallRecord> {
}
