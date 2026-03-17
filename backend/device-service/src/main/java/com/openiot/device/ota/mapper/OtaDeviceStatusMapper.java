package com.openiot.device.ota.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.ota.entity.OtaDeviceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * OTA 设备升级状态 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface OtaDeviceStatusMapper extends BaseMapper<OtaDeviceStatus> {

    /**
     * 原子更新任务成功计数
     *
     * @param taskId 任务ID
     * @return 影响行数
     */
    @Update("UPDATE ota_upgrade_task SET success_count = success_count + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{taskId}")
    int incrementSuccessCount(@Param("taskId") Long taskId);

    /**
     * 原子更新任务失败计数
     *
     * @param taskId 任务ID
     * @return 影响行数
     */
    @Update("UPDATE ota_upgrade_task SET failed_count = failed_count + 1, update_time = CURRENT_TIMESTAMP WHERE id = #{taskId}")
    int incrementFailedCount(@Param("taskId") Long taskId);
}
