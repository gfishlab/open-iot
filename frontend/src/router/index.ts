import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/monitor'
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/DeviceMonitor.vue'),
        meta: { title: '设备监控' }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/device/DeviceList.vue'),
        meta: { title: '设备管理' }
      },
      {
        path: 'tenants',
        name: 'Tenants',
        component: () => import('@/views/tenant/TenantList.vue'),
        meta: { requiresAdmin: true, title: '租户管理' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '产品管理' }
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/ProductDetail.vue'),
        meta: { title: '产品详情' }
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/device/DeviceDetail.vue'),
        meta: { title: '设备详情' }
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alert/AlertList.vue'),
        meta: { title: '告警管理' }
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('@/views/rule/RuleList.vue'),
        meta: { title: '规则引擎' }
      },
      {
        path: 'rules/parse',
        name: 'ParseRules',
        component: () => import('@/views/rule/ParseRuleList.vue'),
        meta: { title: '解析规则' }
      },
      {
        path: 'rules/mapping',
        name: 'MappingRules',
        component: () => import('@/views/rule/MappingRuleList.vue'),
        meta: { title: '映射规则' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/tenant/UserList.vue'),
        meta: { requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'ota/firmware',
        name: 'OtaFirmware',
        component: () => import('@/views/ota/FirmwareList.vue'),
        meta: { title: 'OTA 固件' }
      },
      {
        path: 'ota/tasks',
        name: 'OtaTasks',
        component: () => import('@/views/ota/TaskList.vue'),
        meta: { title: 'OTA 升级任务' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // token 存在但 userInfo 缺失时（如页面刷新且旧 session 未缓存 userInfo），
  // 先从服务器拉取用户信息，保证 isAdmin 等计算属性正确
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      // token 无效或过期，清除后跳转登录
      localStorage.removeItem('token')
      next('/login')
      return
    }
  }

  // ===== 装饰:路由恢复逻辑 =====
  // 登录成功后,检查是否有需要恢复的路由
  if (to.path === '/login' && userStore.isLoggedIn) {
    const routeToRestore = userStore.getRouteToRestore()
    if (routeToRestore) {
      next(routeToRestore)
      return
    }
    next('/monitor')
    return
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/monitor')
  } else {
    next()
  }
})

export default router
