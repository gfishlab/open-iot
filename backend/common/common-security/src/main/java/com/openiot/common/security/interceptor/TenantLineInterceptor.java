package com.openiot.common.security.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.openiot.common.security.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis Plus 租户拦截器
 * 自动为查询添加租户过滤条件
 */
@Component
public class TenantLineInterceptor implements TenantLineHandler {

    /**
     * 不需要租户隔离的表
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
            "tenant",
            "sys_user",
            "flyway_schema_history"
    ));

    /**
     * 获取租户 ID
     */
    @Override
    public Expression getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            // 平台管理员没有租户 ID，返回 Null
            return new NullValue();
        }
        return new StringValue(tenantId);
    }

    /**
     * 获取租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 判断表是否忽略租户隔离
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 平台管理员跳过租户隔离
        if (TenantContext.isPlatformAdmin()) {
            return true;
        }

        // 忽略系统表
        return IGNORE_TABLES.contains(tableName.toLowerCase());
    }
}
