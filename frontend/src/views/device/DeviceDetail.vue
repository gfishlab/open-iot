<template>
  <div class="device-detail-page">
    <!-- 头部信息 -->
    <el-page-header @back="goBack" style="margin-bottom: 24px">
      <template #content>
        <div class="header-content">
          <span class="device-title">{{ deviceInfo.deviceName }}</span>
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
        <el-card style="margin-bottom: 16px">
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
          <div class="action-btns">
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
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <!-- 实时数据 -->
          <el-tab-pane label="实时数据" name="data">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>设备属性</span>
                  <el-button size="small" @click="refreshData">刷新数据</el-button>
                </div>
              </template>
              <div v-loading="dataLoading" style="min-height: 300px">
                <el-descriptions v-if="properties.length > 0" :column="2" border>
                  <el-descriptions-item
                    v-for="prop in properties"
                    :key="prop.identifier"
                    :label="prop.name"
                  >
                    <span class="prop-value" :style="getPropertyStyle(prop.value)">
                      {{ formatValue(prop.value, prop.dataType) }}
                    </span>
                    <span v-if="prop.unit" class="prop-unit">{{ prop.unit }}</span>
                  </el-descriptions-item>
                </el-descriptions>
                <el-empty v-else description="暂无数据" />
              </div>
            </el-card>
          </el-tab-pane>

          <!-- 数据历史 -->
          <el-tab-pane label="数据历史" name="history">
            <el-card>
              <div class="history-toolbar">
                <el-select v-model="selectedProperty" placeholder="选择属性" style="width: 160px">
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
                  style="width: 320px"
                />
                <el-button type="primary" @click="loadHistoryData">查询</el-button>
              </div>
              <div v-loading="historyLoading" style="min-height: 300px">
                <div v-if="historyData.length > 0" ref="chartContainer" style="height: 300px"></div>
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
                  <el-select v-model="serviceForm.serviceId" placeholder="请选择服务" style="width: 100%">
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
                  <span class="event-content">{{ event.content }}</span>
                </el-timeline-item>
                <el-empty v-if="events.length === 0" description="暂无事件记录" />
              </el-timeline>
            </el-card>
          </el-tab-pane>

          <!-- 设备影子 -->
          <el-tab-pane label="设备影子" name="shadow">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>设备影子数据</span>
                  <el-button class="glass-button" size="small" @click="loadShadow">刷新</el-button>
                </div>
              </template>
              <div v-loading="shadowLoading">
                <!-- 影子版本信息 -->
                <div class="shadow-version">
                  <span class="version-label">版本号:</span>
                  <el-tag class="glass-tag">{{ shadowData.version || '-' }}</el-tag>
                  <span class="version-time">更新时间: {{ shadowData.timestamp || '-' }}</span>
                </div>

                <!-- 三个数据区域 -->
                <el-row :gutter="16" style="margin-top: 16px">
                  <!-- Reported (设备上报) -->
                  <el-col :span="8">
                    <div class="shadow-section shadow-section--reported">
                      <div class="section-header">
                        <span class="section-title">Reported (设备上报)</span>
                      </div>
                      <div class="section-content">
                        <pre v-if="shadowData.reported" class="json-viewer">{{ JSON.stringify(shadowData.reported, null, 2) }}</pre>
                        <el-empty v-else description="暂无数据" :image-size="60" />
                      </div>
                    </div>
                  </el-col>

                  <!-- Desired (期望状态) -->
                  <el-col :span="8">
                    <div class="shadow-section shadow-section--desired">
                      <div class="section-header">
                        <span class="section-title">Desired (期望状态)</span>
                        <el-button class="glass-button" size="small" @click="showUpdateDesiredDialog">修改</el-button>
                      </div>
                      <div class="section-content">
                        <pre v-if="shadowData.desired" class="json-viewer">{{ JSON.stringify(shadowData.desired, null, 2) }}</pre>
                        <el-empty v-else description="暂无数据" :image-size="60" />
                      </div>
                    </div>
                  </el-col>

                  <!-- Delta (差异) -->
                  <el-col :span="8">
                    <div class="shadow-section shadow-section--delta">
                      <div class="section-header">
                        <span class="section-title">Delta (差异)</span>
                      </div>
                      <div class="section-content">
                        <pre v-if="shadowData.delta && Object.keys(shadowData.delta).length > 0" class="json-viewer">{{ JSON.stringify(shadowData.delta, null, 2) }}</pre>
                        <el-empty v-else description="无差异" :image-size="60" />
                      </div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </el-card>
          </el-tab-pane>

          <!-- 设备轨迹 -->
          <el-tab-pane label="设备轨迹" name="trajectory">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>设备轨迹</span>
                  <el-button class="glass-button" size="small" @click="loadTrajectory">刷新</el-button>
                </div>
              </template>
              <div v-loading="trajectoryLoading">
                <!-- 时间范围选择器 -->
                <div class="trajectory-toolbar">
                  <el-date-picker
                    v-model="trajectoryDateRange"
                    type="datetimerange"
                    range-separator="-"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    style="width: 320px"
                  />
                  <el-button class="glass-button" type="primary" @click="handleQueryTrajectory">查询轨迹</el-button>
                  <el-button class="glass-button" @click="handleQueryStatistics">统计信息</el-button>
                </div>

                <!-- 轨迹统计卡片 -->
                <div v-if="trajectoryStats" class="trajectory-stats">
                  <div class="stat-item">
                    <span class="stat-icon">📍</span>
                    <div class="stat-info">
                      <span class="stat-label">轨迹点数</span>
                      <span class="stat-value">{{ trajectoryStats.pointCount || 0 }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <span class="stat-icon">📏</span>
                    <div class="stat-info">
                      <span class="stat-label">总距离</span>
                      <span class="stat-value">{{ formatDistance(trajectoryStats.totalDistance) }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <span class="stat-icon">⚡</span>
                    <div class="stat-info">
                      <span class="stat-label">平均速度</span>
                      <span class="stat-value">{{ formatSpeed(trajectoryStats.avgSpeed) }}</span>
                    </div>
                  </div>
                  <div class="stat-item">
                    <span class="stat-icon">🚀</span>
                    <div class="stat-info">
                      <span class="stat-label">最大速度</span>
                      <span class="stat-value">{{ formatSpeed(trajectoryStats.maxSpeed) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 轨迹列表（表格形式，简单展示） -->
                <div class="trajectory-list" v-if="trajectoryPoints.length > 0">
                  <el-table
                    :data="trajectoryPoints"
                    size="small"
                    :max-height="300"
                    style="width: 100%"
                  >
                    <el-table-column prop="eventTime" label="时间" width="180" />
                    <el-table-column prop="latitude" label="纬度" width="120">
                      <template #default="{ row }">{{ row.latitude?.toFixed(6) }}</template>
                    </el-table-column>
                    <el-table-column prop="longitude" label="经度" width="120">
                      <template #default="{ row }">{{ row.longitude?.toFixed(6) }}</template>
                    </el-table-column>
                    <el-table-column prop="speed" label="速度(km/h)" width="100">
                      <template #default="{ row }">{{ row.speed || '-' }}</template>
                    </el-table-column>
                    <el-table-column prop="heading" label="航向角(°)" width="100">
                      <template #default="{ row }">{{ row.heading || '-' }}</template>
                    </el-table-column>
                  </el-table>
                </div>
                <el-empty v-else description="暂无轨迹数据，请选择时间范围查询" :image-size="80" />
              </div>
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

    <!-- 更新期望属性对话框 -->
    <el-dialog v-model="updateDesiredDialogVisible" title="更新期望属性" width="600px">
      <el-form :model="updateDesiredForm" label-width="100px">
        <el-form-item label="当前版本">
          <el-tag class="glass-tag">{{ updateDesiredForm.version }}</el-tag>
          <span style="color: #64748b; font-size: 12px; margin-left: 8px;">用于乐观锁并发控制</span>
        </el-form-item>
        <el-form-item label="期望属性">
          <el-input
            v-model="updateDesiredForm.desired"
            type="textarea"
            :rows="10"
            placeholder='请输入 JSON 格式的期望属性，如: {"temperature": 25}'
          />
        </el-form-item>
        <el-form-item>
          <el-alert
            title="提示"
            type="warning"
            :closable="false"
            show-icon
          >
            <template #default>
            <div style="font-size: 12px; color: #64748b;">
              • 输入的期望属性将与设备上报属性对比，自动计算差值（delta）<br>
              • 如果版本号不匹配，更新将被拒绝，请刷新后重试
            </div>
            </template>
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="glass-button" @click="updateDesiredDialogVisible = false">取消</el-button>
        <el-button class="glass-button" type="primary" @click="confirmUpdateDesired">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import echarts from '@/utils/echarts'

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

// 设备影子
const shadowLoading = ref(false)
const shadowData = ref<any>({
  version: 0,
  timestamp: '',
  reported: {},
  desired: {},
  delta: {}
})
const updateDesiredDialogVisible = ref(false)
const updateDesiredForm = reactive({
  version: 0,
  desired: '{}'
})

// 设备轨迹
const trajectoryLoading = ref(false)
const trajectoryDateRange = ref<any[]>([])
const trajectoryPoints = ref<any[]>([])
const trajectoryStats = ref<any>(null)

// 加载设备信息
async function loadDeviceInfo() {
  try {
    loading.value = true
    const data = await request.get(`/devices/${deviceId}`)
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
    const data = await request.get(`/devices/${deviceId}/data/latest`)
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

// 获取属性值的内联样式（替代 Tailwind 颜色类）
function getPropertyStyle(value: any) {
  if (typeof value === 'boolean') {
    return { color: value ? '#34d399' : '#94a3b8' }
  }
  return {}
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
    const data = await request.get(`/devices/${deviceId}/data/history`, {
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
    await request.post(`/devices/${deviceId}/services/${serviceForm.serviceId}/call`, {
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
    await request.put(`/devices/${deviceId}/properties/${propertyForm.propertyId}`, {
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
    await request.post(`/devices/${deviceId}/commands/${commandForm.commandId}`, {
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
    await request.delete(`/devices/${deviceId}`)
    ElMessage.success('删除成功')
    goBack()
  } catch (error) {
    // 取消删除
  }
}

// ===== 设备影子相关方法 =====

// 加载设备影子
async function loadShadow() {
  try {
    shadowLoading.value = true
    const data = await request.get(`/devices/${deviceId}/shadow`)
    shadowData.value = {
      version: data.version || 0,
      timestamp: data.timestamp || data.updateTime,
      reported: data.reported || {},
      desired: data.desired || {},
      delta: data.delta || {}
    }
  } catch (error) {
    ElMessage.error('加载设备影子失败')
  } finally {
    shadowLoading.value = false
  }
}

// 显示更新期望属性对话框
function showUpdateDesiredDialog() {
  updateDesiredForm.version = shadowData.value.version
  updateDesiredForm.desired = JSON.stringify(shadowData.value.desired || {}, null, 2)
  updateDesiredDialogVisible.value = true
}

// 更新期望属性
async function confirmUpdateDesired() {
  try {
    let desired: any
    try {
      desired = JSON.parse(updateDesiredForm.desired)
    } catch {
      ElMessage.error('期望属性 JSON 格式错误')
      return
    }

    const data = await request.put(`/devices/${deviceId}/shadow/desired`, {
      version: updateDesiredForm.version,
      desired: desired
    })

    // 更新本地影子数据
    shadowData.value = {
      version: data.version || shadowData.value.version + 1,
      timestamp: data.timestamp || data.updateTime,
      reported: data.reported || shadowData.value.reported,
      desired: data.desired || desired,
      delta: data.delta || {}
    }

    ElMessage.success('期望属性更新成功')
    updateDesiredDialogVisible.value = false
  } catch (error: any) {
    if (error?.response?.status === 409) {
      ElMessage.warning('数据已被修改，请刷新后重试')
      loadShadow()
    } else {
      ElMessage.error('更新期望属性失败')
    }
  }
}

// ===== 设备轨迹相关方法 =====

// 加载设备轨迹数据（默认加载最近1小时）
async function loadTrajectory() {
  try {
    trajectoryLoading.value = true

    // 如果没有选择时间范围，默认查询最近1小时
    if (!trajectoryDateRange.value || trajectoryDateRange.value.length !== 2) {
      const endTime = new Date()
      const startTime = new Date(endTime.getTime() - 60 * 60 * 1000) // 1小时前
      trajectoryDateRange.value = [startTime, endTime]
    }

    const [startTime, endTime] = trajectoryDateRange.value
    const data = await request.get(`/devices/${deviceId}/trajectory`, {
      params: {
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString()
      }
    })

    trajectoryPoints.value = data.points || data.list || []
    trajectoryStats.value = data.statistics || data.stats || null
  } catch (error) {
    ElMessage.error('加载轨迹数据失败')
  } finally {
    trajectoryLoading.value = false
  }
}

// 查询轨迹（根据选择的时间范围）
async function handleQueryTrajectory() {
  if (!trajectoryDateRange.value || trajectoryDateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  try {
    trajectoryLoading.value = true
    const [startTime, endTime] = trajectoryDateRange.value
    const data = await request.get(`/devices/${deviceId}/trajectory`, {
      params: {
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString()
      }
    })

    trajectoryPoints.value = data.points || data.list || []
    trajectoryStats.value = data.statistics || data.stats || null

    if (trajectoryPoints.value.length === 0) {
      ElMessage.info('所选时间范围内无轨迹数据')
    } else {
      ElMessage.success(`查询到 ${trajectoryPoints.value.length} 条轨迹记录`)
    }
  } catch (error) {
    ElMessage.error('查询轨迹失败')
  } finally {
    trajectoryLoading.value = false
  }
}

// 查询轨迹统计信息
async function handleQueryStatistics() {
  if (!trajectoryDateRange.value || trajectoryDateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  try {
    trajectoryLoading.value = true
    const [startTime, endTime] = trajectoryDateRange.value
    const data = await request.get(`/devices/${deviceId}/trajectory/statistics`, {
      params: {
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString()
      }
    })

    trajectoryStats.value = data

    // 显示统计信息弹窗
    ElMessageBox.alert(
      `
      <div style="text-align: left; padding: 10px;">
        <p><strong>轨迹点数：</strong> ${data.pointCount || 0}</p>
        <p><strong>总距离：</strong> ${formatDistance(data.totalDistance)}</p>
        <p><strong>平均速度：</strong> ${formatSpeed(data.avgSpeed)}</p>
        <p><strong>最大速度：</strong> ${formatSpeed(data.maxSpeed)}</p>
        <p><strong>起始时间：</strong> ${new Date(data.startTime || 0).toLocaleString()}</p>
        <p><strong>结束时间：</strong> ${new Date(data.endTime || 0).toLocaleString()}</p>
      </div>
      `,
      '轨迹统计信息',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确定'
      }
    )
  } catch (error) {
    ElMessage.error('查询统计信息失败')
  } finally {
    trajectoryLoading.value = false
  }
}

// 格式化距离（米 -> 千米）
function formatDistance(meters: number): string {
  if (!meters || meters === 0) return '0 m'
  if (meters < 1000) {
    return `${meters.toFixed(0)} m`
  }
  return `${(meters / 1000).toFixed(2)} km`
}

// 格式化速度（米/秒 -> 千米/小时）
function formatSpeed(metersPerSecond: number): string {
  if (!metersPerSecond || metersPerSecond === 0) return '0 km/h'
  const kmh = metersPerSecond * 3.6
  return `${kmh.toFixed(1)} km/h`
}

// 切换标签页时加载数据
function handleTabChange(tabName: string) {
  if (tabName === 'shadow' && Object.keys(shadowData.value.reported).length === 0) {
    loadShadow()
  } else if (tabName === 'trajectory' && trajectoryPoints.value.length === 0) {
    // 首次切换到轨迹标签页时，自动加载最近1小时的数据
    loadTrajectory()
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

/* 页面头部 */
.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.device-title {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}

/* 操作按钮列 */
.action-btns {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 卡片标题 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 属性值 */
.prop-value {
  font-size: 16px;
  font-weight: 500;
  color: #f1f5f9;
}

.prop-unit {
  color: #64748b;
  margin-left: 4px;
  font-size: 13px;
}

/* 历史查询工具栏 */
.history-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}

/* 事件内容 */
.event-content {
  margin-left: 8px;
  color: #94a3b8;
}

/* ===== 设备影子样式 ===== */
.shadow-version {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  margin-bottom: 16px;
}

.version-label {
  font-size: 13px;
  color: #64748b;
}

.version-time {
  margin-left: auto;
  font-size: 12px;
  color: #4b5563;
}

.shadow-section {
  background: rgba(30, 41, 59, 0.5);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 200px;
}

.shadow-section--reported {
  border-color: rgba(52, 211, 153, 0.2);
}

.shadow-section--desired {
  border-color: rgba(99, 102, 241, 0.2);
}

.shadow-section--delta {
  border-color: rgba(251, 191, 36, 0.2);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}

.section-content {
  flex: 1;
  padding: 16px;
  min-height: 0;
}

.json-viewer {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 12px;
  font-size: 12px;
  color: #94a3b8;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

/* ===== 设备轨迹样式 ===== */
.trajectory-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.trajectory-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(99, 102, 241, 0.3);
}

.stat-icon {
  font-size: 24px;
  line-height: 1;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.trajectory-list {
  background: rgba(30, 41, 59, 0.5);
  border-radius: 8px;
  overflow: hidden;
}
</style>
