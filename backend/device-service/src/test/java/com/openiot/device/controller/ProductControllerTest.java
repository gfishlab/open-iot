package com.openiot.device.controller;

import com.openiot.device.entity.Product;
import com.openiot.device.mapper.ProductMapper;
import com.openiot.device.vo.ProductCreateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 产品控制器 API 集成测试
 *
 * @author OpenIoT Team
 */
@DisplayName("产品 API 集成测试")
public class ProductControllerTest extends BaseControllerTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("创建产品 - 成功")
    void createProduct_Success() throws Exception {
        // Given
        ProductCreateVO vo = new ProductCreateVO();
        vo.setProductName("智能网关");
        vo.setProductCode("GW001");
        vo.setProtocolType("MQTT");
        vo.setNodeType("gateway");
        vo.setDescription("测试产品");

        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(vo)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("智能网关"))
                .andExpect(jsonPath("$.data.productKey").isNotEmpty())
                .andExpect(jsonPath("$.data.productSecret").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询产品列表 - 成功")
    void getProducts_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products")
                .param("pageNum", "1")
                .param("pageSize", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("查询产品详情 - 成功")
    void getProductDetail_Success() throws Exception {
        // Given - 先创建一个产品
        Product product = new Product();
        product.setTenantId(1L);
        product.setProductName("测试产品");
        product.setProductCode("TEST001");
        product.setProtocolType("MQTT");
        product.setNodeType("device");
        product.setProductKey("test-key");
        product.setProductSecret("test-secret");
        product.setStatus("1");
        productMapper.insert(product);

        // When & Then
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("测试产品"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("更新产品 - 成功")
    void updateProduct_Success() throws Exception {
        // Given
        Product product = new Product();
        product.setTenantId(1L);
        product.setProductName("原始名称");
        product.setProductCode("TEST002");
        product.setProtocolType("MQTT");
        product.setNodeType("device");
        product.setProductKey("test-key-2");
        product.setProductSecret("test-secret-2");
        product.setStatus("1");
        productMapper.insert(product);

        String updateJson = """
            {
                "productName": "更新后的名称",
                "description": "更新描述"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/products/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("更新后的名称"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("删除产品 - 成功")
    void deleteProduct_Success() throws Exception {
        // Given
        Product product = new Product();
        product.setTenantId(1L);
        product.setProductName("待删除产品");
        product.setProductCode("DEL001");
        product.setProtocolType("MQTT");
        product.setNodeType("device");
        product.setProductKey("del-key");
        product.setProductSecret("del-secret");
        product.setStatus("1");
        productMapper.insert(product);

        // When & Then
        mockMvc.perform(delete("/api/products/" + product.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("未认证访问 - 失败")
    void getProducts_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
