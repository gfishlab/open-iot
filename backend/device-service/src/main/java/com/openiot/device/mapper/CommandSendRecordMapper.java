package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.CommandSendRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 命令下发记录 Mapper
 *
 * @author OpenIoT Team
 */
@Mapper
public interface CommandSendRecordMapper extends BaseMapper<CommandSendRecord> {
}
