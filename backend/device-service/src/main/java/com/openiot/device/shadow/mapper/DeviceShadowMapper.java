package com.openiot.device.shadow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.shadow.entity.DeviceShadow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 设备影子 Mapper
 * <p>
 * 提供自定义乐观锁更新方法，确保并发安全。
 * </p>
 *
 * @author open-iot
 */
@Mapper
public interface DeviceShadowMapper extends BaseMapper<DeviceShadow> {

    /**
     * 乐观锁更新 reported + delta
     *
     * @param deviceId        设备ID
     * @param reported        新的 reported JSON
     * @param delta           新的 delta JSON
     * @param expectedVersion 期望的版本号
     * @return 影响行数（0 表示版本冲突）
     */
    @Update("UPDATE device_shadow SET reported = CAST(#{reported} AS JSONB), " +
            "delta = CAST(#{delta} AS JSONB), version = version + 1, " +
            "reported_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE device_id = #{deviceId} AND version = #{expectedVersion} AND del_flag = '0'")
    int updateReportedWithVersion(@Param("deviceId") Long deviceId,
                                   @Param("reported") String reported,
                                   @Param("delta") String delta,
                                   @Param("expectedVersion") Long expectedVersion);

    /**
     * 乐观锁更新 desired + delta
     *
     * @param deviceId        设备ID
     * @param desired         新的 desired JSON
     * @param delta           新的 delta JSON
     * @param expectedVersion 期望的版本号
     * @return 影响行数（0 表示版本冲突）
     */
    @Update("UPDATE device_shadow SET desired = CAST(#{desired} AS JSONB), " +
            "delta = CAST(#{delta} AS JSONB), version = version + 1, " +
            "desired_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP " +
            "WHERE device_id = #{deviceId} AND version = #{expectedVersion} AND del_flag = '0'")
    int updateDesiredWithVersion(@Param("deviceId") Long deviceId,
                                  @Param("desired") String desired,
                                  @Param("delta") String delta,
                                  @Param("expectedVersion") Long expectedVersion);
}
