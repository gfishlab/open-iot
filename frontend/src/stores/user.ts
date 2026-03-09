import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  // 从 localStorage 恢复用户信息，避免刷新后 isAdmin 等计算属性失效
  const userInfo = ref<any>(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('permissions') || '[]'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isTenantAdmin = computed(() => userInfo.value?.role === 'TENANT_ADMIN')

  async function login(username: string, password: string) {
    const data = await request.post('/auth/login', { username, password })
    token.value = data.token
    userInfo.value = data
    permissions.value = data.permissions || []
    // 持久化到 localStorage，避免刷新后丢失
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data))
    localStorage.setItem('permissions', JSON.stringify(data.permissions || []))
    return data
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch {
      // 忽略登出接口错误
    } finally {
      token.value = ''
      userInfo.value = null
      permissions.value = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('permissions')
    }
  }

  async function fetchUserInfo() {
    const data = await request.get('/users/me')
    userInfo.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
    // 如果登录响应中没有权限列表，需要单独获取
    if (!permissions.value.length && data.role) {
      await fetchPermissions()
    }
    return data
  }

  async function fetchPermissions() {
    // 权限已在登录时存储在 Session 中，这里可以从用户信息获取
    // 或者调用专门的权限接口
    permissions.value = userInfo.value?.permissions || []
  }

  /**
   * 检查是否拥有指定权限
   */
  function hasPermission(permission: string | string[]): boolean {
    const required = Array.isArray(permission) ? permission : [permission]
    return required.some(p => permissions.value.includes(p))
  }

  /**
   * 检查是否拥有全部指定权限
   */
  function hasAllPermissions(perms: string[]): boolean {
    return perms.every(p => permissions.value.includes(p))
  }

  /**
   * 检查是否拥有指定角色
   */
  function hasRole(role: string | string[]): boolean {
    const required = Array.isArray(role) ? role : [role]
    return required.some(r => userInfo.value?.role === r)
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    isAdmin,
    isTenantAdmin,
    login,
    logout,
    fetchUserInfo,
    fetchPermissions,
    hasPermission,
    hasAllPermissions,
    hasRole
  }
})
