<template>
  <div class="parse-rule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">解析规则列表</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="searchForm.ruleType" placeholder="全部" clearable style="width: 140px">
            <el-option label="JSON" value="JSON" />
            <el-option label="二进制" value="BINARY" />
            <el-option label="正则" value="REGEX" />
            <el-option label="JavaScript" value="JAVASCRIPT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="handleSearch">搜索</el-button>
          <el-button class="glass-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button class="glass-button" type="primary" @click="handleAdd">新增规则</el-button>
      </div>

      <!-- 列表 -->
      <el-table :data="rules" v-loading="loading" style="width: 100%">
        <el-table-column prop="ruleName" label="规则名称" min-width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="120">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="getRuleTypeTag(row.ruleType)">
              {{ getRuleTypeText(row.ruleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }">
            <el-button class="glass-button" size="small" @click="handleTest(row)">在线测试</el-button>
            <el-button class="glass-button" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button class="glass-button" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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
          @size-change="loadRules"
          @current-change="loadRules"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑解析规则' : '新增解析规则'"
      width="700px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option label="JSON" value="JSON" />
            <el-option label="二进制" value="BINARY" />
            <el-option label="正则" value="REGEX" />
            <el-option label="JavaScript" value="JAVASCRIPT" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则配置" prop="ruleConfig">
          <el-input
            v-model="formData.ruleConfig"
            type="textarea"
            :rows="10"
            :placeholder="getConfigPlaceholder(formData.ruleType)"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="glass-button" type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 在线测试对话框 -->
    <el-dialog
      v-model="testDialogVisible"
      title="在线测试解析规则"
      width="700px"
    >
      <el-form label-width="100px">
        <el-form-item label="规则名称">
          <el-input :model-value="currentTestRule?.ruleName" disabled />
        </el-form-item>
        <el-form-item label="原始数据">
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="5"
            placeholder="请输入需要解析的原始数据"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="runTest" :loading="testing">
            执行测试
          </el-button>
          <el-button class="glass-button" @click="fillTestSample">填充示例</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">解析结果</el-divider>
      <div v-if="testOutput" class="test-result">
        <pre class="result-pre">{{ testOutput }}</pre>
      </div>
      <div v-else class="test-empty">点击"执行测试"查看解析结果</div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

/** 解析规则接口 */
interface ParseRule {
  id: number
  ruleName: string
  ruleType: string
  ruleConfig: string
  status: string
  description?: string
  createTime: string
}

// 列表数据
const loading = ref(false)
const rules = ref<ParseRule[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  ruleName: '',
  ruleType: ''
})

// 对话框
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 表单数据
const formData = reactive({
  ruleName: '',
  ruleType: 'JSON',
  ruleConfig: '',
  status: '1',
  description: ''
})

// 表单验证规则
const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  ruleConfig: [{ required: true, message: '请输入规则配置', trigger: 'blur' }]
}

// 测试对话框
const testDialogVisible = ref(false)
const currentTestRule = ref<ParseRule | null>(null)
const testInput = ref('')
const testOutput = ref('')
const testing = ref(false)

// 加载列表
async function loadRules() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.ruleName) params.ruleName = searchForm.ruleName
    if (searchForm.ruleType) params.ruleType = searchForm.ruleType

    const data = await request.get('/parse-rules', { params })
    rules.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载解析规则列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadRules()
}

function handleReset() {
  searchForm.ruleName = ''
  searchForm.ruleType = ''
  currentPage.value = 1
  loadRules()
}

function handleAdd() {
  editingId.value = null
  Object.assign(formData, {
    ruleName: '',
    ruleType: 'JSON',
    ruleConfig: '',
    status: '1',
    description: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: ParseRule) {
  editingId.value = row.id
  Object.assign(formData, {
    ruleName: row.ruleName,
    ruleType: row.ruleType,
    ruleConfig: row.ruleConfig,
    status: row.status,
    description: row.description || ''
  })
  dialogVisible.value = true
}

function handleDialogClose() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (editingId.value) {
      await request.put(`/parse-rules/${editingId.value}`, formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/parse-rules', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRules()
  } catch (error: unknown) {
    const err = error as Error
    if (err.message) ElMessage.error(err.message)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ParseRule) {
  try {
    await ElMessageBox.confirm(`确定删除解析规则 ${row.ruleName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/parse-rules/${row.id}`)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 在线测试
function handleTest(row: ParseRule) {
  currentTestRule.value = row
  testInput.value = ''
  testOutput.value = ''
  testDialogVisible.value = true
}

function fillTestSample() {
  if (currentTestRule.value?.ruleType === 'JSON') {
    testInput.value = JSON.stringify({
      temperature: 25.6,
      humidity: 60,
      timestamp: new Date().toISOString()
    }, null, 2)
  } else if (currentTestRule.value?.ruleType === 'BINARY') {
    testInput.value = '0x1A0F3C'
  } else {
    testInput.value = 'temperature=25.6&humidity=60'
  }
}

async function runTest() {
  if (!testInput.value.trim()) {
    ElMessage.warning('请输入原始数据')
    return
  }
  try {
    testing.value = true
    testOutput.value = ''
    const data = await request.post('/parse-rules/test', {
      ruleId: currentTestRule.value?.id,
      rawData: testInput.value
    })
    testOutput.value = JSON.stringify(data, null, 2)
  } catch (error: unknown) {
    const err = error as Error
    testOutput.value = `Error: ${err.message || '测试执行失败'}`
  } finally {
    testing.value = false
  }
}

/** 获取规则类型显示文字 */
function getRuleTypeText(type: string): string {
  const map: Record<string, string> = {
    JSON: 'JSON',
    BINARY: '二进制',
    REGEX: '正则',
    JAVASCRIPT: 'JavaScript'
  }
  return map[type] || type
}

/** 获取规则类型标签颜色 */
function getRuleTypeTag(type: string): string {
  const map: Record<string, string> = {
    JSON: '',
    BINARY: 'warning',
    REGEX: 'info',
    JAVASCRIPT: 'success'
  }
  return map[type] || ''
}

/** 根据规则类型返回配置占位提示 */
function getConfigPlaceholder(type: string): string {
  const map: Record<string, string> = {
    JSON: '{\n  "temperaturePath": "$.data.temperature",\n  "humidityPath": "$.data.humidity"\n}',
    BINARY: '// 二进制解析规则\n// offset:0, length:2, type:int16 -> temperature\n// offset:2, length:1, type:uint8 -> humidity',
    REGEX: '// 正则解析规则\ntemperature=(\\d+\\.?\\d*)&humidity=(\\d+)',
    JAVASCRIPT: 'function parse(payload, metadata) {\n  var data = JSON.parse(payload);\n  return {\n    temperature: data.temp / 10.0,\n    humidity: data.hum\n  };\n}'
  }
  return map[type] || '请输入规则配置'
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
/* 页面容器 */
.parse-rule-page {
  padding: 24px;
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

/* 卡片标题 */
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
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

/* 分页容器 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  flex-shrink: 0;
}

/* 测试结果 */
.test-result {
  min-height: 80px;
}

.test-empty {
  text-align: center;
  color: #64748b;
  padding: 24px 0;
}

.result-pre {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  font-family: monospace;
  color: #94a3b8;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
