package com.openiot.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户 DTO - 跨服务传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户ID */
    private Long id;

    /** 租户编码 */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 联系邮箱 */
    private String contactEmail;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
