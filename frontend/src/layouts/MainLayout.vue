<template>
  <el-container class="main-layout">
    <el-aside width="200px">
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon>
          <span>设备监控</span>
        </el-menu-item>
        <el-menu-item index="/product">
          <el-icon><Box /></el-icon>
          <span>产品管理</span>
        </el-menu-item>
        <el-menu-item index="/devices">
          <el-icon><List /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/tenants" v-if="userStore.isAdmin">
          <el-icon><OfficeBuilding /></el-icon>
          <span>租户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span>Open-IoT</span>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Monitor, List, OfficeBuilding, Box, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.el-aside {
  background: #304156;
}
.el-menu {
  border-right: none;
}
.el-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}
.user-info {
  cursor: pointer;
}
</style>
