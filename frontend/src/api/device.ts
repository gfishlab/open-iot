/**
 * 设备管理 API 接口
 */
import request from '@/utils/request'

/** 设备信息接口 */
export interface Device {
  id: number
  deviceCode: string
  deviceName: string
  productId: number
  productName?: string
  protocolType: string
  online: boolean
  status: string
  createTime: string
  updateTime: string
}

/**
 * 分页查询设备列表
 */
export function getDeviceList(params: {
  page: number
  size: number
  deviceName?: string
  online?: boolean | null
}) {
  return request.get('/devices', { params })
}

/**
 * 获取设备详情
 */
export function getDeviceDetail(id: number) {
  return request.get(`/devices/${id}`)
}

/**
 * 创建设备
 */
export function createDevice(data: Partial<Device>) {
  return request.post('/devices', data)
}

/**
 * 更新设备
 */
export function updateDevice(id: number, data: Partial<Device>) {
  return request.put(`/devices/${id}`, data)
}

/**
 * 删除设备
 */
export function deleteDevice(id: number) {
  return request.delete(`/devices/${id}`)
}
