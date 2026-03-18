/**
 * OTA 固件和升级任务 API 接口
 */
import request from '@/utils/request'

// ========== 固件管理 ==========

export interface Firmware {
  id: number
  firmwareName: string
  version: string
  productId: number
  productName?: string
  fileSize: number
  signMethod: string
  filePath: string
  description?: string
  uploadTime: string
}

export interface FirmwareUploadVO {
  productId: number
  firmwareName: string
  version: string
  signMethod: string
  description?: string
}

/**
 * 分页查询固件列表
 */
export function getFirmwareList(params: {
  page: number
  size: number
  productId?: number
}) {
  return request.get('/ota/firmware', { params })
}

/**
 * 上传固件
 */
export function uploadFirmware(data: FirmwareUploadVO, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('productId', String(data.productId))
  formData.append('firmwareName', data.firmwareName)
  formData.append('version', data.version)
  formData.append('signMethod', data.signMethod)
  if (data.description) {
    formData.append('description', data.description)
  }

  return request.post('/ota/firmware', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 下载固件
 */
export function downloadFirmware(firmwareId: number) {
  return request.get(`/ota/firmware/${firmwareId}/download`, {
    responseType: 'blob'
  })
}

/**
 * 删除固件
 */
export function deleteFirmware(firmwareId: number) {
  return request.delete(`/ota/firmware/${firmwareId}`)
}

// ========== 升级任务管理 ==========

export interface OtaTask {
  id: number
  taskName: string
  productId: number
  productName?: string
  firmwareVersionId: number
  firmwareVersion?: string
  upgradeScope: 'all' | 'dynamic' | 'specific'
  deviceIds?: number[]
  deviceCount?: number
  status: 'pending' | 'running' | 'paused' | 'completed' | 'cancelled'
  strategy: 'immediate' | 'scheduled' | 'silent'
  scheduleTime?: string
  progress?: number
  description?: string
  createTime: string
}

export interface OtaTaskCreateVO {
  taskName: string
  productId: number
  firmwareVersionId: number
  upgradeScope: 'all' | 'dynamic' | 'specific'
  deviceIds?: number[]
  strategy: 'immediate' | 'scheduled' | 'silent'
  scheduleTime?: string
  description?: string
}

export interface OtaDeviceStatus {
  id: number
  taskId: number
  deviceId: number
  deviceName: string
  currentVersion: string
  targetVersion: string
  status: 'pending' | 'downloading' | 'installing' | 'completed' | 'failed'
  progress?: number
  downloadedBytes?: number
  totalBytes?: number
  errorMsg?: string
  startTime?: string
  completeTime?: string
}

/**
 * 分页查询升级任务列表
 */
export function getTaskList(params: {
  page: number
  size: number
  status?: string
}) {
  return request.get('/ota/tasks', { params })
}

/**
 * 创建升级任务
 */
export function createTask(data: OtaTaskCreateVO) {
  return request.post('/ota/tasks', data)
}

/**
 * 开始任务
 */
export function startTask(taskId: number) {
  return request.put(`/ota/tasks/${taskId}/start`)
}

/**
 * 暂停任务
 */
export function pauseTask(taskId: number) {
  return request.put(`/ota/tasks/${taskId}/pause`)
}

/**
 * 取消任务
 */
export function cancelTask(taskId: number) {
  return request.put(`/ota/tasks/${taskId}/cancel`)
}

/**
 * 查询任务设备状态
 */
export function getTaskDeviceStatuses(taskId: number, params: {
  page: number
  size: number
}) {
  return request.get(`/ota/tasks/${taskId}/devices`, { params })
}

// ========== 设备影子 ==========

export interface DeviceShadow {
  version: number
  timestamp?: string
  reported: Record<string, any>
  desired: Record<string, any>
  delta: Record<string, any>
}

export interface DesiredUpdateVO {
  version: number
  desired: Record<string, any>
}

/**
 * 获取设备影子
 */
export function getDeviceShadow(deviceId: number) {
  return request.get(`/devices/${deviceId}/shadow`)
}

/**
 * 更新期望属性
 */
export function updateDesired(deviceId: number, data: DesiredUpdateVO) {
  return request.put(`/devices/${deviceId}/shadow/desired`, data)
}

// ========== 设备轨迹 ==========

export interface TrajectoryPoint {
  id: number
  deviceId: number
  latitude: number
  longitude: number
  speed?: number
  heading?: number
  eventTime: string
}

export interface TrajectoryStatistics {
  deviceId: number
  startTime: string
  endTime: string
  pointCount: number
  totalDistance: number // 米
  avgSpeed: number
  maxSpeed: number
}

/**
 * 查询设备轨迹
 */
export function queryTrajectory(deviceId: number, params: {
  startTime: string
  endTime: string
}) {
  return request.get(`/devices/${deviceId}/trajectory/query`, { params })
}

/**
 * 分页查询设备轨迹
 */
export function queryTrajectoryPage(deviceId: number, params: {
  startTime: string
  endTime: string
  pageNum: number
  pageSize: number
}) {
  return request.get(`/devices/${deviceId}/trajectory/page`, { params })
}

/**
 * 获取最新轨迹点
 */
export function getLatestTrajectory(deviceId: number) {
  return request.get(`/devices/${deviceId}/trajectory/latest`)
}

/**
 * 计算轨迹距离
 */
export function calculateTrajectoryDistance(deviceId: number, params: {
  startTime: string
  endTime: string
}) {
  return request.get(`/devices/${deviceId}/trajectory/distance`, { params })
}

/**
 * 获取轨迹统计
 */
export function getTrajectoryStatistics(deviceId: number, params: {
  startTime: string
  endTime: string
}) {
  return request.get(`/devices/${deviceId}/trajectory/statistics`, { params })
}
