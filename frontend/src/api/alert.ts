/**
 * 告警管理 API
 */
import request from '@/utils/request'

// 告警级别
export type AlertLevel = 'info' | 'warning' | 'critical' | 'emergency'

// 告警状态
export type AlertStatus = 'pending' | 'processing' | 'resolved' | 'ignored'

// 告警接口
export interface Alert {
  id: number
  alertId: string
  deviceId: number
  deviceName?: string
  productId?: number
  productName?: string
  alertLevel: AlertLevel
  alertStatus: AlertStatus
  alertTitle: string
  alertContent?: string
  alertTime: string
  handledBy?: string
  handleTime?: string
  handleRemark?: string
  createTime: string
  updateTime: string
}

// 告警统计
export interface AlertStatistics {
  total: number
  pending: number
  processing: number
  resolved: number
  ignored: number
  todayCount: number
  weekCount: number
}

// 告警查询参数
export interface AlertQueryParams {
  page?: number
  size?: number
  deviceId?: number
  productId?: number
  alertLevel?: AlertLevel
  alertStatus?: AlertStatus
  startTime?: string
  endTime?: string
  keyword?: string
}

// 分页响应
export interface PageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/**
 * 获取告警列表（分页）
 */
export function getAlertList(params: AlertQueryParams): Promise<PageResponse<Alert>> {
  return request.get('/alerts', { params })
}

/**
 * 获取告警详情
 */
export function getAlertDetail(id: number): Promise<Alert> {
  return request.get(`/alerts/${id}`)
}

/**
 * 处理告警
 */
export function handleAlert(id: number, data: { status: AlertStatus; remark?: string }): Promise<void> {
  return request.put(`/alerts/${id}/handle`, data)
}

/**
 * 批量处理告警
 */
export function batchHandleAlerts(data: {
  ids: number[]
  status: AlertStatus
  remark?: string
}): Promise<void> {
  return request.put('/alerts/batch-handle', data)
}

/**
 * 删除告警
 */
export function deleteAlert(id: number): Promise<void> {
  return request.delete(`/alerts/${id}`)
}

/**
 * 批量删除告警
 */
export function batchDeleteAlerts(ids: number[]): Promise<void> {
  return request.delete('/alerts/batch', { data: { ids } })
}

/**
 * 获取告警统计
 */
export function getAlertStatistics(): Promise<AlertStatistics> {
  return request.get('/alerts/statistics')
}

/**
 * 获取待处理告警列表
 */
export function getPendingAlerts(params?: { limit?: number }): Promise<Alert[]> {
  return request.get('/alerts/pending', { params })
}

/**
 * 确认告警（将告警状态从待处理改为处理中）
 */
export function confirmAlert(id: number): Promise<void> {
  return request.put(`/alerts/${id}/confirm`)
}

/**
 * 忽略告警
 */
export function ignoreAlert(id: number, remark?: string): Promise<void> {
  return request.put(`/alerts/${id}/ignore`, { remark })
}

/**
 * 获取设备告警历史
 */
export function getDeviceAlertHistory(
  deviceId: number,
  params?: { page?: number; size?: number; limit?: number }
): Promise<PageResponse<Alert>> {
  return request.get(`/devices/${deviceId}/alerts`, { params })
}

/**
 * 获取产品告警统计
 */
export function getProductAlertStatistics(productId: number): Promise<AlertStatistics> {
  return request.get(`/products/${productId}/alert-statistics`)
}
