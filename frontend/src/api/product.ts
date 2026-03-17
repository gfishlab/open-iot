/**
 * 产品管理 API 接口
 */
import request from '@/utils/request'

/** 产品信息接口 */
export interface Product {
  id: number
  productKey: string
  productName: string
  productType: string
  protocolType: string
  nodeType: string
  dataFormat: string
  description?: string
  status: string
  createTime: string
  updateTime: string
}

/**
 * 分页查询产品列表
 */
export function getProductList(params: {
  pageNum: number
  pageSize: number
  productName?: string
  protocolType?: string
}) {
  return request.get('/products', { params })
}

/**
 * 获取产品详情
 */
export function getProductDetail(id: number) {
  return request.get(`/products/${id}`)
}

/**
 * 创建产品
 */
export function createProduct(data: Partial<Product>) {
  return request.post('/products', data)
}

/**
 * 更新产品
 */
export function updateProduct(id: number, data: Partial<Product>) {
  return request.put(`/products/${id}`, data)
}

/**
 * 删除产品
 */
export function deleteProduct(id: number) {
  return request.delete(`/products/${id}`)
}

/**
 * 获取全部产品列表（不分页，用于下拉选择）
 */
export function getAllProducts() {
  return request.get('/products', { params: { pageNum: 1, pageSize: 9999 } })
}
