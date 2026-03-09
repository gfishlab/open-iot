import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { requiresAuth: false }
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
        component: () => import('@/views/monitor/DeviceMonitor.vue')
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/device/DeviceList.vue')
      },
      {
        path: 'tenants',
        name: 'Tenants',
        component: () => import('@/views/tenant/TenantList.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue')
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/ProductDetail.vue')
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/device/DeviceList.vue')
      },
      {
        path: 'devices/:id',
        name: 'DeviceDetail',
        component: () => import('@/views/device/DeviceDetail.vue')
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/alert/AlertList.vue')
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('@/views/rule/RuleList.vue')
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

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/monitor')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/monitor')
  } else {
    next()
  }
})

export default router
