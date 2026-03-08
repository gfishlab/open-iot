<template>
  <div class="monitor-container">
    <!-- 顶部统计区域 -->
    <div class="stats-grid">
      <div class="stat-card devices">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 3v2m6-2a9 9 0 011.996 3.684A9 9 0 01-9-3.684A9 9 0 013.996 0 9z" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M15 12a3 3 0 11-3-3 3 3 0 013 3-3 3 3 0 013 3 3-3 3 3 0 013 3 3z" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">设备总数</span>
          <span class="stat-value">{{ dashboardData.totalDevices }}</span>
          <span class="stat-trend up">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 17l5-5M5 5m4.586-4.586a2 2 0 00-2.828 0L5 5l4.586-4.586z" stroke-linecap="round" stroke-linejoin="round"/></svg>
            较上周 +12%
          </span>
        </div>
      </div>

      <div class="stat-card online">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 7v2m0 10a2 2 0 010-2-2z"/>
            <path d="M12 2a10 10 0 0110 10 10 10 10 0 01-10 10A10 10 0 0110-10z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">在线设备</span>
          <span class="stat-value online">{{ dashboardData.onlineDevices }}</span>
          <span class="stat-trend up">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 17l5-5M5 5m4.586-4.586a2 2 0 00-2.828 0L5 5l4.586-4.586z" stroke-linecap="round" stroke-linejoin="round"/></svg>
            {{ onlineRate }}% 在线率
          </span>
        </div>
      </div>

      <div class="stat-card alerts">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 9v2m0 4m0 4h.01m-.167-3.22A5.02 5.02 0 00-5 4.1 4.1 0 00-1.9 3.16L6.1 8.6a5.02 5.02 0 00-5 4.1 4.1 0 00-1.9 3.16z"/>
            <path d="M15 17H9l3-3 3 3z"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">今日告警</span>
          <span class="stat-value alert">{{ dashboardData.todayAlerts }}</span>
          <span class="stat-trend warning" v-if="dashboardData.pendingAlerts > 0">
            {{ dashboardData.pendingAlerts }} 待处理
          </span>
        </div>
      </div>

      <div class="stat-card products">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 7l-8-4-8 4m0 6l8 4 8-4m0 6l-8 4 8 4M4.5 7 8 4V3.5L12 7 20 3.5V7z" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M4 12l8 4 8-4M4 17l8 4 8-4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">产品数量</span>
          <span class="stat-value">{{ dashboardData.totalProducts }}</span>
          <span class="stat-trend">
            {{ dashboardData.activeProducts }} 个启用中
          </span>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="content-grid">
      <!-- 左侧图表区 -->
      <div class="charts-section">
        <div class="chart-card">
          <div class="chart-header">
            <h3>设备状态分布</h3>
          </div>
          <div class="chart-body">
            <div ref="statusChart" class="chart-container"></div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>告警趋势（近7天）</h3>
          </div>
          <div class="chart-body">
            <div ref="alertTrendChart" class="chart-container"></div>
          </div>
        </div>
      </div>

      <!-- 右侧信息区 -->
      <div class="info-section">
        <div class="info-card alerts-list">
          <div class="info-header">
            <h3>最新告警</h3>
            <button class="view-all-btn" @click="$router.push('/alerts')">
              查看全部
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="info-body" v-loading="alertLoading">
            <div v-if="recentAlerts.length === 0" class="empty-state">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>暂无告警</span>
            </div>
            <div v-else class="alert-list">
              <div v-for="alert in recentAlerts" :key="alert.id" class="alert-item">
                <div class="alert-indicator" :class="alert.alertLevel"></div>
                <div class="alert-content">
                  <div class="alert-header">
                    <span :class="['alert-badge', alert.alertLevel]">{{ alert.alertLevel }}</span>
                    <span class="alert-title">{{ alert.alertTitle }}</span>
                  </div>
                  <span class="alert-device">设备: {{ alert.deviceName }}</span>
                </div>
                <span class="alert-time">{{ formatTime(alert.alertTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="info-card device-list">
          <div class="info-header">
            <h3>设备状态</h3>
            <button class="view-all-btn" @click="$router.push('/devices')">
              查看全部
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="info-body">
            <div v-if="deviceStatus.length === 0" class="empty-state">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M9 3v2m6-2a9 9 0 011.996 3.684A9 9 0 0124.996 5.684L21 12l-3.004 3.316A9 9 0 01-9 3.684M15 12a3 3 0 11-3-3 3 3 0 013 3z" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>暂无设备</span>
            </div>
            <div v-else class="device-items">
              <div v-for="device in deviceStatus" :key="device.id" class="device-item">
                <div class="device-status-dot" :class="{ online: device.online, offline: !device.online }"></div>
                <div class="device-info">
                  <span class="device-name">{{ device.deviceName }}</span>
                  <span class="device-last-active">{{ device.lastActive || '--' }}</span>
                </div>
                <span :class="['device-status-tag', device.online ? 'online' : 'offline']">
                  {{ device.online ? '在线' : '离线' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '@/utils/request'

const router = useRouter()

// 仪表板数据
const dashboardData = ref({
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

// 格式化时间
function formatTime(time: string | Date) {
  if (!time) return '--'
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

// 加载仪表板数据
async function loadDashboardData() {
  try {
    const [stats, alerts, devices] = await Promise.allSettled([
      request.get('/dashboard/statistics').catch(() => ({ data: getMockStats() })),
      request.get('/alerts', { params: { page: 1, size: 5 } }).catch(() => ({ data: [] })),
      request.get('/devices', { params: { page: 1, size: 5 } }).catch(() => ({ data: [] }))
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
  renderStatusChart()
  renderTrendChart()
}

function renderStatusChart() {
  if (!statusChart.value) return

  if (!statusChartInstance) {
    statusChartInstance = echarts.init(statusChart.value)
  }

  const statusOption = {
    tooltip: { trigger: 'item' },
    legend: { show: false },
    series: [{
      name: '设备状态',
      type: 'pie',
      radius: ['55%', '75%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#1a1f2e',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 16, fontWeight: 'bold' }
      },
      labelLine: { show: false },
      data: [
        { value: dashboardData.value.onlineDevices, name: '在线', itemStyle: { color: '#10b981' } },
        { value: dashboardData.value.totalDevices - dashboardData.value.onlineDevices, name: '离线', itemStyle: { color: '#4b5563' } }
      ]
    }]
  }

  statusChartInstance.setOption(statusOption)
}

function renderTrendChart() {
  if (!alertTrendChart.value) return

  if (!alertTrendChartInstance) {
    alertTrendChartInstance = echarts.init(alertTrendChart.value)
  }

  const trendOption = {
    tooltip: { trigger: 'axis' },
    grid: {
    left: '2%',
    right: '4%',
    bottom: '8%',
    top: '8%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6b7280' }
  },
  yAxis: {
    type: 'value',
    splitLine: { show: false },
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#6b7280' }
  },
  series: [{
    name: '告警数',
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 8,
    lineStyle: { width: 3, color: '#f59e0b' },
    itemStyle: { color: '#f59e0b' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(245, 158, 11, 0.25)' },
          { offset: 1, color: 'rgba(245, 158, 11, 0.02)' }
        ]
      }
    },
    data: [12, 18, 15, 23, 20, 16, 23]
  }]
}

  alertTrendChartInstance.setOption(trendOption)
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
.monitor-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0f172a;
  padding: 24px;
  gap: 24px;
  overflow: hidden;
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  flex-shrink: 0;
}

.stat-card {
  background: linear-gradient(135deg, #1e293b 0%, #1a1f2e 100%);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon svg {
  width: 32px;
  height: 32px;
}

.stat-card.devices .stat-icon {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(59, 130, 246, 0.1));
  color: #818cf8;
}

.stat-card.online .stat-icon{
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(16, 185, 129, 0.1));
  color: #34d399;
}

.stat-card.alerts .stat-icon{
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(245, 158, 11, 0.1));
  color: #fbbf24;
}

.stat-card.products .stat-icon{
  background: linear-gradient(135deg, rgba(168, 85, 247, 0.2), rgba(168, 85, 247, 0.1));
  color: #c084fc;
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
  font-weight: 500;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-value.online {
  color: #34d399;
}

.stat-value.alert{
  color: #fbbf24;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.stat-trend.up {
  color: #34d399;
}

.stat-trend.up svg {
  width: 16px;
  height: 16px;
}

.stat-trend.warning {
  color: #fbbf24;
}

/* 主内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

/* 图表区域 */
.charts-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-card {
  background: linear-gradient(135deg, #1e293b 0%, #1a1f2e 100%);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.chart-body {
  flex: 1;
  padding: 16px;
  min-height: 0;
}

.chart-container {
  width: 100%;
  height: 100%;
  min-height: 200px;
}

/* 信息区域 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  background: linear-gradient(135deg, #1e293b 0%, #1a1f2e 100%);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.info-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.view-all-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #f1f5f9;
}

.view-all-btn svg {
  width: 16px;
  height: 16px;
}

.info-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: #4b5563;
}

.empty-state svg {
  width: 48px;
  height: 48px;
  opacity: 0.5;
}

.empty-state span {
  font-size: 14px;
}

/* 告警列表 */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  transition: background 0.2s ease;
}

.alert-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.alert-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.alert-indicator.critical {
  background: #ef4444;
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.5);
}

.alert-indicator.warning {
  background: #f59e0b;
  box-shadow: 0 0 12px rgba(245, 158, 11, 0.5);
}

.alert-indicator.info {
  background: #3b82f6;
  box-shadow: 0 0 12px rgba(59, 130, 246, 0.5);
}

.alert-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}

.alert-badge.critical {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.alert-badge.warning {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}

.alert-badge.info {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

.alert-title {
  font-size: 14px;
  color: #f1f5f9;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.alert-device {
  font-size: 12px;
  color: #6b7280;
}

.alert-time {
  font-size: 12px;
  color: #4b5563;
  white-space: nowrap;
}

/* 设备列表 */
.device-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  transition: background 0.2s ease;
}

.device-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.device-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.device-status-dot.online {
  background: #34d399;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.5);
}

.device-status-dot.offline{
  background: #4b5563;
}

.device-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.device-name {
  font-size: 14px;
  color: #f1f5f9;
  font-weight: 500;
}

.device-last-active {
  font-size: 12px;
  color: #6b7280;
}

.device-status-tag {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.device-status-tag.online {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}

.device-status-tag.offline{
  background: rgba(75, 85, 99, 0.2);
  color: #9ca3af;
}

/* 滚动条样式 */
.info-body::-webkit-scrollbar {
  width: 6px;
}

.info-body::-webkit-scrollbar-track {
  background: transparent;
}

.info-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.info-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .monitor-container {
    padding: 16px;
    gap: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 28px;
  }
}
</style>
