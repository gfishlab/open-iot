<template>
  <!-- 整体布局容器：左侧边栏 + 右侧主区域 -->
  <div class="admin-layout">
    <!-- ===== 左侧导航菜单 ===== -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo 区域 -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <!-- 折叠时隐藏文字 -->
        <span v-show="!isCollapsed" class="logo-text">Open-IoT</span>
      </div>

      <!-- 导航菜单列表 -->
      <nav class="sidebar-nav">
        <template v-for="item in visibleMenuItems" :key="item.path">
          <!-- 折叠时展示 tooltip 提示菜单名称 -->
          <el-tooltip
            :content="item.title"
            placement="right"
            :disabled="!isCollapsed"
            effect="dark"
          >
            <router-link
              :to="item.path"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
            >
              <!-- 菜单图标 -->
              <span class="nav-icon" v-html="item.iconSvg"></span>
              <!-- 展开时显示菜单文字 -->
              <span v-show="!isCollapsed" class="nav-text">{{ item.title }}</span>
              <!-- 展开时显示激活指示条 -->
              <span v-show="!isCollapsed && isActive(item.path)" class="active-bar"></span>
            </router-link>
          </el-tooltip>
        </template>
      </nav>

      <!-- 侧边栏底部状态 -->
      <div v-show="!isCollapsed" class="sidebar-footer">
        <div class="system-status">
          <span class="status-dot"></span>
          <span>系统运行正常</span>
        </div>
      </div>
    </aside>

    <!-- ===== 右侧主区域 ===== -->
    <div class="main-wrapper">
      <!-- 顶部 Header -->
      <header class="header">
        <!-- 左侧：折叠按钮 + 面包屑 -->
        <div class="header-left">
          <!-- 折叠/展开侧边栏按钮 -->
          <button class="collapse-btn" @click="toggleCollapse" title="折叠/展开菜单">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <template v-if="isCollapsed">
                <!-- 展开图标：三条线向右 -->
                <line x1="3" y1="12" x2="21" y2="12"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <line x1="3" y1="18" x2="21" y2="18"/>
              </template>
              <template v-else>
                <!-- 折叠图标：三条线向左收缩 -->
                <line x1="3" y1="6" x2="21" y2="6"/>
                <line x1="3" y1="12" x2="15" y2="12"/>
                <line x1="3" y1="18" x2="21" y2="18"/>
              </template>
            </svg>
          </button>

          <!-- 面包屑导航 -->
          <el-breadcrumb class="breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/monitor' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumbs.length > 0">
              {{ breadcrumbs[0].title }}
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumbs.length > 1">
              {{ breadcrumbs[1].title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 右侧：用户信息下拉 -->
        <div class="header-right">
          <!-- 使用 Element Plus Dropdown -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-trigger">
              <!-- 用户头像（取用户名首字母） -->
              <div class="user-avatar">
                {{ avatarLetter }}
              </div>
              <div class="user-info">
                <span class="user-name">{{ username }}</span>
                <span class="user-role-badge">{{ userRoleText }}</span>
              </div>
              <!-- 下拉箭头 -->
              <svg class="dropdown-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 9l6 6 6-6"/>
              </svg>
            </div>

            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <!-- 用户基本信息展示行（不可点击） -->
                <el-dropdown-item disabled class="user-info-item">
                  <div class="dropdown-user-detail">
                    <div class="dropdown-avatar">{{ avatarLetter }}</div>
                    <div>
                      <div class="dropdown-username">{{ username }}</div>
                      <div class="dropdown-role">{{ userRoleText }}</div>
                    </div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" class="logout-item">
                  <!-- 退出登录 -->
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                  </svg>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容区：渲染子路由视图 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ===== 侧边栏折叠状态（从 localStorage 读取，实现持久化） =====
const isCollapsed = ref(localStorage.getItem('sidebar-collapsed') === 'true')

/** 切换折叠/展开状态 */
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(isCollapsed.value))
}

