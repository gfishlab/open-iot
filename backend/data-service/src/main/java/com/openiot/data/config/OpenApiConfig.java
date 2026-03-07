package com.openiot.data.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Data 服务 API 文档配置
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
                        .title("OpenIoT 数据服务 API")
                        .version("1.0.0")
                        .description("""
                                ## OpenIoT 平台数据服务接口文档

                                ### 功能模块
                                - **SSE 实时推送**：设备状态、数据变化实时通知
                                - **死信队列处理**：失败消息重放
                                - **数据重放**：历史数据回放功能
                                - **历史数据查询**：基于 InfluxDB 的时序数据查询
                                - **数据转发**：多目标数据转发（HTTP、Kafka、MQTT）
                                - **告警管理**：智能告警规则配置和告警记录管理

                                ### 认证方式
                                所有接口需要通过 Sa-Token 认证，请求头携带：
                                - `satoken`: 用户登录凭证
                                - `X-Tenant-Id`: 租户ID（自动注入）
                                """)
                        .contact(new Contact()
                                .name("OpenIoT Team")
                                .email("support@openiot.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
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
     * SSE 推送 API 分组
     */
    @Bean
    public GroupedOpenApi sseApi() {
        return GroupedOpenApi.builder()
                .group("01-SSE推送")
                .pathsToMatch("/api/v1/sse/**")
                .build();
    }

    /**
     * 死信队列 API 分组
     */
    @Bean
    public GroupedOpenApi dlqApi() {
        return GroupedOpenApi.builder()
                .group("02-死信队列")
                .pathsToMatch("/api/v1/dlq/**")
                .build();
    }

    /**
     * 数据重放 API 分组
     */
    @Bean
    public GroupedOpenApi replayApi() {
        return GroupedOpenApi.builder()
                .group("03-数据重放")
                .pathsToMatch("/api/v1/replay/**")
                .build();
    }

    /**
     * 历史数据 API 分组
     */
    @Bean
    public GroupedOpenApi historyDataApi() {
        return GroupedOpenApi.builder()
                .group("04-历史数据")
                .pathsToMatch("/api/v1/devices/*/properties/**",
                            "/api/v1/devices/*/status/**",
                            "/api/v1/devices/*/events/**",
                            "/api/v1/devices/*/influxdb/**")
                .build();
    }

    /**
     * 数据转发 API 分组
     */
    @Bean
    public GroupedOpenApi dataForwardApi() {
        return GroupedOpenApi.builder()
                .group("05-数据转发")
                .pathsToMatch("/api/v1/forward/**")
                .build();
    }

    /**
     * 告警管理 API 分组
     */
    @Bean
    public GroupedOpenApi alarmApi() {
        return GroupedOpenApi.builder()
                .group("06-告警管理")
                .pathsToMatch("/api/v1/alarms/**")
                .build();
    }
}
