package com.openiot.device.service;

import lombok.Data;

/**
 * 产品统计 VO
 *
 * @author OpenIoT Team
 */
@Data
public class ProductStatisticsVO {
    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 设备总数
     */
    private Long totalDevices;

    /**
     * 在线设备数
     */
    private Long onlineDevices;

    /**
     * 离线设备数
     */
    private Long offlineDevices;
}