// ===== 菜单配置 =====
const menuItems = [
  {
    path: '/monitor',
    title: '设备监控',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="3" y="3" width="7" height="7" rx="1"/>
      <rect x="14" y="3" width="7" height="7" rx="1"/>
      <rect x="3" y="14" width="7" height="7" rx="1"/>
      <rect x="14" y="14" width="7" height="7" rx="1"/>
    </svg>`,
    requiresAdmin: false,
  },
  {
    path: '/product',
    title: '产品管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M20 7l-8-4-8 4m0 6l8 4 8-4m0-6v6l-8 4-8-4V7l8 4 8-4z"/>
    </svg>`,
    requiresAdmin: false,
  },
  {
    path: '/devices',
    title: '设备管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="4" y="4" width="16" height="16" rx="2"/>
      <circle cx="9" cy="9" r="1"/>
      <circle cx="15" cy="9" r="1"/>
      <circle cx="9" cy="15" r="1"/>
      <circle cx="15" cy="15" r="1"/>
    </svg>`,
    requiresAdmin: false,
  },
  {
    path: '/alerts',
    title: '告警管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
      <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
    </svg>`,
    requiresAdmin: false,
  },
  {
    path: '/rules',
    title: '规则引擎',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
      <rect x="9" y="3" width="6" height="4" rx="2"/>
      <path d="M9 12h6m-6 4h6"/>
    </svg>`,
    requiresAdmin: false,
  },
  {
    path: '/tenants',
    title: '租户管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16"/>
      <path d="M3 21h18"/>
      <path d="M9 7h1m4 0h1m-6 4h1m4 0h1m-6 4h1m4 0h1"/>
    </svg>`,
    requiresAdmin: true,
  },
]

/** 过滤掉管理员才能看到的菜单项 */
const visibleMenuItems = computed(() =>
  menuItems.filter(item => !item.requiresAdmin || userStore.isAdmin)
)

/** 判断菜单项是否处于激活状态（支持前缀匹配，如 /product/:id 也匹配 /product） */
function isActive(path: string): boolean {
  if (path === '/monitor') return route.path === path
  return route.path.startsWith(path)
}

// ===== 面包屑 =====
/** 路径到面包屑标题的映射 */
const pathTitleMap: Record<string, string> = {
  '/monitor': '设备监控',
  '/product': '产品管理',
  '/devices': '设备管理',
  '/alerts': '告警管理',
  '/rules': '规则引擎',
  '/tenants': '租户管理',
}

/** 动态生成面包屑（最多两级） */
const breadcrumbs = computed(() => {
  const crumbs: { title: string; path: string }[] = []
  // 查找一级路径匹配
  for (const [path, title] of Object.entries(pathTitleMap)) {
    if (route.path.startsWith(path)) {
      crumbs.push({ title, path })
      // 判断是否有详情页（二级路由）
      if (route.path !== path && route.params.id) {
        crumbs.push({ title: '详情', path: route.path })
      }
      break
    }
  }
  return crumbs
})

// ===== 用户信息 =====
/** 用户名 */
const username = computed(() => userStore.userInfo?.username || '用户')

/** 头像首字母（大写） */
const avatarLetter = computed(() =>
  username.value.charAt(0).toUpperCase()
)

/** 角色文字 */
const userRoleText = computed(() => {
  const roleMap: Record<string, string> = {
    ADMIN: '平台管理员',
    TENANT_ADMIN: '租户管理员',
    USER: '普通用户',
  }
  return roleMap[userStore.userInfo?.role] || userStore.userInfo?.role || '用户'
})

// ===== 用户下拉菜单操作 =====
async function handleUserCommand(command: string) {
  if (command === 'logout') {
    try {
      // 弹出确认框再退出
      await ElMessageBox.confirm('确认退出登录？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await userStore.logout()
      router.push('/login')
    } catch {
      // 用户取消退出，忽略
    }
  }
}
</script>

<style scoped>
/* ===== CSS 变量：侧边栏宽度 ===== */
:root {
  --sidebar-width: 240px;
  --sidebar-collapsed-width: 64px;
  --header-height: 60px;
}

/* ===== 整体布局 ===== */
.admin-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #0f172a;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 240px;
  min-width: 240px; /* 防止 flex 挤压 */
  height: 100vh;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease, min-width 0.25s ease;
  overflow: hidden; /* 折叠时隐藏溢出内容 */
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

/* 折叠状态：缩小宽度 */
.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

/* ===== Logo 区域 ===== */
.sidebar-logo {
  height: 60px; /* 与 header 等高 */
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  width: 32px;
  height: 32px;
  min-width: 32px; /* 防止压缩 */
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-icon svg {
  width: 18px;
  height: 18px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.5px;
  white-space: nowrap;
  overflow: hidden;
}

/* ===== 导航菜单 ===== */
.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 单个菜单项 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s ease, color 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

/* 折叠时菜单项居中 */
.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px 0;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #e2e8f0;
}

/* 激活状态 */
.nav-item.active {
  background: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
}

/* 菜单图标容器 */
.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  min-width: 20px;
  flex-shrink: 0;
}

