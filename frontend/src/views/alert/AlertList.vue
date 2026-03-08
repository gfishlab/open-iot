<template>
  <div class="alert-page">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--blue">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">总告警数</div>
            <div class="stat-value">{{ statistics.totalCount || 0 }}</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--yellow">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">待处理</div>
            <div class="stat-value stat-value--yellow">{{ statistics.pendingCount || 0 }}</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--green">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">已处理</div>
            <div class="stat-value stat-value--green">{{ statistics.resolvedCount || 0 }}</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--red">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">严重告警</div>
            <div class="stat-value stat-value--red">{{ statistics.criticalCount || 0 }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 告警列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警列表</span>
          <div class="header-actions">
            <el-button size="small" @click="handleBatchHandle" :disabled="selectedIds.length === 0">
              批量处理
            </el-button>
            <el-button size="small" type="primary" @click="loadAlerts">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="告警级别">
          <el-select v-model="searchForm.level" placeholder="全部" clearable style="width: 128px">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="信息" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 128px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已忽略" value="ignored" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="searchForm.deviceName" placeholder="设备名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        :data="alerts"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="alertTitle" label="告警标题" min-width="200" />
        <el-table-column prop="alertLevel" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.alertLevel)" size="small">
              {{ getLevelText(row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alertTime" label="告警时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending' || row.status === 'processing'"
              size="small"
              type="success"
              @click="handleResolve(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              size="small"
              @click="handleIgnore(row)"
            >
              忽略
            </el-button>
            <el-button size="small" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAlerts"
          @current-change="loadAlerts"
        />
      </div>
    </el-card>

    <!-- 处理告警对话框 -->
    <el-dialog v-model="handleDialogVisible" title="处理告警" width="500px">
      <el-form :model="handleForm" label-width="100px">
        <el-form-item label="处理状态">
          <el-radio-group v-model="handleForm.status">
            <el-radio label="resolved">已解决</el-radio>
            <el-radio label="ignored">已忽略</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle" :loading="handleLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 类型定义
interface Alert {
  id: number
  alertTitle: string
  alertLevel: string
  deviceId: number
  deviceName: string
  status: string
  alertTime: string
  alertContent: string
}

// 列表数据
const loading = ref(false)
const alerts = ref<Alert[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableRef = ref()
const selectedIds = ref<number[]>([])

// 统计数据
const statistics = ref<any>({})

// 搜索表单
const searchForm = reactive({
  level: '',
  status: '',
  deviceName: ''
})

// 处理对话框
const handleDialogVisible = ref(false)
const handleLoading = ref(false)
const currentAlert = ref<Alert | null>(null)
const handleForm = reactive({
  status: 'resolved',
  remark: ''
})

// 加载告警列表
async function loadAlerts() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.level) params.level = searchForm.level
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.deviceName) params.deviceName = searchForm.deviceName

    const data = await request.get('/alerts', { params })
    alerts.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载告警列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
async function loadStatistics() {
  try {
    const data = await request.get('/alerts/statistics')
    statistics.value = data || {}
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadAlerts()
}

// 重置
function handleReset() {
  searchForm.level = ''
  searchForm.status = ''
  searchForm.deviceName = ''
  currentPage.value = 1
  loadAlerts()
}

// 选择变化
function handleSelectionChange(selection: Alert[]) {
  selectedIds.value = selection.map(item => item.id)
}

// 获取级别类型
function getLevelType(level: string) {
  const map: Record<string, string> = {
    critical: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return map[level] || ''
}

// 获取级别文本
function getLevelText(level: string) {
  const map: Record<string, string> = {
    critical: '严重',
    warning: '警告',
    info: '信息'
  }
  return map[level] || level
}

// 获取状态类型
function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'warning',
    processing: 'primary',
    resolved: 'success',
    ignored: 'info'
  }
  return map[status] || ''
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    resolved: '已解决',
    ignored: '已忽略'
  }
  return map[status] || status
}

// 处理告警
function handleResolve(row: Alert) {
  currentAlert.value = row
  handleForm.status = 'resolved'
  handleForm.remark = ''
  handleDialogVisible.value = true
}

// 忽略告警
async function handleIgnore(row: Alert) {
  try {
    await ElMessageBox.confirm('确定忽略此告警吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.put(`/alerts/${row.id}/handle`, {
      status: 'ignored'
    })
    ElMessage.success('操作成功')
    loadAlerts()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 批量处理
function handleBatchHandle() {
  if (selectedIds.value.length === 0) return
  currentAlert.value = null
  handleForm.status = 'resolved'
  handleForm.remark = ''
  handleDialogVisible.value = true
}

// 查看详情
function handleViewDetail(row: Alert) {
  ElMessage.info(`告警内容: ${row.alertContent}`)
}

// 确认处理
async function confirmHandle() {
  try {
    handleLoading.value = true
    if (currentAlert.value) {
      // 单个处理
      await request.put(`/alerts/${currentAlert.value.id}/handle`, handleForm)
    } else {
      // 批量处理
      await request.put('/alerts/batch-handle', {
        alertIds: selectedIds.value,
        ...handleForm
      })
    }
    ElMessage.success('操作成功')
    handleDialogVisible.value = false
    loadAlerts()
    loadStatistics()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    handleLoading.value = false
  }
}

onMounted(() => {
  loadAlerts()
  loadStatistics()
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.alert-page {
  padding: 20px;
}

/* ===== 统计卡片行 ===== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  border: none;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 图标容器 */
.stat-icon {
  width: 48px;
  height: 48px;
  min-width: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

/* 四种颜色变体 */
.stat-icon--blue {
  background: rgba(99, 102, 241, 0.15);
  color: #818cf8;
}

.stat-icon--yellow {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.stat-icon--green {
  background: rgba(52, 211, 153, 0.15);
  color: #34d399;
}

.stat-icon--red {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}

.stat-text {
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1;
}

.stat-value--yellow { color: #fbbf24; }
.stat-value--green  { color: #34d399; }
.stat-value--red    { color: #f87171; }

/* ===== 告警列表 card header ===== */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* ===== 搜索表单 ===== */
.search-form {
  margin-bottom: 16px;
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
