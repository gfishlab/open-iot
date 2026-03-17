package com.openiot.device.ota.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 固件上传 VO
 *
 * @author open-iot
 */
@Data
public class FirmwareUploadVO {

    /** 产品ID */
    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 固件名称 */
    @NotBlank(message = "固件名称不能为空")
    private String firmwareName;

    /** 版本号 */
    @NotBlank(message = "版本号不能为空")
    private String version;

    /** 版本说明 */
    private String description;
}
