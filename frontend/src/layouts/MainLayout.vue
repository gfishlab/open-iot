<template>
  <div class="admin-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar glass-sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo 区域 -->
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
          </svg>
        </div>
        <span v-show="!isCollapsed" class="logo-text">open-iot</span>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in visibleMenuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <span class="nav-icon" v-html="item.iconSvg"></span>
          <span v-show="!isCollapsed" class="nav-text">{{ item.title }}</span>
          <span v-if="isActive(item.path)" class="active-bar"></span>
        </router-link>
      </nav>

      <!-- 底部系统状态 -->
      <div class="sidebar-footer" v-show="!isCollapsed">
        <div class="system-status">
          <span class="status-dot"></span>
          <span v-show="!isCollapsed">系统运行正常</span>
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="main-wrapper">
      <!-- 顶部 Header -->
      <header class="header glass-header">
        <div class="header-left">
          <!-- 折叠/展开按钮 -->
          <button class="collapse-btn" @click="toggleCollapse">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="3" y1="12" x2="21" y2="12" />
            </svg>
          </button>
          <!-- 添加 TabsView 组件 -->
          <TabsView style="margin-left: 16px;" />
        </div>

        <!-- 右侧用户信息 -->
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-trigger">
              <div class="user-avatar">{{ avatarLetter }}</div>
              <div class="user-info">
                <span class="user-name">{{ username }}</span>
                <span class="user-role-badge">{{ userRoleText }}</span>
              </div>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
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
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容区:添加 keep-alive -->
      <div class="main-content">
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="tabsStore.cachedViews">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import TabsView from '@/components/TabsView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

// 侧边栏折叠状态
const isCollapsed = ref(false)

// 装饰:根据 localStorage 恢复折叠状态
onMounted(() => {
  const savedCollapsed = localStorage.getItem('sidebar-collapsed')
  if (savedCollapsed !== null) {
    isCollapsed.value = savedCollapsed === 'true'
  }
  // 初始化标签页
  tabsStore.initTabs()
})

// 监听路由变化,自动添加标签页
watch(
  () => route.path,
  () => {
    if (route.meta.requiresAuth !== false) {
      tabsStore.addTab(route)
    }
  },
  { immediate: true }
)

// 装饰:切换折叠/展开状态
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
    requiresAdmin: false
  },
  {
    path: '/product',
    title: '产品管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M20 7l-8-4-8 4m0 6l8 4 8-4M4 7l8 4 8-4"/>
    </svg>`,
    requiresAdmin: false
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
    requiresAdmin: false
  },
  {
    path: '/alerts',
    title: '告警管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
      <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
    </svg>`,
    requiresAdmin: false
  },
  {
    path: '/rules',
    title: '规则引擎',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
      <rect x="9" y="3" width="6" height="4" rx="2"/>
      <path d="M9 12h6m-6 4h6"/>
    </svg>`,
    requiresAdmin: false
  },
  {
    path: '/tenants',
    title: '租户管理',
    iconSvg: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16"/>
      <path d="M3 21h18"/>
      <path d="M9 7h1m4 0h1m-6 4h1m4 0h1m-6 4h1m4 0h1"/>
    </svg>`,
    requiresAdmin: true
  }
]

/** 过滤掉管理员才能看到的菜单项 */
const visibleMenuItems = computed(() =>
  menuItems.filter(item => !item.requiresAdmin || userStore.isAdmin)
)

/** 判断菜单项是否处于激活状态(支持前缀匹配,如 /product/:id 也匹配 /product) */
function isActive(path: string): boolean {
  if (path === '/monitor') return route.path === path
  return route.path.startsWith(path)
}

// ===== 用户信息 =====
/** 用户名 */
const username = computed(() => userStore.userInfo?.username || '用户')

/** 头像首字母(大写) */
const avatarLetter = computed(() =>
  username.value.charAt(0).toUpperCase()
)

/** 角色文字 */
const userRoleText = computed(() => {
  const roleMap: Record<string, string> = {
    ADMIN: '平台管理员',
    TENANT_ADMIN: '租户管理员',
    USER: '普通用户'
  }
  return roleMap[userStore.userInfo?.role] || userStore.userInfo?.role || '用户'
})

// ===== 用户下拉菜单操作 =====
async function handleUserCommand(command: string) {
  if (command === 'logout') {
    try {
      // 记住当前路由
      userStore.rememberLogoutRoute(route.path)
      // 弹出确认框再退出
      await ElMessageBox.confirm('确认退出登录？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      tabsStore.clearTabs()
      router.push('/login')
    } catch {
      // 用户取消退出,忽略
    }
  }
}
</script>

<style scoped>
/* ===== CSS 变量:侧边栏宽度 ===== */
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
  min-width: 240px;
  height: 100vh;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  transition: width 0.25s ease, min-width 0.25s ease;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

/* 折叠状态:缩小宽度 */
.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

/* ===== Logo 区域 ===== */
.sidebar-logo {
  height: 60px;
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
  min-width: 32px;
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

/* 激活指示条(右侧竖线) */
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
  min-width: 0;
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
  flex: 1;
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

/* ===== 右侧用户信息 ===== */
.header-right {
  flex-shrink: 0;
}

/* 用户触发区域 */
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
