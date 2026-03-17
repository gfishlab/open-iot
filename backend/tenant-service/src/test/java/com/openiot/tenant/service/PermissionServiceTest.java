package com.openiot.tenant.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.openiot.common.core.exception.BusinessException;
import com.openiot.tenant.entity.SysPermission;
import com.openiot.tenant.entity.SysRole;
import com.openiot.tenant.mapper.SysPermissionMapper;
import com.openiot.tenant.mapper.SysRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PermissionService 单元测试
 * 覆盖权限查询、角色校验、缓存命中、权限校验等核心逻辑
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysPermissionMapper permissionMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PermissionService permissionService;

    /**
     * 测试用的模拟角色列表
     */
    private List<SysRole> mockRoles;

    /**
     * 测试用的模拟权限列表
     */
    private List<SysPermission> mockPermissions;

    /**
     * 测试用户ID
     */
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 构建测试角色
        SysRole adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleCode("ADMIN");
        adminRole.setRoleName("平台管理员");
        adminRole.setStatus("1");
        adminRole.setDeleteFlag("0");
        adminRole.setSortOrder(1);

        SysRole tenantAdminRole = new SysRole();
        tenantAdminRole.setId(2L);
        tenantAdminRole.setRoleCode("TENANT_ADMIN");
        tenantAdminRole.setRoleName("租户管理员");
        tenantAdminRole.setStatus("1");
        tenantAdminRole.setDeleteFlag("0");
        tenantAdminRole.setSortOrder(2);

        mockRoles = List.of(adminRole, tenantAdminRole);

        // 构建测试权限
        SysPermission userListPerm = new SysPermission();
        userListPerm.setId(1L);
        userListPerm.setPermissionCode("system:user:list");
        userListPerm.setPermissionName("用户列表");
        userListPerm.setResourceType(SysPermission.TYPE_MENU);
        userListPerm.setParentId(0L);
        userListPerm.setStatus("1");
        userListPerm.setDeleteFlag("0");

        SysPermission userAddPerm = new SysPermission();
        userAddPerm.setId(2L);
        userAddPerm.setPermissionCode("system:user:add");
        userAddPerm.setPermissionName("新增用户");
        userAddPerm.setResourceType(SysPermission.TYPE_BUTTON);
        userAddPerm.setParentId(1L);
        userAddPerm.setStatus("1");
        userAddPerm.setDeleteFlag("0");

        SysPermission deviceListPerm = new SysPermission();
        deviceListPerm.setId(3L);
        deviceListPerm.setPermissionCode("device:device:list");
        deviceListPerm.setPermissionName("设备列表");
        deviceListPerm.setResourceType(SysPermission.TYPE_MENU);
        deviceListPerm.setParentId(0L);
        deviceListPerm.setStatus("1");
        deviceListPerm.setDeleteFlag("0");

        mockPermissions = List.of(userListPerm, userAddPerm, deviceListPerm);
    }

    // ==================== 查询用户角色列表 ====================

    @Nested
    @DisplayName("查询用户角色列表")
    class GetUserRolesTest {

        @Test
        @DisplayName("缓存未命中时应从数据库查询角色并写入缓存")
        void getUserRoles_cacheMiss_shouldQueryDbAndSetCache() {
            // Arrange: 缓存返回 null，模拟缓存未命中
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID)).thenReturn(null);
            when(roleMapper.selectByUserId(TEST_USER_ID)).thenReturn(mockRoles);

            // Act
            List<String> roles = permissionService.getUserRoles(TEST_USER_ID);

            // Assert: 返回正确的角色编码列表
            assertNotNull(roles);
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("TENANT_ADMIN"));

            // 验证写入缓存
            verify(valueOperations).set(
                    eq("openiot:user:roles:" + TEST_USER_ID),
                    eq(List.of("ADMIN", "TENANT_ADMIN")),
                    eq(2L),
                    eq(TimeUnit.HOURS)
            );
        }

        @Test
        @DisplayName("缓存命中时应直接返回缓存数据，不查询数据库")
        void getUserRoles_cacheHit_shouldReturnCachedData() {
            // Arrange: 缓存返回角色列表
            List<String> cachedRoles = List.of("ADMIN", "TENANT_ADMIN");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID)).thenReturn(cachedRoles);

            // Act
            List<String> roles = permissionService.getUserRoles(TEST_USER_ID);

            // Assert: 返回缓存中的角色
            assertEquals(cachedRoles, roles);

            // 验证没有查询数据库
            verify(roleMapper, never()).selectByUserId(anyLong());
            // 验证没有再次写入缓存
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("userId为null时应返回空列表")
        void getUserRoles_nullUserId_shouldReturnEmptyList() {
            // Act
            List<String> roles = permissionService.getUserRoles(null);

            // Assert
            assertNotNull(roles);
            assertTrue(roles.isEmpty());

            // 验证不与 Redis 和数据库交互
            verifyNoInteractions(redisTemplate);
            verify(roleMapper, never()).selectByUserId(anyLong());
        }

        @Test
        @DisplayName("用户没有角色时应返回空列表并缓存空结果")
        void getUserRoles_noRoles_shouldReturnEmptyAndCache() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID)).thenReturn(null);
            when(roleMapper.selectByUserId(TEST_USER_ID)).thenReturn(Collections.emptyList());

            // Act
            List<String> roles = permissionService.getUserRoles(TEST_USER_ID);

            // Assert
            assertNotNull(roles);
            assertTrue(roles.isEmpty());

            // 验证空列表也会被缓存
            verify(valueOperations).set(
                    eq("openiot:user:roles:" + TEST_USER_ID),
                    eq(Collections.emptyList()),
                    eq(2L),
                    eq(TimeUnit.HOURS)
            );
        }
    }

    // ==================== 查询用户权限列表 ====================

    @Nested
    @DisplayName("查询用户权限列表")
    class GetUserPermissionsTest {

        @Test
        @DisplayName("缓存未命中时应从数据库查询权限并写入缓存")
        void getUserPermissions_cacheMiss_shouldQueryDbAndSetCache() {
            // Arrange
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID)).thenReturn(null);
            when(permissionMapper.selectByUserId(TEST_USER_ID)).thenReturn(mockPermissions);

            // Act
            List<String> permissions = permissionService.getUserPermissions(TEST_USER_ID);

            // Assert
            assertNotNull(permissions);
            assertEquals(3, permissions.size());
            assertTrue(permissions.contains("system:user:list"));
            assertTrue(permissions.contains("system:user:add"));
            assertTrue(permissions.contains("device:device:list"));

            // 验证写入缓存
            verify(valueOperations).set(
                    eq("openiot:user:permissions:" + TEST_USER_ID),
                    eq(List.of("system:user:list", "system:user:add", "device:device:list")),
                    eq(2L),
                    eq(TimeUnit.HOURS)
            );
        }

        @Test
        @DisplayName("缓存命中时应直接返回缓存数据，不查询数据库")
        void getUserPermissions_cacheHit_shouldReturnCachedData() {
            // Arrange
            List<String> cachedPermissions = List.of("system:user:list", "system:user:add");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID)).thenReturn(cachedPermissions);

            // Act
            List<String> permissions = permissionService.getUserPermissions(TEST_USER_ID);

            // Assert
            assertEquals(cachedPermissions, permissions);
            verify(permissionMapper, never()).selectByUserId(anyLong());
        }

        @Test
        @DisplayName("userId为null时应返回空列表")
        void getUserPermissions_nullUserId_shouldReturnEmptyList() {
            // Act
            List<String> permissions = permissionService.getUserPermissions(null);

            // Assert
            assertNotNull(permissions);
            assertTrue(permissions.isEmpty());
            verifyNoInteractions(redisTemplate);
        }
    }

    // ==================== 获取用户完整权限信息 ====================

    @Test
    @DisplayName("获取用户完整权限信息应包含角色和权限")
    void getUserPermissionInfo_shouldContainRolesAndPermissions() {
        // Arrange: 模拟缓存未命中，从数据库查询
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID)).thenReturn(null);
        when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID)).thenReturn(null);
        when(roleMapper.selectByUserId(TEST_USER_ID)).thenReturn(mockRoles);
        when(permissionMapper.selectByUserId(TEST_USER_ID)).thenReturn(mockPermissions);

        // Act
        PermissionService.UserPermissionInfo info = permissionService.getUserPermissionInfo(TEST_USER_ID);

        // Assert
        assertNotNull(info);
        assertEquals(TEST_USER_ID, info.getUserId());
        assertEquals(2, info.getRoles().size());
        assertEquals(3, info.getPermissions().size());
    }

    // ==================== 缓存清除 ====================

    @Nested
    @DisplayName("缓存清除")
    class CacheClearTest {

        @Test
        @DisplayName("清除用户权限缓存应删除角色和权限两个Key")
        void clearUserPermissionCache_shouldDeleteBothKeys() {
            // Arrange
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // Act
            permissionService.clearUserPermissionCache(TEST_USER_ID);

            // Assert: 同时删除角色缓存和权限缓存
            verify(redisTemplate).delete("openiot:user:roles:" + TEST_USER_ID);
            verify(redisTemplate).delete("openiot:user:permissions:" + TEST_USER_ID);
        }

        @Test
        @DisplayName("清除角色缓存应删除角色缓存Key")
        void clearRoleCache_shouldDeleteRoleKey() {
            // Arrange
            Long roleId = 1L;
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // Act
            permissionService.clearRoleCache(roleId);

            // Assert
            verify(redisTemplate).delete("openiot:role:" + roleId);
        }
    }

    // ==================== 权限校验方法 ====================

    @Nested
    @DisplayName("权限校验方法")
    class PermissionCheckTest {

        @Test
        @DisplayName("hasRole - 用户拥有该角色应返回true")
        void hasRole_withMatchingRole_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                // Arrange
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("ADMIN", "TENANT_ADMIN"));

                // Act & Assert
                assertTrue(permissionService.hasRole("ADMIN"));
            }
        }

        @Test
        @DisplayName("hasRole - 用户没有该角色应返回false")
        void hasRole_withoutMatchingRole_shouldReturnFalse() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                // Arrange
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_USER"));

                // Act & Assert
                assertFalse(permissionService.hasRole("ADMIN"));
            }
        }

        @Test
        @DisplayName("hasRole - 未登录时应返回false")
        void hasRole_whenNotLoggedIn_shouldReturnFalse() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(false);

                assertFalse(permissionService.hasRole("ADMIN"));
            }
        }

        @Test
        @DisplayName("hasAnyRole - 拥有任一角色应返回true")
        void hasAnyRole_withOneMatchingRole_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_USER"));

                assertTrue(permissionService.hasAnyRole("ADMIN", "TENANT_USER"));
            }
        }

        @Test
        @DisplayName("hasPermission - 拥有该权限应返回true")
        void hasPermission_withMatchingPermission_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID))
                        .thenReturn(List.of("system:user:list", "system:user:add"));

                assertTrue(permissionService.hasPermission("system:user:list"));
            }
        }

        @Test
        @DisplayName("hasPermission - 未登录时应返回false")
        void hasPermission_whenNotLoggedIn_shouldReturnFalse() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(false);

                assertFalse(permissionService.hasPermission("system:user:list"));
            }
        }

        @Test
        @DisplayName("hasAnyPermission - 拥有任一权限应返回true")
        void hasAnyPermission_withOneMatching_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID))
                        .thenReturn(List.of("system:user:list"));

                assertTrue(permissionService.hasAnyPermission("system:user:list", "system:user:add"));
            }
        }

        @Test
        @DisplayName("hasAllPermissions - 拥有全部权限应返回true")
        void hasAllPermissions_withAllMatching_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID))
                        .thenReturn(List.of("system:user:list", "system:user:add", "device:device:list"));

                assertTrue(permissionService.hasAllPermissions("system:user:list", "system:user:add"));
            }
        }

        @Test
        @DisplayName("hasAllPermissions - 缺少任一权限应返回false")
        void hasAllPermissions_withMissingPermission_shouldReturnFalse() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID))
                        .thenReturn(List.of("system:user:list"));

                assertFalse(permissionService.hasAllPermissions("system:user:list", "system:user:add"));
            }
        }

        @Test
        @DisplayName("checkRole - 没有角色时应抛出403异常")
        void checkRole_withoutRole_shouldThrowForbiddenException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_USER"));

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> permissionService.checkRole("ADMIN"));

                assertEquals(403, exception.getCode());
                assertTrue(exception.getMessage().contains("ADMIN"));
            }
        }

        @Test
        @DisplayName("checkPermission - 没有权限时应抛出403异常")
        void checkPermission_withoutPermission_shouldThrowForbiddenException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:permissions:" + TEST_USER_ID))
                        .thenReturn(List.of("system:user:list"));

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> permissionService.checkPermission("system:user:delete"));

                assertEquals(403, exception.getCode());
                assertTrue(exception.getMessage().contains("system:user:delete"));
            }
        }
    }

    // ==================== 便捷方法测试 ====================

    @Nested
    @DisplayName("便捷方法")
    class ConvenienceMethodsTest {

        @Test
        @DisplayName("isPlatformAdmin - 拥有ADMIN角色应返回true")
        void isPlatformAdmin_withAdminRole_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("ADMIN"));

                assertTrue(permissionService.isPlatformAdmin());
            }
        }

        @Test
        @DisplayName("isTenantAdmin - 拥有TENANT_ADMIN角色应返回true")
        void isTenantAdmin_withTenantAdminRole_shouldReturnTrue() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_ADMIN"));

                assertTrue(permissionService.isTenantAdmin());
            }
        }

        @Test
        @DisplayName("getCurrentTenantId - 已登录应返回会话中的tenantId")
        void getCurrentTenantId_whenLoggedIn_shouldReturnTenantId() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                SaSession mockSession = mock(SaSession.class);
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getSession).thenReturn(mockSession);
                when(mockSession.get("tenantId")).thenReturn("100");

                assertEquals("100", permissionService.getCurrentTenantId());
            }
        }

        @Test
        @DisplayName("getCurrentTenantId - 未登录应返回null")
        void getCurrentTenantId_whenNotLoggedIn_shouldReturnNull() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::isLogin).thenReturn(false);

                assertNull(permissionService.getCurrentTenantId());
            }
        }
    }

    // ==================== 租户访问校验 ====================

    @Nested
    @DisplayName("租户访问校验")
    class TenantAccessTest {

        @Test
        @DisplayName("checkTenantAccess - 平台管理员可以访问任何租户数据")
        void checkTenantAccess_platformAdmin_shouldAlwaysPass() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                // Arrange: 用户是平台管理员
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("ADMIN"));

                // Act & Assert: 访问任何租户都不应抛异常
                assertDoesNotThrow(() -> permissionService.checkTenantAccess("999"));
            }
        }

        @Test
        @DisplayName("checkTenantAccess - 租户用户访问自己的租户数据应通过")
        void checkTenantAccess_sameTenant_shouldPass() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                // Arrange: 非管理员用户，当前租户为 100
                SaSession mockSession = mock(SaSession.class);
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                stpMock.when(StpUtil::getSession).thenReturn(mockSession);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_USER"));
                when(mockSession.get("tenantId")).thenReturn("100");

                // Act & Assert: 访问自己的租户不应抛异常
                assertDoesNotThrow(() -> permissionService.checkTenantAccess("100"));
            }
        }

        @Test
        @DisplayName("checkTenantAccess - 租户用户访问其他租户数据应抛出403异常")
        void checkTenantAccess_differentTenant_shouldThrowForbiddenException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                // Arrange: 非管理员用户，当前租户为 100
                SaSession mockSession = mock(SaSession.class);
                stpMock.when(StpUtil::isLogin).thenReturn(true);
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(TEST_USER_ID);
                stpMock.when(StpUtil::getSession).thenReturn(mockSession);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get("openiot:user:roles:" + TEST_USER_ID))
                        .thenReturn(List.of("TENANT_USER"));
                when(mockSession.get("tenantId")).thenReturn("100");

                // Act & Assert: 访问租户 200 应抛出异常
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> permissionService.checkTenantAccess("200"));

                assertEquals(403, exception.getCode());
            }
        }
    }

    // ==================== 角色管理方法 ====================

    @Nested
    @DisplayName("角色管理方法")
    class RoleManagementTest {

        @Test
        @DisplayName("listAllRoles - 应返回所有未删除的角色并按排序字段排序")
        void listAllRoles_shouldReturnNonDeletedRoles() {
            // Arrange
            when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockRoles);

            // Act
            List<SysRole> roles = permissionService.listAllRoles();

            // Assert
            assertNotNull(roles);
            assertEquals(2, roles.size());
            assertEquals("ADMIN", roles.get(0).getRoleCode());
            assertEquals("TENANT_ADMIN", roles.get(1).getRoleCode());
        }

        @Test
        @DisplayName("getRoleByCode - 应根据角色编码返回对应角色")
        void getRoleByCode_shouldReturnMatchingRole() {
            // Arrange
            SysRole adminRole = mockRoles.get(0);
            when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(adminRole);

            // Act
            SysRole result = permissionService.getRoleByCode("ADMIN");

            // Assert
            assertNotNull(result);
            assertEquals("ADMIN", result.getRoleCode());
            assertEquals("平台管理员", result.getRoleName());
        }

        @Test
        @DisplayName("getRoleByCode - 角色不存在时应返回null")
        void getRoleByCode_notFound_shouldReturnNull() {
            // Arrange
            when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // Act
            SysRole result = permissionService.getRoleByCode("NON_EXISTENT");

            // Assert
            assertNull(result);
        }
    }
}
