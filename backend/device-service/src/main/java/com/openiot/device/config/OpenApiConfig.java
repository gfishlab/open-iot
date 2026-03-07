package com.openiot.device.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/Knife4j API 文档配置
 *
 * @author OpenIoT Team
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI 全局配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenIoT 设备服务 API")
                        .version("1.0.0")
                        .description("""
                                ## OpenIoT 平台设备服务接口文档

                                ### 功能模块
                                - **产品管理**：产品的增删改查、设备关联、统计信息
                                - **设备管理**：设备注册、认证、状态管理
                                - **物模型管理**：属性、事件、服务定义
                                - **设备控制**：服务调用、属性设置、命令下发
                                - **告警管理**：告警规则、告警记录、告警处理
                                - **规则引擎**：设备规则配置、规则执行

                                ### 认证方式
                                所有接口需要通过 Sa-Token 认证，请求头携带：
                                - `satoken`: 用户登录凭证
                                - `X-Tenant-Id`: 租户ID（自动注入）

                                ### 错误码说明
                                - `200`：操作成功
                                - `400`：请求参数错误
                                - `401`：未认证或认证失败
                                - `403`：无权限访问
                                - `404`：资源不存在
                                - `500`：服务器内部错误
                                """)
                        .contact(new Contact()
                                .name("OpenIoT Team")
                                .email("support@openiot.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                // 通用响应示例
                .components(new io.swagger.v3.oas.models.Components()
                        .addSchemas("ApiResponse", new Schema<>()
                                .type("object")
                                .addProperty("code", new Schema<>().type("integer").example(200))
                                .addProperty("message", new Schema<>().type("string").example("操作成功"))
                                .addProperty("data", new Schema<>()
                                        .type("object")
                                        .description("响应数据"))
                                .addProperty("timestamp", new Schema<>().type("string").example("2026-03-07T12:00:00")))
                );
    }

    /**
     * 产品管理 API 分组
     */
    @Bean
    public GroupedOpenApi productApi() {
        return GroupedOpenApi.builder()
                .group("01-产品管理")
                .pathsToMatch("/api/products/**")
                .build();
    }

    /**
     * 设备管理 API 分组
     */
    @Bean
    public GroupedOpenApi deviceApi() {
        return GroupedOpenApi.builder()
                .group("02-设备管理")
                .pathsToMatch("/api/devices/**")
                .pathsToExclude("/api/devices/*/services/**", "/api/devices/*/properties/**", "/api/devices/*/commands/**")
                .build();
    }

    /**
     * 物模型管理 API 分组
     */
    @Bean
    public GroupedOpenApi thingModelApi() {
        return GroupedOpenApi.builder()
                .group("03-物模型管理")
                .pathsToMatch("/api/products/*/thing-model/**")
                .build();
    }

    /**
     * 设备控制 API 分组
     */
    @Bean
    public GroupedOpenApi deviceControlApi() {
        return GroupedOpenApi.builder()
                .group("04-设备控制")
                .pathsToMatch("/api/devices/*/services/**",
                            "/api/devices/*/properties/**",
                            "/api/devices/*/commands/**")
                .build();
    }

    /**
     * 告警管理 API 分组
     */
    @Bean
    public GroupedOpenApi alertApi() {
        return GroupedOpenApi.builder()
                .group("05-告警管理")
                .pathsToMatch("/api/alerts/**")
                .build();
    }

    /**
     * 规则引擎 API 分组
     */
    @Bean
    public GroupedOpenApi ruleApi() {
        return GroupedOpenApi.builder()
                .group("06-规则引擎")
                .pathsToMatch("/api/rules/**")
                .build();
    }
}
