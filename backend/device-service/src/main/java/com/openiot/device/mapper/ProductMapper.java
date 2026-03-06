package com.openiot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openiot.device.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品 Mapper
 *
 * @author open-iot
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
