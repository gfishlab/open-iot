/**
 * 规则引擎 API 接口
 */
import request from '@/utils/request'

export interface Rule {
  id: number
  ruleName: string
  ruleCode: string
  ruleType: string
  expression: string
  scriptContent?: string
  productId?: number
  productName?: string
  deviceId?: number
  deviceName?: string
  alertLevel: string
  alertTemplate: string
  status: string
  description?: string
  createTime: string
  updateTime: string
}

export interface RuleTestRequest {
  ruleId?: number
  ruleType: string
  expression?: string
  scriptContent?: string
  deviceData: Record<string, any>
}

export interface RuleTestResponse {
  matched: boolean
  result?: Record<string, any>
  error?: string
  executionTime: number
}

/**
 * 分页查询规则列表
 */
export function getRuleList(params: {
  page: number
  size: number
  ruleName?: string
  ruleType?: string
  status?: string
}) {
  return request.get('/rules', { params })
}

/**
 * 获取规则详情
 */
export function getRuleDetail(id: number) {
  return request.get(`/rules/${id}`)
}

/**
 * 创建规则
 */
export function createRule(data: Partial<Rule>) {
  return request.post('/rules', data)
}

/**
 * 更新规则
 */
export function updateRule(id: number, data: Partial<Rule>) {
  return request.put(`/rules/${id}`, data)
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return request.delete(`/rules/${id}`)
}

/**
 * 启用规则
 */
export function enableRule(id: number) {
  return request.put(`/rules/${id}/enable`)
}

/**
 * 禁用规则
 */
export function disableRule(id: number) {
  return request.put(`/rules/${id}/disable`)
}

/**
 * 测试规则
 */
export function testRule(data: RuleTestRequest) {
  return request.post('/rules/test', data)
}

/**
 * 批量删除规则
 */
export function batchDeleteRules(ids: number[]) {
  return request.delete('/rules/batch', { data: { ids } })
}

/**
 * 获取规则执行日志
 */
export function getRuleLogs(params: {
  page: number
  size: number
  ruleId?: number
  startTime?: string
  endTime?: string
}) {
  return request.get('/rules/logs', { params })
}
