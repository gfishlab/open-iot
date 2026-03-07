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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

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
