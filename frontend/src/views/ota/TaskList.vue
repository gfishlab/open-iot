<template>
  <div class="ota-task-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">OTA 升级任务</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="任务状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待执行" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="已暂停" value="paused" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="handleSearch">搜索</el-button>
          <el-button class="glass-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button class="glass-button" type="primary" @click="handleCreate">创建升级任务</el-button>
        <el-button class="glass-button" @click="loadTasks">刷新</el-button>
      </div>

      <!-- 任务列表 -->
      <el-table :data="tasks" v-loading="loading" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" min-width="200" />
        <el-table-column prop="firmwareVersion" label="目标版本" width="120">
          <template #default="{ row }">
            <el-tag class="glass-tag" type="primary">{{ row.firmwareVersion }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" width="180" />
        <el-table-column prop="upgradeScope" label="升级范围" width="100">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="row.upgradeScope === 'all' ? 'warning' : 'info'">
              {{ row.upgradeScope === 'all' ? '全部设备' : '指定设备' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviceCount" label="设备数量" width="100">
          <template #default="{ row }">{{ row.deviceCount || 0 }} 台</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="getProgress(row)"
              :status="getProgressStatus(row.status)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button class="glass-button" size="small" @click="handleViewDevices(row)">设备状态</el-button>
            <el-button
              v-if="row.status === 'pending' || row.status === 'paused'"
              class="glass-button"
              size="small"
              type="primary"
              @click="handleStart(row)"
            >
              开始
            </el-button>
            <el-button
              v-if="row.status === 'running'"
              class="glass-button"
              size="small"
              type="warning"
              @click="handlePause(row)"
            >
              暂停
            </el-button>
            <el-button
              v-if="row.status !== 'completed' && row.status !== 'cancelled'"
              class="glass-button"
              size="small"
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
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
          class="glass-pagination"
          @size-change="loadTasks"
          @current-change="loadTasks"
        />
      </div>
    </el-card>

    <!-- 创建升级任务对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建 OTA 升级任务" width="700px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="目标产品" prop="productId">
          <el-select v-model="createForm.productId" placeholder="请选择产品" style="width: 100%" @change="handleProductChange">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标固件" prop="firmwareVersionId">
          <el-select v-model="createForm.firmwareVersionId" placeholder="请选择固件版本" style="width: 100%" :disabled="!createForm.productId">
            <el-option
              v-for="firmware in availableFirmwares"
              :key="firmware.id"
              :label="`${firmware.firmwareName} - ${firmware.version}`"
              :value="firmware.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="升级范围" prop="upgradeScope">
          <el-radio-group v-model="createForm.upgradeScope">
            <el-radio value="all">全部设备</el-radio>
            <el-radio value="dynamic">动态分组</el-radio>
            <el-radio value="specific">指定设备</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.upgradeScope === 'specific'" label="选择设备" prop="deviceIds">
          <el-select
            v-model="createForm.deviceIds"
            placeholder="请选择设备"
            multiple
            style="width: 100%"
            :disabled="!createForm.productId"
          >
            <el-option
              v-for="device in availableDevices"
              :key="device.id"
              :label="device.deviceName"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="升级策略">
          <el-select v-model="createForm.strategy" placeholder="请选择升级策略" style="width: 100%">
            <el-option label="立即升级" value="immediate" />
            <el-option label="定时升级" value="scheduled" />
            <el-option label="静默升级" value="silent" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.strategy === 'scheduled'" label="计划时间" prop="scheduleTime">
          <el-date-picker
            v-model="createForm.scheduleTime"
            type="datetime"
            placeholder="选择执行时间"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 设备状态对话框 -->
    <el-dialog v-model="devicesDialogVisible" title="设备升级状态" width="900px">
      <div class="device-stats">
        <div class="stat-item">
          <span class="stat-label">总数</span>
          <span class="stat-value">{{ deviceStats.total }}</span>
        </div>
        <div class="stat-item stat-item--success">
          <span class="stat-label">成功</span>
          <span class="stat-value">{{ deviceStats.success }}</span>
        </div>
        <div class="stat-item stat-item--running">
          <span class="stat-label">升级中</span>
          <span class="stat-value">{{ deviceStats.running }}</span>
        </div>
        <div class="stat-item stat-item--failed">
          <span class="stat-label">失败</span>
          <span class="stat-value">{{ deviceStats.failed }}</span>
        </div>
        <div class="stat-item stat-item--pending">
          <span class="stat-label">待升级</span>
          <span class="stat-value">{{ deviceStats.pending }}</span>
        </div>
      </div>
      <el-table :data="deviceStatuses" v-loading="devicesLoading" size="small" max-height="400" style="margin-top: 16px">
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="currentVersion" label="当前版本" width="120" />
        <el-table-column prop="targetVersion" label="目标版本" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="getDeviceStatusType(row.status)" size="small">
              {{ getDeviceStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="{ row }">
            <span v-if="row.status === 'completed'">100%</span>
            <span v-else-if="row.status === 'pending'">0%</span>
            <span v-else-if="row.progress !== undefined">{{ row.progress }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.errorMsg" style="color: #f87171">{{ row.errorMsg }}</span>
            <span v-else style="color: #64748b">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="deviceCurrentPage"
          v-model:page-size="devicePageSize"
          :total="deviceTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadDeviceStatuses"
          @current-change="loadDeviceStatuses"
        />
      </div>
      <template #footer>
        <el-button @click="devicesDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '@/utils/request'
import * as otaApi from '@/api/ota'
import * as productApi from '@/api/product'
import * as deviceApi from '@/api/device'
import type { OtaTask, OtaTaskCreateVO, Product, Firmware, Device, DeviceStatus } from '@/api/ota'

// 列表数据
const loading = ref(false)
const tasks = ref<OtaTask[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 产品和固件列表
const products = ref<Product[]>([])
const availableFirmwares = ref<Firmware[]>([])
const availableDevices = ref<Device[]>([])

// 搜索表单
const searchForm = reactive({
  status: ''
})

// 创建任务对话框
const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()

// 创建表单
const createForm = reactive({
  taskName: '',
  productId: undefined as number | undefined,
  firmwareVersionId: undefined as number | undefined,
  upgradeScope: 'all',
  deviceIds: [] as number[],
  strategy: 'immediate',
  scheduleTime: null as Date | null,
  description: ''
})

// 创建表单验证规则
const createRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  firmwareVersionId: [{ required: true, message: '请选择固件版本', trigger: 'change' }],
  upgradeScope: [{ required: true, message: '请选择升级范围', trigger: 'change' }],
  deviceIds: [
    {
      validator: (rule, value, callback) => {
        if (createForm.upgradeScope === 'specific' && (!value || value.length === 0)) {
          callback(new Error('请选择设备'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 设备状态对话框
const devicesDialogVisible = ref(false)
const devicesLoading = ref(false)
const deviceStatuses = ref<DeviceStatus[]>([])
const deviceCurrentPage = ref(1)
const devicePageSize = ref(10)
const deviceTotal = ref(0)
const currentTaskId = ref<number | null>(null)
const deviceStats = ref({
  total: 0,
  success: 0,
  running: 0,
  failed: 0,
  pending: 0
})

// 加载任务列表
async function loadTasks() {
  try {
    loading.value = true
    const data = await otaApi.getTaskList({
      page: currentPage.value,
      size: pageSize.value,
      status: searchForm.status || undefined
    })
    tasks.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

// 加载产品列表
async function loadProducts() {
  try {
    const data = await productApi.getProductList({ pageNum: 1, pageSize: 1000 })
    products.value = (data.records || data.list || []).map((p: any) => ({
      id: p.id,
      name: p.productName || p.name
    }))
  } catch (error) {
    console.error('加载产品列表失败', error)
  }
}

// 加载固件列表
async function loadFirmwares(productId: number) {
  try {
    const data = await otaApi.getFirmwareList({ productId, page: 1, size: 1000 })
    availableFirmwares.value = data.records || data.list || []
  } catch (error) {
    console.error('加载固件列表失败', error)
  }
}

// 加载设备列表
async function loadDevices(productId: number) {
  try {
    const data = await deviceApi.getDeviceList({ productId, page: 1, size: 1000 })
    availableDevices.value = data.records || data.list || []
  } catch (error) {
    console.error('加载设备列表失败', error)
  }
}

// 产品变化时加载对应固件和设备
function handleProductChange() {
  createForm.firmwareVersionId = undefined
  createForm.deviceIds = []
  if (createForm.productId) {
    loadFirmwares(createForm.productId)
    loadDevices(createForm.productId)
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadTasks()
}

// 重置
function handleReset() {
  searchForm.status = ''
  currentPage.value = 1
  loadTasks()
}

// 打开创建对话框
function handleCreate() {
  Object.assign(createForm, {
    taskName: '',
    productId: undefined,
    firmwareVersionId: undefined,
    upgradeScope: 'all',
    deviceIds: [],
    strategy: 'immediate',
    scheduleTime: null,
    description: ''
  })
  createDialogVisible.value = true
}

// 禁用过去的日期
function disabledDate(time: Date) {
  return time.getTime() < Date.now() - 86400000
}

// 确认创建
async function confirmCreate() {
  try {
    await createFormRef.value?.validate()
    creating.value = true

    const payload: any = {
      taskName: createForm.taskName,
      productId: createForm.productId,
      firmwareVersionId: createForm.firmwareVersionId,
      upgradeScope: createForm.upgradeScope,
      strategy: createForm.strategy,
      description: createForm.description
    }

    if (createForm.upgradeScope === 'specific') {
      payload.deviceIds = createForm.deviceIds
    }

    if (createForm.strategy === 'scheduled' && createForm.scheduleTime) {
      payload.scheduleTime = createForm.scheduleTime.toISOString()
    }

    await otaApi.createTask(payload as OtaTaskCreateVO)
    ElMessage.success('升级任务创建成功')
    createDialogVisible.value = false
    loadTasks()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || '创建失败')
    }
  } finally {
    creating.value = false
  }
}

// 开始任务
async function handleStart(row: OtaTask) {
  try {
    await ElMessageBox.confirm(`确定开始执行任务 ${row.taskName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await otaApi.startTask(row.id)
    ElMessage.success('任务已开始执行')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 暂停任务
async function handlePause(row: OtaTask) {
  try {
    await ElMessageBox.confirm(`确定暂停任务 ${row.taskName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await otaApi.pauseTask(row.id)
    ElMessage.success('任务已暂停')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 取消任务
async function handleCancel(row: OtaTask) {
  try {
    await ElMessageBox.confirm(`确定取消任务 ${row.taskName} 吗？取消后无法恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await otaApi.cancelTask(row.id)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 查看设备状态
function handleViewDevices(row: OtaTask) {
  currentTaskId.value = row.id
  deviceCurrentPage.value = 1
  deviceStatuses.value = []
  devicesDialogVisible.value = true
  loadDeviceStatuses()
}

// 加载设备状态
async function loadDeviceStatuses() {
  if (!currentTaskId.value) return
  try {
    devicesLoading.value = true
    const data = await otaApi.getTaskDeviceStatuses(currentTaskId.value, {
      page: deviceCurrentPage.value,
      size: devicePageSize.value
    })
    deviceStatuses.value = data.records || data.list || []
    deviceTotal.value = data.total || 0

    // 统计状态
    const stats = { total: data.total || 0, success: 0, running: 0, failed: 0, pending: 0 }
    deviceStatuses.value.forEach((d: DeviceStatus) => {
      if (d.status === 'completed') stats.success++
      else if (d.status === 'running') stats.running++
      else if (d.status === 'failed') stats.failed++
      else if (d.status === 'pending') stats.pending++
    })
    deviceStats.value = stats
  } catch (error) {
    ElMessage.error('加载设备状态失败')
  } finally {
    devicesLoading.value = false
  }
}

// 获取状态类型
function getStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    running: 'warning',
    paused: 'warning',
    completed: 'success',
    cancelled: 'danger'
  }
  return map[status] || ''
}

// 获取状态文本
function getStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待执行',
    running: '执行中',
    paused: '已暂停',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

// 获取进度
function getProgress(row: OtaTask): number {
  if (row.status === 'completed') return 100
  if (row.status === 'pending') return 0
  return row.progress || 0
}

// 获取进度状态
function getProgressStatus(status: string): 'success' | 'exception' | undefined {
  if (status === 'completed') return 'success'
  if (status === 'cancelled') return 'exception'
  return undefined
}

// 获取设备状态类型
function getDeviceStatusType(status: string) {
  const map: Record<string, string> = {
    pending: 'info',
    downloading: 'warning',
    installing: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || ''
}

// 获取设备状态文本
function getDeviceStatusText(status: string) {
  const map: Record<string, string> = {
    pending: '待升级',
    downloading: '下载中',
    installing: '安装中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

onMounted(() => {
  loadTasks()
  loadProducts()
})
</script>

<style scoped>
/* 页面容器 */
.ota-task-list-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.el-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.el-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.el-table {
  flex: 1;
  min-height: 400px;
}

/* 卡片标题行 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

/* 操作栏 */
.action-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

/* 分页容器 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  flex-shrink: 0;
}

/* 设备统计 */
.device-stats {
  display: flex;
  gap: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
}

.stat-item--success .stat-value { color: #34d399; }
.stat-item--running .stat-value { color: #fbbf24; }
.stat-item--failed .stat-value { color: #f87171; }
.stat-item--pending .stat-value { color: #94a3b8; }
</style>
