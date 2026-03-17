package com.openiot.device.ota.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件版本 VO（列表展示用）
 *
 * @author open-iot
 */
@Data
public class FirmwareVersionVO {

    /** 固件版本ID */
    private Long id;

    /** 产品ID */
    private Long productId;

    /** 固件名称 */
    private String firmwareName;

    /** 版本号 */
    private String version;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MD5 校验和 */
    private String fileMd5;

    /** 版本说明 */
    private String description;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
