<template>
  <div class="product-detail-page">
    <el-page-header @back="goBack" :title="product.productName || '产品详情'">
      <template #content>
        <span class="product-title">{{ product.productName || '加载中...' }}</span>
        <el-tag class="glass-tag" :type="product.status === '1' ? 'success' : 'danger'" size="small">
          {{ product.status === '1' ? '启用' : '禁用' }}
        </el-tag>
      </template>
    </el-page-header>

    <el-divider />

    <el-skeleton :loading="loading" animated>
      <template #default>
        <!-- 基本信息 -->
        <el-card class="glass-card" style="margin-bottom: 24px">
          <template #header>
            <div class="card-header">
              <span class="card-title">基本信息</span>
              <el-button class="glass-button" type="primary" size="small" @click="handleEdit">编辑</el-button>
            </div>
          </template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="产品Key">{{ product.productKey }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ product.productName }}</el-descriptions-item>
            <el-descriptions-item label="产品类型">
              <el-tag class="glass-tag">{{ product.productType === 'DEVICE' ? '设备' : '网关' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="协议类型">{{ product.protocolType }}</el-descriptions-item>
            <el-descriptions-item label="节点类型">{{ product.nodeType === 'DIRECT' ? '直连' : '网关' }}</el-descriptions-item>
            <el-descriptions-item label="数据格式">{{ product.dataFormat }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ product.createTime }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ product.updateTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 物模型 -->
        <el-card class="glass-card" style="margin-bottom: 24px">
          <template #header>
            <div class="card-header">
              <span class="card-title">物模型定义</span>
              <el-button class="glass-button" type="primary" size="small" @click="handleEditThingModel">编辑物模型</el-button>
            </div>
          </template>
          <el-tabs v-model="thingModelTab">
            <el-tab-pane label="属性" name="properties">
              <el-table
                :data="thingModel.properties"
                border
                size="small"
                v-if="thingModel.properties?.length"
              >
                <el-table-column prop="identifier" label="标识符" width="150" />
                <el-table-column prop="name" label="名称" width="150" />
                <el-table-column prop="dataType" label="数据类型" width="100" />
                <el-table-column prop="unit" label="单位" width="80" />
                <el-table-column prop="min" label="最小值" width="80" />
                <el-table-column prop="max" label="最大值" width="80" />
              </el-table>
              <el-empty description="暂无属性定义" v-else />
            </el-tab-pane>
            <el-tab-pane label="事件" name="events">
              <el-table
                :data="thingModel.events"
                border
                size="small"
                v-if="thingModel.events?.length"
              >
                <el-table-column prop="identifier" label="标识符" width="200" />
                <el-table-column prop="name" label="名称" width="200" />
                <el-table-column prop="type" label="事件类型" width="120">
                  <template #default="{ row }: { row: EventDefinition }">
                    <el-tag class="glass-tag" :type="row.type === 'info' ? 'info' : row.type === 'alert' ? 'warning' : 'danger'">
                      {{ row.type === 'info' ? '信息' : row.type === 'alert' ? '告警' : '故障' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty description="暂无事件定义" v-else />
            </el-tab-pane>
            <el-tab-pane label="服务" name="services">
              <el-table
                :data="thingModel.services"
                border
                size="small"
                v-if="thingModel.services?.length"
              >
                <el-table-column prop="identifier" label="标识符" width="200" />
                <el-table-column prop="name" label="名称" width="200" />
                <el-table-column prop="callType" label="调用方式" width="120">
                  <template #default="{ row }: { row: ServiceDefinition }">
                    <el-tag class="glass-tag">{{ row.callType === 'sync' ? '同步' : '异步' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty description="暂无服务定义" v-else />
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 关联设备 -->
        <el-card class="glass-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">关联设备 ({{ devices.length }} 台)</span>
              <el-button class="glass-button" type="primary" size="small" @click="handleAddDevice">添加设备</el-button>
            </div>
          </template>
          <el-table :data="devices" border size="small" v-loading="devicesLoading">
            <el-table-column prop="deviceKey" label="设备Key" width="200" />
            <el-table-column prop="deviceName" label="设备名称" width="180" />
            <el-table-column prop="protocolType" label="协议类型" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }: { row: Device }">
                <el-tag class="glass-tag" :type="row.status === '1' ? 'success' : 'danger'" size="small">
                  {{ row.status === '1' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }: { row: Device }">
                <el-button class="glass-button" size="small" @click="viewDevice(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty description="暂无关联设备" v-if="!devices.length && !devicesLoading" />
        </el-card>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// 类型定义
interface Product {
  id: number
  productKey: string
  productName: string
  productType: string
  protocolType: string
  nodeType: string
  dataFormat: string
  status: string
  createTime: string
  updateTime: string
  thingModel?: ThingModel
}

interface Device {
  id: number
  deviceKey: string
  deviceName: string
  protocolType: string
  status: string
  createTime: string
}

interface PropertyDefinition {
  identifier: string
  name: string
  dataType: string
  unit: string
  min: string
  max: string
}

interface EventDefinition {
  identifier: string
  name: string
  type: string
}

interface ServiceDefinition {
  identifier: string
  name: string
  callType: string
}

interface ThingModel {
  properties: PropertyDefinition[]
  events: EventDefinition[]
  services: ServiceDefinition[]
}

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const devicesLoading = ref(false)
const productId = ref<number>(0)

// 产品信息
const product = reactive<Product>({
  id: 0,
  productKey: '',
  productName: '',
  productType: '',
  protocolType: '',
  nodeType: '',
  dataFormat: '',
  status: '',
  createTime: '',
  updateTime: ''
})

// 物模型
const thingModelTab = ref('properties')
const thingModel = reactive<ThingModel>({
  properties: [],
  events: [],
  services: []
})

// 关联设备
const devices = ref<Device[]>([])

// 返回
function goBack(): void {
  router.push('/product')
}

// 加载产品详情
async function loadProduct(): Promise<void> {
  try {
    loading.value = true
    const data = await request.get(`/products/${productId.value}`)
    Object.assign(product, data)
    // 加载物模型
    if (data.thingModel) {
      Object.assign(thingModel, data.thingModel)
    }
  } catch (error: unknown) {
    const err = error as Error
    ElMessage.error(err.message || '加载产品详情失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 加载关联设备
async function loadDevices(): Promise<void> {
  try {
    devicesLoading.value = true
    const data = await request.get(`/products/${productId.value}/devices`)
    devices.value = data || []
  } catch {
    devices.value = []
  } finally {
    devicesLoading.value = false
  }
}

// 编辑产品
function handleEdit(): void {
  router.push(`/product/${productId.value}/edit`)
}

// 编辑物模型
function handleEditThingModel(): void {
  router.push(`/product/${productId.value}/thing-model`)
}

// 添加设备
function handleAddDevice(): void {
  router.push(`/device/create?productId=${productId.value}`)
}

// 查看设备
function viewDevice(row: Device): void {
  router.push(`/device/${row.id}`)
}

onMounted(() => {
  productId.value = Number(route.params.id)
  if (!productId.value) {
    ElMessage.error('产品ID无效')
    goBack()
    return
  }
  loadProduct()
  loadDevices()
})
</script>

<style scoped>
.product-detail-page {
  padding: 24px;
}

.product-title {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  margin-right: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: 600;
  color: #f1f5f9;
}
</style>
