package com.openiot.tenant.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.tenant.entity.SysUser;
import com.openiot.tenant.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * 覆盖登录、登出、获取当前用户等核心认证逻辑
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private AuthService authService;

    /**
     * 用于生成 BCrypt 密码哈希的编码器（测试辅助）
     */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 测试用的模拟用户
     */
    private SysUser mockUser;

    /**
     * 原始密码明文
     */
    private static final String RAW_PASSWORD = "admin123";

    @BeforeEach
    void setUp() {
        // 构建一个标准的启用状态用户
        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        mockUser.setTenantId(100L);
        mockUser.setStatus("1");
        mockUser.setRole("ADMIN");
        mockUser.setDeleteFlag("0");
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功 - 正确的用户名和密码应返回 Token")
    void login_withValidCredentials_shouldReturnToken() {
        // Arrange: 模拟 Mapper 查询返回用户
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        // 模拟权限服务返回角色和权限
        when(permissionService.getUserRoles(1L)).thenReturn(List.of("ADMIN"));
        when(permissionService.getUserPermissions(1L)).thenReturn(List.of("system:user:list"));

        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            // 模拟 Sa-Token 登录行为
            SaSession mockSession = mock(SaSession.class);
            stpMock.when(() -> StpUtil.login(anyLong())).thenAnswer(invocation -> null);
            stpMock.when(StpUtil::getSession).thenReturn(mockSession);
            stpMock.when(StpUtil::getTokenValue).thenReturn("mock-token-abc123");

            // Act
            String token = authService.login("admin", RAW_PASSWORD);

            // Assert: 返回的 Token 不为空
            assertNotNull(token);
            assertEquals("mock-token-abc123", token);

            // 验证 Sa-Token 登录被调用
            stpMock.verify(() -> StpUtil.login(1L));

            // 验证会话信息被正确设置
            verify(mockSession).set("tenantId", "100");
            verify(mockSession).set("userId", "1");
            verify(mockSession).set("username", "admin");
            verify(mockSession).set("roles", List.of("ADMIN"));
            verify(mockSession).set("role", "ADMIN");
        }
    }

    @Test
    @DisplayName("登录成功 - 租户ID为null时不设置tenantId到会话")
    void login_withNullTenantId_shouldNotSetTenantIdInSession() {
        // Arrange: 平台管理员，tenantId 为 null
        mockUser.setTenantId(null);
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(permissionService.getUserRoles(1L)).thenReturn(List.of("ADMIN"));
        when(permissionService.getUserPermissions(1L)).thenReturn(List.of("system:user:list"));

        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            SaSession mockSession = mock(SaSession.class);
            stpMock.when(() -> StpUtil.login(anyLong())).thenAnswer(invocation -> null);
            stpMock.when(StpUtil::getSession).thenReturn(mockSession);
            stpMock.when(StpUtil::getTokenValue).thenReturn("mock-token");

            // Act
            authService.login("admin", RAW_PASSWORD);

            // Assert: tenantId 不应该被设置
            verify(mockSession, never()).set(eq("tenantId"), anyString());
            // 但 userId 和 username 仍然应该被设置
            verify(mockSession).set("userId", "1");
            verify(mockSession).set("username", "admin");
        }
    }

    @Test
    @DisplayName("登录失败 - 用户不存在应抛出异常")
    void login_withNonExistentUser_shouldThrowException() {
        // Arrange: Mapper 返回 null，表示用户不存在
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login("nonexistent", "password"));

        assertEquals(401, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 密码错误应抛出异常")
    void login_withWrongPassword_shouldThrowException() {
        // Arrange: 用户存在但密码不匹配
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login("admin", "wrongpassword"));

        assertEquals(401, exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录失败 - 用户被禁用应抛出异常")
    void login_withDisabledUser_shouldThrowException() {
        // Arrange: 用户状态为 "0"（禁用）
        mockUser.setStatus("0");
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login("admin", RAW_PASSWORD));

        assertEquals(401, exception.getCode());
        assertEquals("账户已禁用", exception.getMessage());
    }

    // ==================== 密码校验测试 ====================

    @Test
    @DisplayName("密码校验 - BCrypt 编码后的密码应能通过验证")
    void passwordValidation_bcryptEncodedPassword_shouldMatch() {
        // Arrange: 使用 BCrypt 编码密码
        String encoded = passwordEncoder.encode("test123");

        // Act & Assert: 正确密码应匹配
        assertTrue(passwordEncoder.matches("test123", encoded));
        // 错误密码不应匹配
        assertFalse(passwordEncoder.matches("wrong", encoded));
    }

    @Test
    @DisplayName("密码校验 - 同一密码每次编码结果不同（BCrypt 随机盐）")
    void passwordValidation_bcryptShouldGenerateDifferentHashes() {
        // Act: 对同一密码编码两次
        String encoded1 = passwordEncoder.encode("samePassword");
        String encoded2 = passwordEncoder.encode("samePassword");

        // Assert: 两次编码结果不同（因为随机盐），但都能匹配原始密码
        assertNotEquals(encoded1, encoded2);
        assertTrue(passwordEncoder.matches("samePassword", encoded1));
        assertTrue(passwordEncoder.matches("samePassword", encoded2));
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出 - 已登录用户应清除缓存并调用StpUtil.logout")
    void logout_whenLoggedIn_shouldClearCacheAndLogout() {
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            // Arrange: 用户已登录
            SaSession mockSession = mock(SaSession.class);
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getSession).thenReturn(mockSession);
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(mockSession.get("username")).thenReturn("admin");

            // Act
            authService.logout();

            // Assert: 清除权限缓存
            verify(permissionService).clearUserPermissionCache(1L);
            // 调用 Sa-Token 登出
            stpMock.verify(StpUtil::logout);
        }
    }

    @Test
    @DisplayName("登出 - 未登录用户应直接返回不做任何操作")
    void logout_whenNotLoggedIn_shouldDoNothing() {
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            // Arrange: 用户未登录
            stpMock.when(StpUtil::isLogin).thenReturn(false);

            // Act
            authService.logout();

            // Assert: 不应调用 logout 和 clearCache
            stpMock.verify(StpUtil::logout, never());
            verify(permissionService, never()).clearUserPermissionCache(anyLong());
        }
    }

    // ==================== 获取当前用户测试 ====================

    @Test
    @DisplayName("获取当前用户 - 已登录时应返回正确的用户信息")
    void getCurrentUser_whenLoggedIn_shouldReturnUserInfo() {
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            // Arrange
            SaSession mockSession = mock(SaSession.class);
            stpMock.when(StpUtil::isLogin).thenReturn(true);
            stpMock.when(StpUtil::getLoginId).thenReturn(1L);
            stpMock.when(StpUtil::getSession).thenReturn(mockSession);
            when(mockSession.get("tenantId")).thenReturn("100");
            when(mockSession.get("role")).thenReturn("ADMIN");
            when(mockSession.get("username")).thenReturn("admin");

            // Act
            var userInfo = authService.getCurrentUser();

            // Assert
            assertNotNull(userInfo);
            assertEquals("1", userInfo.getUserId());
            assertEquals("100", userInfo.getTenantId());
            assertEquals("ADMIN", userInfo.getRole());
            assertEquals("admin", userInfo.getUsername());
        }
    }

    @Test
    @DisplayName("获取当前用户 - 未登录时应返回null")
    void getCurrentUser_whenNotLoggedIn_shouldReturnNull() {
        try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
            // Arrange
            stpMock.when(StpUtil::isLogin).thenReturn(false);

            // Act
            var userInfo = authService.getCurrentUser();

            // Assert
            assertNull(userInfo);
        }
    }
}