.nav-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.nav-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 激活指示条（右侧竖线） */
.active-bar {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #6366f1;
  border-radius: 2px;
}

/* ===== 侧边栏底部 ===== */
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(52, 211, 153, 0.1);
  border-radius: 8px;
  font-size: 12px;
  color: #34d399;
}

.status-dot {
  width: 7px;
  height: 7px;
  min-width: 7px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.6);
  animation: blink 2s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ===== 右侧主区域 ===== */
.main-wrapper {
  flex: 1;
  min-width: 0; /* 防止内容溢出撑开 flex 容器 */
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* ===== 顶部 Header ===== */
.header {
  height: 60px;
  min-height: 60px;
  background: #1e293b;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 16px;
  flex-shrink: 0;
  z-index: 9;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

/* 折叠/展开按钮 */
.collapse-btn {
  width: 34px;
  height: 34px;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #f1f5f9;
}

.collapse-btn svg {
  width: 18px;
  height: 18px;
}

/* 面包屑覆盖 Element Plus 默认颜色 */
.breadcrumb :deep(.el-breadcrumb__inner),
.breadcrumb :deep(.el-breadcrumb__separator) {
  color: #64748b !important;
  font-size: 13px;
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #94a3b8 !important;
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner a:hover) {
  color: #a5b4fc !important;
}

/* ===== 右侧用户信息 ===== */
.header-right {
  flex-shrink: 0;
}

/* el-dropdown 触发区域 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: background 0.2s;
  outline: none;
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.08);
}

/* 用户头像圆角方块 */
.user-avatar {
  width: 32px;
  height: 32px;
  min-width: 32px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: #e2e8f0;
}

.user-role-badge {
  font-size: 11px;
  color: #64748b;
}

.dropdown-arrow {
  width: 14px;
  height: 14px;
  color: #64748b;
}

/* ===== 下拉菜单样式覆盖 ===== */
:global(.user-dropdown-menu) {
  background: #1e293b !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  padding: 4px !important;
  min-width: 200px;
}

:global(.user-dropdown-menu .el-dropdown-menu__item) {
  color: #94a3b8 !important;
  border-radius: 6px;
  margin: 2px 0;
}

:global(.user-dropdown-menu .el-dropdown-menu__item:not(.is-disabled):hover) {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #e2e8f0 !important;
}

:global(.user-dropdown-menu .el-dropdown-menu__item.is-disabled) {
  opacity: 1 !important;
  cursor: default !important;
}

/* 下拉菜单分隔线 */
:global(.user-dropdown-menu .el-dropdown-menu__item--divided) {
  border-top: 1px solid rgba(255, 255, 255, 0.08) !important;
  margin-top: 4px;
  padding-top: 6px;
}

/* 退出登录项 */
:global(.logout-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f87171 !important;
}

:global(.logout-item svg) {
  width: 15px;
  height: 15px;
}

:global(.logout-item:hover) {
  background: rgba(248, 113, 113, 0.1) !important;
  color: #fca5a5 !important;
}

/* 用户信息展示行 */
.dropdown-user-detail {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.dropdown-avatar {
  width: 36px;
  height: 36px;
  min-width: 36px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: 700;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
}

.dropdown-role {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

/* ===== 主内容区 ===== */
.main-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

/* 子路由视图撑满父容器 */
.main-content :deep(> div) {
  flex: 1;
  min-height: 0;
}
</style>
