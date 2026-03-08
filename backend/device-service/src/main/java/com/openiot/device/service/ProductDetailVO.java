package com.openiot.device.service;

import com.openiot.device.entity.Product;
import lombok.Data;

/**
 * 产品详情 VO
 *
 * @author OpenIoT Team
 */
@Data
public class ProductDetailVO {
    /**
     * 产品信息
     */
    private Product product;

    /**
     * 设备数量
     */
    private Long deviceCount;
}
