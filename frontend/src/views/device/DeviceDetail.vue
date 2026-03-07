<template>
  <div class="device-detail-page">
    <!-- 头部信息 -->
    <el-page-header @back="goBack" class="mb-6">
      <template #content>
        <div class="flex items-center gap-4">
          <span class="text-lg font-semibold">{{ deviceInfo.deviceName }}</span>
          <el-tag :type="deviceInfo.online ? 'success' : 'info'">
            {{ deviceInfo.online ? '在线' : '离线' }}
          </el-tag>
        </div>
      </template>
      <template #extra>
        <el-button type="primary" @click="handleRefresh">刷新</el-button>
      </template>
    </el-page-header>

    <el-row :gutter="20">
      <!-- 左侧：设备基本信息 -->
      <el-col :span="8">
        <el-card class="mb-4">
          <template #header>
            <span>设备信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备编码">{{ deviceInfo.deviceCode }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ deviceInfo.deviceName }}</el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ deviceInfo.productName }}</el-descriptions-item>
            <el-descriptions-item label="所属网关">{{ deviceInfo.gatewayName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="IP 地址">{{ deviceInfo.ipAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="固件版本">{{ deviceInfo.firmwareVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ deviceInfo.createTime }}</el-descriptions-item>
            <el-descriptions-item label="最后激活">{{ deviceInfo.lastActiveTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 设备操作 -->
        <el-card>
          <template #header>
            <span>设备操作</span>
          </template>
          <div class="flex flex-col gap-3">
            <el-button type="primary" :disabled="!deviceInfo.online" @click="handleControl">
              设备控制
            </el-button>
            <el-button type="warning" @click="handleEdit">编辑设备</el-button>
            <el-button type="danger" @click="handleDelete">删除设备</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：设备数据 -->
      <el-col :span="16">
        <!-- 标签页 -->
        <el-tabs v-model="activeTab">
          <!-- 实时数据 -->
          <el-tab-pane label="实时数据" name="data">
            <el-card>
              <template #header>
                <div class="flex justify-between items-center">
                  <span>设备属性</span>
                  <el-button size="small" @click="refreshData">刷新数据</el-button>
                </div>
              </template>
              <div v-loading="dataLoading" class="min-h-[300px]">
                <el-descriptions v-if="properties.length > 0" :column="2" border>
                  <el-descriptions-item
                    v-for="prop in properties"
                    :key="prop.identifier"
                    :label="prop.name"
                  >
                    <span class="text-lg font-medium" :class="getPropertyClass(prop.value)">
                      {{ formatValue(prop.value, prop.dataType) }}
                    </span>
                    <span v-if="prop.unit" class="text-gray-500 ml-1">{{ prop.unit }}</span>
                  </el-descriptions-item>
                </el-descriptions>
                <el-empty v-else description="暂无数据" />
              </div>
            </el-card>
          </el-tab-pane>

          <!-- 数据历史 -->
          <el-tab-pane label="数据历史" name="history">
            <el-card>
              <div class="mb-4 flex gap-2">
                <el-select v-model="selectedProperty" placeholder="选择属性" class="w-40">
                  <el-option
                    v-for="prop in properties"
                    :key="prop.identifier"
                    :label="prop.name"
                    :value="prop.identifier"
                  />
                </el-select>
                <el-date-picker
                  v-model="dateRange"
                  type="datetimerange"
                  range-separator="-"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  class="w-80"
                />
                <el-button type="primary" @click="loadHistoryData">查询</el-button>
              </div>
              <div v-loading="historyLoading" class="min-h-[300px]">
                <div v-if="historyData.length > 0" ref="chartContainer" class="h-[300px]"></div>
                <el-empty v-else description="暂无历史数据" />
              </div>
            </el-card>
          </el-tab-pane>

          <!-- 服务调用 -->
          <el-tab-pane label="服务调用" name="service">
            <el-card>
              <template #header>
                <span>调用服务</span>
              </template>
              <el-form :model="serviceForm" label-width="120px">
                <el-form-item label="选择服务">
                  <el-select v-model="serviceForm.serviceId" placeholder="请选择服务" class="w-full">
                    <el-option
                      v-for="service in services"
                      :key="service.id"
                      :label="service.name"
                      :value="service.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="调用参数">
                  <el-input
                    v-model="serviceForm.params"
                    type="textarea"
                    :rows="3"
                    placeholder='JSON 格式，如: {"delay": 5000}'
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="callService" :loading="serviceCalling">
                    调用服务
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-tab-pane>

          <!-- 事件记录 -->
          <el-tab-pane label="事件记录" name="events">
            <el-card>
              <el-timeline v-loading="eventsLoading">
                <el-timeline-item
                  v-for="event in events"
                  :key="event.id"
                  :timestamp="event.timestamp"
                  placement="top"
                >
                  <el-tag :type="getEventType(event.level)" size="small">{{ event.level }}</el-tag>
                  <span class="ml-2">{{ event.content }}</span>
                </el-timeline-item>
                <el-empty v-if="events.length === 0" description="暂无事件记录" />
              </el-timeline>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>

    <!-- 设备控制对话框 -->
    <el-dialog v-model="controlDialogVisible" title="设备控制" width="600px">
      <el-tabs v-model="controlTab">
        <el-tab-pane label="设置属性" name="property">
          <el-form :model="propertyForm" label-width="120px">
            <el-form-item label="选择属性">
              <el-select v-model="propertyForm.propertyId" placeholder="请选择属性">
                <el-option
                  v-for="prop in writableProperties"
                  :key="prop.identifier"
                  :label="prop.name"
                  :value="prop.identifier"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="属性值">
              <el-input v-model="propertyForm.value" placeholder="请输入属性值" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="setProperty" :loading="settingProperty">
                设置属性
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="发送命令" name="command">
          <el-form :model="commandForm" label-width="120px">
            <el-form-item label="选择命令">
              <el-select v-model="commandForm.commandId" placeholder="请选择命令">
                <el-option
                  v-for="cmd in commands"
                  :key="cmd.id"
                  :label="cmd.name"
                  :value="cmd.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="命令参数">
              <el-input
                v-model="commandForm.params"
                type="textarea"
                :rows="3"
                placeholder='JSON 格式，如: {"mode": "factory"}'
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="sendCommand" :loading="sendingCommand">
                发送命令
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const deviceId = route.params.id as string

// 设备信息
const deviceInfo = ref<any>({})
const loading = ref(false)

// 标签页
const activeTab = ref('data')

// 实时数据
const dataLoading = ref(false)
const properties = ref<any[]>([])

// 历史数据
const historyLoading = ref(false)
const selectedProperty = ref('')
const dateRange = ref<any[]>([])
const historyData = ref<any[]>([])
const chartContainer = ref<HTMLElement>()

// 服务调用
const services = ref<any[]>([])
const serviceForm = reactive({
  serviceId: '',
  params: ''
})
const serviceCalling = ref(false)

// 事件记录
const events = ref<any[]>([])
const eventsLoading = ref(false)

// 设备控制
const controlDialogVisible = ref(false)
const controlTab = ref('property')
const writableProperties = ref<any[]>([])
const commands = ref<any[]>([])
const propertyForm = reactive({
  propertyId: '',
  value: ''
})
const commandForm = reactive({
  commandId: '',
  params: ''
})
const settingProperty = ref(false)
const sendingCommand = ref(false)

// 加载设备信息
async function loadDeviceInfo() {
  try {
    loading.value = true
    const data = await request.get(`/api/devices/${deviceId}`)
    deviceInfo.value = data
  } catch (error) {
    ElMessage.error('加载设备信息失败')
  } finally {
    loading.value = false
  }
}

// 刷新设备状态
function handleRefresh() {
  loadDeviceInfo()
  refreshData()
}

// 返回列表
function goBack() {
  router.back()
}

// 刷新实时数据
async function refreshData() {
  try {
    dataLoading.value = true
    const data = await request.get(`/api/devices/${deviceId}/data/latest`)
    properties.value = data?.properties || []
  } catch (error) {
    ElMessage.error('加载实时数据失败')
  } finally {
    dataLoading.value = false
  }
}

// 格式化值
function formatValue(value: any, dataType: string) {
  if (value === null || value === undefined) return '-'
  if (dataType === 'boolean') return value ? '是' : '否'
  return String(value)
}

// 获取属性值的样式类
function getPropertyClass(value: any) {
  if (typeof value === 'boolean') {
    return value ? 'text-green-600' : 'text-gray-600'
  }
  return ''
}

// 加载历史数据
async function loadHistoryData() {
  if (!selectedProperty.value) {
    ElMessage.warning('请先选择属性')
    return
  }

  try {
    historyLoading.value = true
    const [startTime, endTime] = dateRange.value || []
    const data = await request.get(`/api/devices/${deviceId}/data/history`, {
      params: {
        property: selectedProperty.value,
        startTime,
        endTime
      }
    })
    historyData.value = data || []
    renderChart()
  } catch (error) {
    ElMessage.error('加载历史数据失败')
  } finally {
    historyLoading.value = false
  }
}

// 渲染图表
function renderChart() {
  if (!chartContainer.value || historyData.value.length === 0) return

  const chart = echarts.init(chartContainer.value)
  const option = {
    xAxis: {
      type: 'category',
      data: historyData.value.map((d: any) => d.time)
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      data: historyData.value.map((d: any) => d.value),
      type: 'line',
      smooth: true
    }]
  }
  chart.setOption(option)
}

// 调用服务
async function callService() {
  if (!serviceForm.serviceId) {
    ElMessage.warning('请选择服务')
    return
  }

  try {
    serviceCalling.value = true
    let params: any = {}
    if (serviceForm.params) {
      try {
        params = JSON.parse(serviceForm.params)
      } catch {
        ElMessage.error('参数格式错误，请使用 JSON 格式')
        return
      }
    }
    await request.post(`/api/devices/${deviceId}/services/${serviceForm.serviceId}/call`, {
      inputParams: params
    })
    ElMessage.success('服务调用已下发')
  } catch (error) {
    ElMessage.error('服务调用失败')
  } finally {
    serviceCalling.value = false
  }
}

// 获取事件类型样式
function getEventType(level: string) {
  const map: Record<string, string> = {
    info: '',
    warning: 'warning',
    error: 'danger',
    critical: 'danger'
  }
  return map[level] || ''
}

// 设备控制
function handleControl() {
  if (!deviceInfo.value.online) {
    ElMessage.warning('设备离线，无法控制')
    return
  }
  controlDialogVisible.value = true
}

// 设置属性
async function setProperty() {
  if (!propertyForm.propertyId || !propertyForm.value) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    settingProperty.value = true
    await request.put(`/api/devices/${deviceId}/properties/${propertyForm.propertyId}`, {
      value: propertyForm.value
    })
    ElMessage.success('属性设置已下发')
    controlDialogVisible.value = false
  } catch (error) {
    ElMessage.error('属性设置失败')
  } finally {
    settingProperty.value = false
  }
}

// 发送命令
async function sendCommand() {
  if (!commandForm.commandId) {
    ElMessage.warning('请选择命令')
    return
  }

  try {
    sendingCommand.value = true
    let params: any = {}
    if (commandForm.params) {
      try {
        params = JSON.parse(commandForm.params)
      } catch {
        ElMessage.error('参数格式错误，请使用 JSON 格式')
        return
      }
    }
    await request.post(`/api/devices/${deviceId}/commands/${commandForm.commandId}`, {
      params
    })
    ElMessage.success('命令已下发')
    controlDialogVisible.value = false
  } catch (error) {
    ElMessage.error('命令发送失败')
  } finally {
    sendingCommand.value = false
  }
}

// 编辑设备
function handleEdit() {
  ElMessage.info('编辑功能开发中')
}

// 删除设备
async function handleDelete() {
  try {
    await ElMessageBox.confirm(`确定删除设备 ${deviceInfo.value.deviceName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/api/devices/${deviceId}`)
    ElMessage.success('删除成功')
    goBack()
  } catch (error) {
    // 取消删除
  }
}

onMounted(() => {
  loadDeviceInfo()
  refreshData()
})
</script>

<style scoped>
.device-detail-page {
  padding: 20px;
}
</style>
