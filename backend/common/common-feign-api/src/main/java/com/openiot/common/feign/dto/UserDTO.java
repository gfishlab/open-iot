package com.openiot.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户 DTO - 跨服务传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色：ADMIN/TENANT_ADMIN */
    private String role;

    /** 状态：1-启用 0-禁用 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
