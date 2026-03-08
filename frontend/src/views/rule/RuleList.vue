<template>
  <div class="rule-page">
    <!-- 顶部统计卡片 -->
    <div class="stat-row">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--blue">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">总规则数</div>
            <div class="stat-value">{{ statistics.totalCount || 0 }}</div>
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
            <div class="stat-label">已启用</div>
            <div class="stat-value stat-value--green">{{ statistics.enabledCount || 0 }}</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--gray">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">已禁用</div>
            <div class="stat-value stat-value--gray">{{ statistics.disabledCount || 0 }}</div>
          </div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon stat-icon--purple">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <div class="stat-text">
            <div class="stat-label">今日触发</div>
            <div class="stat-value stat-value--purple">{{ statistics.todayTriggerCount || 0 }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 规则列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>规则列表</span>
          <div class="header-actions">
            <el-button size="small" @click="handleBatchDelete" :disabled="selectedIds.length === 0">
              批量删除
            </el-button>
            <el-button size="small" type="primary" @click="handleCreate">
              新增规则
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="searchForm.ruleType" placeholder="全部" clearable style="width: 128px">
            <el-option label="表达式" value="EXPRESSION" />
            <el-option label="脚本" value="SCRIPT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 128px">
            <el-option label="已启用" value="1" />
            <el-option label="已禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        :data="rules"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        style="width: 100%"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="ruleCode" label="规则编码" width="150" />
        <el-table-column prop="ruleType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.ruleType === 'EXPRESSION' ? 'primary' : 'success'" size="small">
              {{ row.ruleType === 'EXPRESSION' ? '表达式' : '脚本' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="关联产品" width="120">
          <template #default="{ row }">{{ row.productName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="deviceName" label="关联设备" width="120">
          <template #default="{ row }">{{ row.deviceName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="alertLevel" label="告警级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlertLevelType(row.alertLevel)" size="small">
              {{ getAlertLevelText(row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="'1'"
              :inactive-value="'0'"
              @change="handleStatusChange(row)"
              :loading="row.statusChanging"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column label="操作" fixed="right" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="handleTest(row)">测试</el-button>
            <el-button size="small" @click="handleViewLogs(row)">日志</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
          @size-change="loadRules"
          @current-change="loadRules"
        />
      </div>
    </el-card>

    <!-- 新增/编辑规则对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <el-form :model="ruleForm" :rules="formRules" ref="formRef" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input
                v-model="ruleForm.ruleCode"
                placeholder="请输入规则编码"
                :disabled="!!ruleForm.id"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规则类型" prop="ruleType">
              <el-select v-model="ruleForm.ruleType" style="width: 100%" @change="handleRuleTypeChange">
                <el-option label="表达式规则" value="EXPRESSION" />
                <el-option label="脚本规则" value="SCRIPT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="告警级别" prop="alertLevel">
              <el-select v-model="ruleForm.alertLevel" style="width: 100%">
                <el-option label="信息" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="严重" value="critical" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="关联产品" prop="productId">
          <el-select
            v-model="ruleForm.productId"
            placeholder="请选择产品（可选）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关联设备" prop="deviceId">
          <el-select
            v-model="ruleForm.deviceId"
            placeholder="请选择设备（可选）"
            clearable
            filterable
            style="width: 100%"
            :disabled="!ruleForm.productId"
          >
            <el-option
              v-for="device in devices"
              :key="device.id"
              :label="device.deviceName"
              :value="device.id"
            />
          </el-select>
        </el-form-item>

        <!-- 表达式规则 -->
        <template v-if="ruleForm.ruleType === 'EXPRESSION'">
          <el-form-item label="触发条件" prop="expression">
            <div style="width: 100%">
              <el-input
                v-model="ruleForm.expression"
                type="textarea"
                :rows="3"
                placeholder='Aviator 表达式，如：temperature > 50 && humidity < 20'
              />
              <div class="form-hint">
                支持的数据字段：设备属性（temperature, humidity, pressure 等）
              </div>
            </div>
          </el-form-item>
        </template>

        <!-- 脚本规则 -->
        <template v-if="ruleForm.ruleType === 'SCRIPT'">
          <el-form-item label="脚本内容" prop="scriptContent">
            <div style="width: 100%">
              <el-input
                v-model="ruleForm.scriptContent"
                type="textarea"
                :rows="8"
                placeholder="// JavaScript 脚本
// 可用变量：data（设备数据）、logger（日志记录器）
// 返回值：{ matched: true/false, alertLevel: 'info/warning/critical', message: '告警消息' }

function execute(data) {
  if (data.temperature > 50) {
    return {
      matched: true,
      alertLevel: 'critical',
      message: '温度过高: ' + data.temperature + '℃'
    };
  }
  return { matched: false };
}"
              />
              <div class="form-hint">
                使用 GraalJS 执行 JavaScript 脚本，返回匹配结果和告警信息
              </div>
            </div>
          </el-form-item>
        </template>

        <el-form-item label="告警模板" prop="alertTemplate">
          <el-input
            v-model="ruleForm.alertTemplate"
            placeholder="告警消息模板，如：设备 {deviceName} 温度过高：{temperature}℃"
          />
          <div class="form-hint">
            可用变量：{deviceName}、{propertyValue}、{threshold} 等
          </div>
        </el-form-item>

        <el-form-item label="规则描述">
          <el-input
            v-model="ruleForm.description"
            type="textarea"
            :rows="2"
            placeholder="请输入规则描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试规则对话框 -->
    <el-dialog v-model="testDialogVisible" title="测试规则" width="700px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="模拟数据">
          <el-input
            v-model="testForm.deviceData"
            type="textarea"
            :rows="6"
            placeholder='请输入 JSON 格式的设备数据，如：
{
  "temperature": 55,
  "humidity": 15,
  "pressure": 1013
}'
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runTest" :loading="testing">执行测试</el-button>
          <el-button @click="fillSampleData">填充示例数据</el-button>
        </el-form-item>
      </el-form>

      <!-- 测试结果 -->
      <el-divider>测试结果</el-divider>
      <div v-if="testResult" class="test-result">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="匹配状态">
            <el-tag :type="testResult.matched ? 'danger' : 'success'">
              {{ testResult.matched ? '触发告警' : '未触发' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">
            {{ testResult.executionTime }} ms
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="testResult.result" class="result-block">
          <div class="result-label">返回结果：</div>
          <pre class="result-pre">{{ JSON.stringify(testResult.result, null, 2) }}</pre>
        </div>

        <div v-if="testResult.error" class="result-block">
          <div class="result-label result-label--error">错误信息：</div>
          <pre class="result-pre result-pre--error">{{ testResult.error }}</pre>
        </div>
      </div>
      <div v-else class="test-empty">
        点击"执行测试"查看结果
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志对话框 -->
    <el-dialog v-model="logsDialogVisible" title="规则执行日志" width="900px">
      <el-table :data="logs" v-loading="logsLoading" size="small" max-height="400">
        <el-table-column prop="deviceName" label="设备" width="120" />
        <el-table-column prop="matched" label="匹配结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.matched ? 'danger' : 'success'" size="small">
              {{ row.matched ? '触发' : '未触发' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executionTime" label="执行耗时(ms)" width="100" />
        <el-table-column prop="errorMsg" label="错误信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.errorMsg" style="color: #f87171">{{ row.errorMsg }}</span>
            <span v-else style="color: #64748b">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="执行时间" width="160" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="logsCurrentPage"
          v-model:page-size="logsPageSize"
          :total="logsTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
      <template #footer>
        <el-button @click="logsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as ruleApi from '@/api/rule'
import type { Rule, RuleTestRequest, RuleTestResponse } from '@/api/rule'

// 列表数据
const loading = ref(false)
const rules = ref<Rule[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableRef = ref()
const selectedIds = ref<number[]>([])

// 统计数据
const statistics = ref<any>({})

// 搜索表单
const searchForm = reactive({
  ruleName: '',
  ruleType: '',
  status: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = computed(() => ruleForm.id ? '编辑规则' : '新增规则')
const saving = ref(false)
const formRef = ref<FormInstance>()

// 规则表单
const ruleForm = reactive<Partial<Rule>>({
  ruleName: '',
  ruleCode: '',
  ruleType: 'EXPRESSION',
  expression: '',
  scriptContent: '',
  productId: undefined,
  deviceId: undefined,
  alertLevel: 'warning',
  alertTemplate: '',
  status: '1',
  description: ''
})

// 表单验证规则
const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleCode: [
    { required: true, message: '请输入规则编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '只能包含字母、数字、下划线和横线', trigger: 'blur' }
  ],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  alertLevel: [{ required: true, message: '请选择告警级别', trigger: 'change' }],
  expression: [
    {
      validator: (rule, value, callback) => {
        if (ruleForm.ruleType === 'EXPRESSION' && !value) {
          callback(new Error('请输入触发条件表达式'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  scriptContent: [
    {
      validator: (rule, value, callback) => {
        if (ruleForm.ruleType === 'SCRIPT' && !value) {
          callback(new Error('请输入脚本内容'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  alertTemplate: [{ required: true, message: '请输入告警模板', trigger: 'blur' }]
}

// 产品和设备列表
const products = ref<any[]>([])
const devices = ref<any[]>([])

// 测试对话框
const testDialogVisible = ref(false)
const testing = ref(false)
const currentTestRule = ref<Rule | null>(null)
const testForm = reactive({ deviceData: '' })
const testResult = ref<RuleTestResponse | null>(null)

// 日志对话框
const logsDialogVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<any[]>([])
const logsCurrentPage = ref(1)
const logsPageSize = ref(10)
const logsTotal = ref(0)
const currentLogRuleId = ref<number | null>(null)

// 加载规则列表
async function loadRules() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.ruleName) params.ruleName = searchForm.ruleName
    if (searchForm.ruleType) params.ruleType = searchForm.ruleType
    if (searchForm.status) params.status = searchForm.status

    const data = await ruleApi.getRuleList(params as any)
    rules.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载规则列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
function loadStatistics() {
  statistics.value = {
    totalCount: rules.value.length,
    enabledCount: rules.value.filter(r => r.status === '1').length,
    disabledCount: rules.value.filter(r => r.status === '0').length,
    todayTriggerCount: 0
  }
}

// 加载产品列表（模拟数据）
function loadProducts() {
  products.value = [
    { id: 1, name: '温湿度传感器' },
    { id: 2, name: '智能网关' },
    { id: 3, name: '烟雾报警器' }
  ]
}

// 加载设备列表（模拟数据）
function loadDevices(productId?: number) {
  if (productId) {
    devices.value = [
      { id: 1, deviceName: '设备001', productId: 1 },
      { id: 2, deviceName: '设备002', productId: 1 },
      { id: 3, deviceName: '网关001', productId: 2 }
    ]
  } else {
    devices.value = []
  }
}

function handleSearch() {
  currentPage.value = 1
  loadRules()
}

function handleReset() {
  searchForm.ruleName = ''
  searchForm.ruleType = ''
  searchForm.status = ''
  currentPage.value = 1
  loadRules()
}

function handleSelectionChange(selection: Rule[]) {
  selectedIds.value = selection.map(item => item.id)
}

function handleCreate() {
  Object.assign(ruleForm, {
    id: undefined,
    ruleName: '',
    ruleCode: '',
    ruleType: 'EXPRESSION',
    expression: '',
    scriptContent: '',
    productId: undefined,
    deviceId: undefined,
    alertLevel: 'warning',
    alertTemplate: '',
    status: '1',
    description: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: Rule) {
  Object.assign(ruleForm, { ...row })
  if (row.productId) loadDevices(row.productId)
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true
    const data = { ...ruleForm }
    delete (data as any).id
    delete (data as any).createTime
    delete (data as any).updateTime
    if (ruleForm.id) {
      await ruleApi.updateRule(ruleForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await ruleApi.createRule(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRules()
    loadStatistics()
  } catch (error: any) {
    if (error !== false) ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleDialogClose() {
  formRef.value?.resetFields()
  devices.value = []
}

async function handleDelete(row: Rule) {
  try {
    await ElMessageBox.confirm(`确定删除规则 ${row.ruleName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await ruleApi.deleteRule(row.id)
    ElMessage.success('删除成功')
    loadRules()
    loadStatistics()
  } catch {}
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 条规则吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await ruleApi.batchDeleteRules(selectedIds.value)
    ElMessage.success('删除成功')
    loadRules()
    loadStatistics()
  } catch {}
}

async function handleStatusChange(row: Rule) {
  try {
    (row as any).statusChanging = true
    if (row.status === '1') {
      await ruleApi.enableRule(row.id)
      ElMessage.success('规则已启用')
    } else {
      await ruleApi.disableRule(row.id)
      ElMessage.success('规则已禁用')
    }
    loadStatistics()
  } catch {
    row.status = row.status === '1' ? '0' : '1'
    ElMessage.error('操作失败')
  } finally {
    (row as any).statusChanging = false
  }
}

function handleRuleTypeChange() {
  if (ruleForm.ruleType === 'EXPRESSION') {
    ruleForm.scriptContent = ''
  } else {
    ruleForm.expression = ''
  }
}

function getAlertLevelType(level: string) {
  return { critical: 'danger', warning: 'warning', info: 'info' }[level] || ''
}

function getAlertLevelText(level: string) {
  return { critical: '严重', warning: '警告', info: '信息' }[level] || level
}

function handleTest(row: Rule) {
  currentTestRule.value = row
  testForm.deviceData = ''
  testResult.value = null
  testDialogVisible.value = true
}

function fillSampleData() {
  testForm.deviceData = JSON.stringify({
    temperature: 55, humidity: 15, pressure: 1013,
    timestamp: new Date().toISOString()
  }, null, 2)
}

async function runTest() {
  if (!currentTestRule.value) return
  let deviceData: Record<string, any>
  try {
    deviceData = JSON.parse(testForm.deviceData)
  } catch {
    ElMessage.error('设备数据格式错误，请使用 JSON 格式')
    return
  }
  try {
    testing.value = true
    testResult.value = null
    const req: RuleTestRequest = {
      ruleId: currentTestRule.value.id,
      ruleType: currentTestRule.value.ruleType,
      expression: currentTestRule.value.expression,
      scriptContent: currentTestRule.value.scriptContent,
      deviceData
    }
    testResult.value = await ruleApi.testRule(req)
  } catch {
    ElMessage.error('测试执行失败')
  } finally {
    testing.value = false
  }
}

function handleViewLogs(row: Rule) {
  currentLogRuleId.value = row.id
  logsCurrentPage.value = 1
  logs.value = []
  logsDialogVisible.value = true
  loadLogs()
}

async function loadLogs() {
  if (!currentLogRuleId.value) return
  try {
    logsLoading.value = true
    const data = await ruleApi.getRuleLogs({
      page: logsCurrentPage.value,
      size: logsPageSize.value,
      ruleId: currentLogRuleId.value
    })
    logs.value = data.records || []
    logsTotal.value = data.total || 0
  } catch {
    ElMessage.error('加载日志失败')
  } finally {
    logsLoading.value = false
  }
}

onMounted(() => {
  loadRules()
  loadProducts()
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.rule-page {
  padding: 20px;
}

/* ===== 统计卡片行 ===== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card { border: none; }

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

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

.stat-icon--blue   { background: rgba(99, 102, 241, 0.15); color: #818cf8; }
.stat-icon--green  { background: rgba(52, 211, 153, 0.15); color: #34d399; }
.stat-icon--gray   { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
.stat-icon--purple { background: rgba(167, 139, 250, 0.15); color: #a78bfa; }

.stat-text { flex: 1; }

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

.stat-value--green  { color: #34d399; }
.stat-value--gray   { color: #94a3b8; }
.stat-value--purple { color: #a78bfa; }

/* ===== Card header ===== */
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

/* ===== 表单提示 ===== */
.form-hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

/* ===== 测试结果 ===== */
.test-result { min-height: 100px; }

.test-empty {
  text-align: center;
  color: #64748b;
  padding: 32px 0;
}

.result-block { margin-top: 16px; }

.result-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  margin-bottom: 8px;
}

.result-label--error { color: #f87171; }

.result-pre {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  color: #94a3b8;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.result-pre--error {
  background: rgba(248, 113, 113, 0.08);
  border-color: rgba(248, 113, 113, 0.2);
  color: #f87171;
}
</style>
