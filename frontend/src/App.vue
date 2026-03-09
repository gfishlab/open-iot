<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 应用启动时，若 token 存在但 userInfo 缺失（如旧 session 或首次修复后），
// 自动从服务器拉取用户信息，保证 isAdmin 等计算属性正确
onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // 拉取失败（token 过期等），忽略——路由守卫会处理跳转登录
    }
  }
})
</script>

<style>
/* CSS Reset */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 根元素 */
:root {
  --color-bg-primary: #0f172a;
  --color-bg-secondary: #1e293b;
  --color-bg-tertiary: #334155;
  --color-text-primary: #f1f5f9;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;
  --color-border: rgba(255, 255, 255, 0.1);
  --color-accent: #6366f1;
  --color-accent-light: #a5b4fc;
  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.15);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.2);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.3);
}

/* HTML & Body */
html, body {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  background: var(--color-bg-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 链接 */
a {
  color: var(--color-accent-light);
  text-decoration: none;
  transition: color 0.2s ease;
}

a:hover {
  color: var(--color-accent);
}

/* 滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* Element Plus 主题覆盖已移至 src/styles/element-dark.css，在 main.ts 中统一管理 */
</style>
