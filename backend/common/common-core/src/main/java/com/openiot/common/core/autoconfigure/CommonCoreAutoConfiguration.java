package com.openiot.common.core.autoconfigure;

import com.openiot.common.core.config.TraceConfig;
import com.openiot.common.core.exception.GlobalExceptionHandler;
import com.openiot.common.core.handler.MyBatisMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * common-core 自动配置类
 * 通过 Spring Boot SPI 机制自动注册全局异常处理器和链路追踪配置
 * 所有依赖 common-core 的服务无需额外配置即可生效
 */
@AutoConfiguration
@Import({GlobalExceptionHandler.class, TraceConfig.class})
public class CommonCoreAutoConfiguration {

    /**
     * 注册 MyBatis Plus 自动填充处理器
     */
    @Bean
    public MyBatisMetaObjectHandler myBatisMetaObjectHandler() {
        return new MyBatisMetaObjectHandler();
    }
}
