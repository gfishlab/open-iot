/**
 * 租户管理 API 接口
 */
import request from '@/utils/request'

/** 租户信息接口 */
export interface Tenant {
  id: number
  tenantCode: string
  tenantName: string
  contactEmail: string
  status: string
  createTime: string
  updateTime: string
}

/** 创建租户请求（包含管理员账号） */
export interface CreateTenantRequest {
  tenantName: string
  contactEmail: string
  adminUsername: string
  adminPassword: string
}

/**
 * 分页查询租户列表
 */
export function getTenantList(params: {
  page: number
  size: number
  tenantName?: string
}) {
  return request.get('/tenants', { params })
}

/**
 * 获取租户详情
 */
export function getTenantDetail(id: number) {
  return request.get(`/tenants/${id}`)
}

/**
 * 创建租户
 */
export function createTenant(data: CreateTenantRequest) {
  return request.post('/tenants', data)
}

/**
 * 更新租户
 */
export function updateTenant(id: number, data: Partial<Tenant>) {
  return request.put(`/tenants/${id}`, data)
}

/**
 * 禁用租户
 */
export function disableTenant(id: number) {
  return request.put(`/tenants/${id}/disable`)
}

/**
 * 启用租户
 */
export function enableTenant(id: number) {
  return request.put(`/tenants/${id}/enable`)
}

/**
 * 删除租户
 */
export function deleteTenant(id: number) {
  return request.delete(`/tenants/${id}`)
}
