import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限指令
 * 用于控制元素的显示/隐藏
 *
 * 使用方式：
 * v-permission="'user:create'"        // 单个权限
 * v-permission="['user:create']"      // 数组形式
 * v-permission:any="['user:create', 'user:update']"  // 满足任意一个
 * v-permission:all="['user:create', 'user:update']"  // 满足全部
 */

/**
 * 检查用户是否拥有指定权限
 */
function checkPermission(value: string | string[], mode: 'any' | 'all' = 'any'): boolean {
  const userStore = useUserStore()
  const permissions = userStore.permissions || []

  if (!permissions.length) {
    return false
  }

  const requiredPermissions = Array.isArray(value) ? value : [value]

  if (mode === 'all') {
    return requiredPermissions.every(p => permissions.includes(p))
  }

  return requiredPermissions.some(p => permissions.includes(p))
}

/**
 * 权限指令
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const mode = binding.arg as 'any' | 'all' || 'any'
    const value = binding.value

    if (!value) {
      return
    }

    const hasPermission = checkPermission(value, mode)

    if (!hasPermission) {
      // 移除元素
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 角色指令
 * 用于基于角色控制元素显示
 *
 * 使用方式：
 * v-role="'ADMIN'"              // 单个角色
 * v-role="['ADMIN', 'TENANT_ADMIN']"  // 满足任意一个角色
 */
export const role: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const userStore = useUserStore()
    const userRole = userStore.userInfo?.role
    const requiredRoles = Array.isArray(binding.value) ? binding.value : [binding.value]

    if (!userRole || !requiredRoles.includes(userRole)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 安装权限指令
 */
export function setupPermissionDirectives(app: any) {
  app.directive('permission', permission)
  app.directive('role', role)
}

export default {
  permission,
  role,
  setupPermissionDirectives,
  checkPermission
}
