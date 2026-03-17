package com.openiot.device.ota.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件版本实体
 *
 * @author open-iot
 */
@Data
@TableName("firmware_version")
public class FirmwareVersion {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联产品ID */
    private Long productId;

    /** 固件名称 */
    private String firmwareName;

    /** 版本号（semver） */
    private String version;

    /** 固件包相对存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MD5 校验和 */
    private String fileMd5;

    /** SHA256 校验和 */
    private String fileSha256;

    /** 版本说明 */
    private String description;

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
