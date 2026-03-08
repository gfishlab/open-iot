<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="logo-text">Open-IoT</span>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/monitor" class="nav-item" :class="{ active: route.path === '/monitor' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1"/>
            <rect x="14" y="3" width="7" height="7" rx="1"/>
            <rect x="3" y="14" width="7" height="7" rx="1"/>
            <rect x="14" y="14" width="7" height="7" rx="1"/>
          </svg>
          <span>设备监控</span>
        </router-link>

        <router-link to="/product" class="nav-item" :class="{ active: route.path.startsWith('/product') }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 7l-8-4-8 4m0 6l8 4 8-4m0-6v6l-8 4-8-4V7l8 4 8-4z"/>
          </svg>
          <span>产品管理</span>
        </router-link>

        <router-link to="/devices" class="nav-item" :class="{ active: route.path === '/devices' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="4" y="4" width="16" height="16" rx="2"/>
            <circle cx="9" cy="9" r="1"/>
            <circle cx="15" cy="9" r="1"/>
            <circle cx="9" cy="15" r="1"/>
            <circle cx="15" cy="15" r="1"/>
          </svg>
          <span>设备管理</span>
        </router-link>

        <router-link to="/alerts" class="nav-item" :class="{ active: route.path === '/alerts' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
            <circle cx="12" cy="12" r="10"/>
          </svg>
          <span>告警管理</span>
        </router-link>

        <router-link to="/rules" class="nav-item" :class="{ active: route.path === '/rules' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
            <path d="M9 12h6m-6 4h6"/>
          </svg>
          <span>规则引擎</span>
        </router-link>

        <router-link v-if="userStore.isAdmin" to="/tenants" class="nav-item" :class="{ active: route.path === '/tenants' }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16"/>
            <path d="M3 21h18"/>
            <path d="M9 7h1m4 0h1m-6 4h1m4 0h1m-6 4h1m4 0h1"/>
          </svg>
          <span>租户管理</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="system-status">
          <div class="status-indicator online"></div>
          <span>系统运行正常</span>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="main-area">
      <!-- 顶部栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <h1 class="page-title">{{ getPageTitle() }}</h1>
        </div>
        <div class="topbar-right">
          <div class="user-dropdown" @click="toggleDropdown">
            <div class="user-avatar">
              {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
            </div>
            <span class="user-name">{{ userStore.userInfo?.username || '用户' }}</span>
            <svg :class="['dropdown-arrow', { open: showDropdown }]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </div>
          <Transition name="dropdown">
            <div v-if="showDropdown" class="dropdown-menu">
              <div class="dropdown-item user-info-item">
                <span class="user-role">{{ userStore.userInfo?.role || '用户' }}</span>
              </div>
              <div class="dropdown-divider"></div>
              <button class="dropdown-item logout-btn" @click="handleLogout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                </svg>
                退出登录
              </button>
            </div>
          </Transition>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

import { onClickOutside } from '@vueuse/core'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement>()

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

function getPageTitle() {
  const pathMap: Record<string, string> = {
    '/monitor': '设备监控',
    '/product': '产品管理',
    '/devices': '设备管理',
    '/alerts': '告警管理',
    '/rules': '规则引擎',
    '/tenants': '租户管理'
  }
  return pathMap[route.path] || '控制台'
}

// 点击外部关闭下拉菜单
onClickOutside(dropdownRef, () => {
  showDropdown.value = false
})
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: #0f172a;
}

/* 侧边栏 */
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo svg {
  width: 24px;
  height: 24px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.5px;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  color: #94a3b8;
  text-decoration: none;
  border-radius: 12px;
  transition: all 0.2s ease;
  font-size: 15px;
  font-weight: 500;
}

.nav-item svg {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #a5b4fc;
}

.nav-item.active svg {
  color: #a5b4fc;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(52, 211, 153, 0.1);
  border-radius: 10px;
  font-size: 13px;
  color: #34d399;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34d399;
  animation: pulse 2s infinite;
}

.status-indicator.online {
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.5);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 主区域 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 顶部栏 */
.topbar {
  height: 72px;
  background: linear-gradient(135deg, #1e293b 0%, #1a1f2e 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
  margin: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.user-dropdown:hover {
  background: rgba(255, 255, 255, 0.1);
}

.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
  color: #f1f5f9;
}

.dropdown-arrow {
  width: 20px;
  height: 20px;
  color: #6b7280;
  transition: transform 0.2s ease;
}

.dropdown-arrow.open {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 200px;
  background: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  z-index: 100;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  color: #94a3b8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  background: transparent;
  border: none;
  width: 100%;
  text-align: left;
}

.dropdown-item svg {
  width: 18px;
  height: 18px;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
}

.user-info-item {
  cursor: default;
}

.user-role {
  font-size: 13px;
  color: #6b7280;
}

.dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 8px 0;
}

.logout-btn {
  color: #f87171;
}

.logout-btn:hover {
  background: rgba(248, 113, 113, 0.1);
  color: #fca5a5;
}

/* 内容区域 */
.content-area {
  flex: 1;
  overflow: auto;
  padding: 0;
}

/* 过渡动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 响应式 */
@media (max-width: 1200px) {
  .sidebar {
    width: 220px;
  }

  .topbar {
    padding: 0 24px;
  }
}

@media (max-width: 900px) {
  .layout-container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .sidebar-nav {
    flex-direction: row;
    overflow-x: auto;
  }

  .nav-item span {
    display: none;
  }

  .sidebar-footer {
    display: none;
  }
}
</style>
