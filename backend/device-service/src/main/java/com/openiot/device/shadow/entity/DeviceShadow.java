package com.openiot.device.shadow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备影子实体
 * <p>
 * 存储设备的 reported（设备上报）和 desired（期望）属性，
 * 以及两者的差异 delta。使用乐观锁（version）防止并发冲突。
 * </p>
 *
 * @author open-iot
 */
@Data
@TableName("device_shadow")
public class DeviceShadow {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 设备ID */
    private Long deviceId;

    /** 设备上报属性（JSONB 字符串） */
    private String reported;

    /** 期望属性（JSONB 字符串） */
    private String desired;

    /** 差异值（desired - reported，JSONB 字符串） */
    private String delta;

    /** 乐观锁版本号 */
    private Long version;

    /** 最后 reported 更新时间 */
    private LocalDateTime reportedTime;

    /** 最后 desired 更新时间 */
    private LocalDateTime desiredTime;

    /** 元数据（JSONB 字符串） */
    private String metadata;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 删除标志：0-正常 1-删除 */
    @TableLogic
    private String delFlag;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    private Long createBy;

    /** 更新人 */
    private Long updateBy;
}
