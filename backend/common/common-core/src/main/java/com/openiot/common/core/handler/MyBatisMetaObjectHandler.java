package com.openiot.common.core.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 * 自动填充 createTime、updateTime、createBy、updateBy 字段
 */
@Slf4j
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

        // 填充创建人（通过反射获取 TenantContext，避免循环依赖）
        Long userId = getCurrentUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新人
        Long userId = getCurrentUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    /**
     * 通过反射获取当前用户ID
     * 避免 common-core 直接依赖 common-security 导致循环依赖
     */
    private Long getCurrentUserId() {
        try {
            Class<?> tenantContextClass = Class.forName("com.openiot.common.security.context.TenantContext");
            Object userId = tenantContextClass.getMethod("getUserId").invoke(null);
            return userId != null ? Long.valueOf(userId.toString()) : null;
        } catch (Exception e) {
            // TenantContext 不可用（如 gateway-service 不依赖 common-security）
            return null;
        }
    }
}
