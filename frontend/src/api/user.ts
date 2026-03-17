/**
 * 用户管理 API 接口
 */
import request from '@/utils/request'

/** 用户信息接口 */
export interface User {
  id: number
  username: string
  realName: string
  role: string
  status: string
  tenantId?: number
  createTime: string
  updateTime: string
}

/** 创建用户请求 */
export interface CreateUserRequest {
  username: string
  realName: string
  role: string
  password: string
}

/**
 * 分页查询用户列表
 */
export function getUserList(params: {
  page: number
  size: number
  username?: string
  role?: string
}) {
  return request.get('/users', { params })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id: number) {
  return request.get(`/users/${id}`)
}

/**
 * 创建用户
 */
export function createUser(data: CreateUserRequest) {
  return request.post('/users', data)
}

/**
 * 更新用户
 */
export function updateUser(id: number, data: Partial<User>) {
  return request.put(`/users/${id}`, data)
}
