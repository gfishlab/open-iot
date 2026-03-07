<template>
  <div class="dashboard-page">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="mb-6">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-blue-100">
              <svg class="w-10 h-10 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
              </svg>
            </div>
            <div class="stat-text">
              <div class="stat-label">设备总数</div>
              <div class="stat-value">{{ dashboardData.totalDevices }}</div>
              <div class="stat-desc">较上周 +12%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-green-100">
              <svg class="w-10 h-10 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.636 18.364a9 9 0 010-12.728m0 0l12.728-12.728a9 9 0 010 12.728m0 0l-12.728 12.728a9 9 0 010-12.728" />
                <circle cx="12" cy="12" r="3" fill="currentColor" />
              </svg>
            </div>
            <div class="stat-text">
              <div class="stat-label">在线设备</div>
              <div class="stat-value text-green-600">{{ dashboardData.onlineDevices }}</div>
              <div class="stat-desc">在线率 {{ onlineRate }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-yellow-100">
              <svg class="w-10 h-10 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div class="stat-text">
              <div class="stat-label">今日告警</div>
              <div class="stat-value text-yellow-600">{{ dashboardData.todayAlerts }}</div>
              <div class="stat-desc">待处理 {{ dashboardData.pendingAlerts }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon bg-purple-100">
              <svg class="w-10 h-10 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2M9 11H9" />
              </svg>
            </div>
            <div class="stat-text">
              <div class="stat-label">产品数量</div>
              <div class="stat-value">{{ dashboardData.totalProducts }}</div>
              <div class="stat-desc">{{ dashboardData.activeProducts }} 个启用</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧：设备状态分布 -->
      <el-col :span="12">
        <el-card class="mb-4">
          <template #header>
            <span>设备状态分布</span>
          </template>
          <div ref="statusChart" style="height: 280px"></div>
        </el-card>

        <!-- 告警趋势 -->
        <el-card>
          <template #header>
            <span>告警趋势（近7天）</span>
          </template>
          <div ref="alertTrendChart" style="height: 250px"></div>
        </el-card>
      </el-col>

      <!-- 右侧：实时告警 -->
      <el-col :span="12">
        <el-card class="mb-4">
          <template #header>
            <div class="flex justify-between items-center">
              <span>最新告警</span>
              <el-button size="small" text @click="$router.push('/alerts')">查看全部</el-button>
            </div>
          </template>
          <el-timeline v-loading="alertLoading">
            <el-timeline-item
              v-for="alert in recentAlerts"
              :key="alert.id"
              :timestamp="alert.alertTime"
              placement="top"
            >
              <div class="flex items-center gap-2">
                <el-tag :type="getAlertType(alert.alertLevel)" size="small">
                  {{ alert.alertLevel }}
                </el-tag>
                <span class="font-medium">{{ alert.alertTitle }}</span>
              </div>
              <div class="text-sm text-gray-500 mt-1">
                设备: {{ alert.deviceName }}
              </div>
            </el-timeline-item>
            <el-empty v-if="recentAlerts.length === 0" description="暂无告警" :image-size="80" />
          </el-timeline>
        </el-card>

        <!-- 设备状态列表 -->
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span>设备状态</span>
              <el-button size="small" text @click="$router.push('/devices')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="deviceStatus" size="small" max-height="300">
            <el-table-column prop="deviceName" label="设备名称" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <div :class="['w-2 h-2 rounded-full', row.online ? 'bg-green-500' : 'bg-gray-400']"></div>
                  <span>{{ row.online ? '在线' : '离线' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="lastActive" label="最后激活" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

// 仪表板数据
const dashboardData = ref<any>({
  totalDevices: 0,
  onlineDevices: 0,
  todayAlerts: 0,
  pendingAlerts: 0,
  totalProducts: 0,
  activeProducts: 0
})

// 在线率
const onlineRate = computed(() => {
  if (dashboardData.value.totalDevices === 0) return 0
  return Math.round((dashboardData.value.onlineDevices / dashboardData.value.totalDevices) * 100)
})

// 告警列表
const recentAlerts = ref<any[]>([])
const alertLoading = ref(false)

// 设备状态
const deviceStatus = ref<any[]>([])

// 图表
const statusChart = ref<HTMLElement>()
const alertTrendChart = ref<HTMLElement>()
let statusChartInstance: echarts.ECharts | null = null
let alertTrendChartInstance: echarts.ECharts | null = null

// 加载仪表板数据
async function loadDashboardData() {
  try {
    // 并行加载多个数据
    const [stats, alerts, devices] = await Promise.allSettled([
      request.get('/api/dashboard/statistics').catch(() => ({ data: getMockStats() })),
      request.get('/api/alerts', { params: { page: 1, size: 5 } }).catch(() => ({ data: [] })),
      request.get('/api/devices', { params: { page: 1, size: 5 } }).catch(() => ({ data: [] }))
    ])

    if (stats.status === 'fulfilled') {
      Object.assign(dashboardData.value, stats.data)
    }
    if (alerts.status === 'fulfilled') {
      recentAlerts.value = alerts.data?.records || alerts.data || []
    }
    if (devices.status === 'fulfilled') {
      deviceStatus.value = devices.data?.records || devices.data || []
    }

    // 渲染图表
    renderCharts()
  } catch (error) {
    console.error('加载仪表板数据失败', error)
  }
}

// 模拟统计数据（用于开发）
function getMockStats() {
  return {
    totalDevices: 156,
    onlineDevices: 128,
    todayAlerts: 23,
    pendingAlerts: 5,
    totalProducts: 12,
    activeProducts: 10
  }
}

// 渲染图表
function renderCharts() {
  // 设备状态分布饼图
  if (statusChart.value) {
    if (!statusChartInstance) {
      statusChartInstance = echarts.init(statusChart.value)
    }
    const statusOption = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center'
      },
      series: [{
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: dashboardData.value.onlineDevices, name: '在线', itemStyle: { color: '#22c55e' } },
          { value: dashboardData.value.totalDevices - dashboardData.value.onlineDevices, name: '离线', itemStyle: { color: '#94a3b8' } }
        ]
      }]
    }
    statusChartInstance.setOption(statusOption)
  }

  // 告警趋势折线图
  if (alertTrendChart.value) {
    if (!alertTrendChartInstance) {
      alertTrendChartInstance = echarts.init(alertTrendChart.value)
    }
    const trendOption = {
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        name: '告警数',
        type: 'line',
        stack: 'Total',
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#f59e0b'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 158, 11, 0.3)' },
            { offset: 1, color: 'rgba(245, 158, 11, 0.05)' }
          ])
        },
        showSymbol: false,
        data: [12, 18, 15, 23, 20, 16, 23]
      }]
    }
    }
    alertTrendChartInstance.setOption(trendOption)
  }
}

// 告警类型样式
function getAlertType(level: string) {
  const map: Record<string, string> = {
    critical: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return map[level] || ''
}

// 窗口大小改变时重绘图表
function handleResize() {
  statusChartInstance?.resize()
  alertTrendChartInstance?.resize()
}

onMounted(() => {
  loadDashboardData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  statusChartInstance?.dispose()
  alertTrendChartInstance?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  padding: 20px;
}

.stat-card {
  border: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-text {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.stat-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
