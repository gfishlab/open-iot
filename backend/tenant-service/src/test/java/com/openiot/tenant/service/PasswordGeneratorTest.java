package com.openiot.tenant.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码生成测试类
 * 用于生成正确的 BCrypt 密码哈希
 */
public class PasswordGeneratorTest {

    @Test
    public void generateAdminPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成 admin123 的密码哈希
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("========================================");
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("========================================");
        System.out.println();
        System.out.println("SQL to update database:");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'admin';");
        System.out.println("UPDATE sys_user SET password = '" + encodedPassword + "' WHERE username = 'tenant_admin';");
        System.out.println("========================================");

        // 验证密码匹配
        boolean matches = encoder.matches(rawPassword, encodedPassword);
        System.out.println("Password matches: " + matches);
    }
}
