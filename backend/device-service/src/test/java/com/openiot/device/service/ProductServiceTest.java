package com.openiot.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.device.entity.Product;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.vo.ProductCreateVO;
import com.openiot.device.vo.ProductUpdateVO;
import com.openiot.device.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 产品服务单元测试
 *
 * @author OpenIoT Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("产品服务测试")
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTenantId(1L);
        testProduct.setProductName("智能网关");
        testProduct.setProductCode("GW001");
        testProduct.setProtocolType("MQTT");
        testProduct.setNodeType("gateway");
        testProduct.setStatus("1");
        testProduct.setProductKey("test-key-12345");
        testProduct.setProductSecret("test-secret-67890");
    }

    @Test
    @DisplayName("创建产品 - 成功")
    void createProduct_Success() {
        // Given
        ProductCreateVO vo = new ProductCreateVO();
        vo.setProductName("智能网关");
        vo.setProductCode("GW001");
        vo.setProtocolType("MQTT");
        vo.setNodeType("gateway");

        when(productMapper.insert(any(Product.class))).thenReturn(1);
        when(productMapper.selectById(any(Long.class))).thenReturn(testProduct);

        // When
        Product result = productService.createProduct(vo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("智能网关");
        assertThat(result.getProductKey()).isNotNull();
        assertThat(result.getProductSecret()).isNotNull();

        verify(productMapper, times(1)).insert(any(Product.class));
    }

    @Test
    @DisplayName("查询产品 - 成功")
    void getProductById_Success() {
        // Given
        when(productMapper.selectById(1L)).thenReturn(testProduct);

        // When
        Product result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("智能网关");

        verify(productMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("查询产品 - 不存在")
    void getProductById_NotFound() {
        // Given
        when(productMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品不存在");

        verify(productMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("更新产品 - 成功")
    void updateProduct_Success() {
        // Given
        ProductUpdateVO vo = new ProductUpdateVO();
        vo.setProductName("智能网关 Pro");
        vo.setDescription("升级版");

        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        // When
        Product result = productService.updateProduct(1L, vo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProductName()).isEqualTo("智能网关 Pro");

        verify(productMapper, times(1)).updateById(any(Product.class));
    }

    @Test
    @DisplayName("删除产品 - 成功")
    void deleteProduct_Success() {
        // Given
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.deleteById(1L)).thenReturn(1);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("分页查询产品 - 成功")
    void getProductList_Success() {
        // Given
        Page<Product> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(testProduct));
        page.setTotal(1);

        when(productMapper.selectPage(any(), any())).thenReturn(page);

        // When
        Page<Product> result = productService.getProductList(1, 10, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getProductName()).isEqualTo("智能网关");

        verify(productMapper, times(1)).selectPage(any(), any());
    }

    @Test
    @DisplayName("更新产品状态 - 成功")
    void updateStatus_Success() {
        // Given
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        // When
        productService.updateStatus(1L, "0");

        // Then
        verify(productMapper, times(1)).updateById(any(Product.class));
    }
}
