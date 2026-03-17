<template>
  <div class="mapping-rule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">映射规则列表</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="规则名称">
          <el-input v-model="searchForm.ruleName" placeholder="请输入规则名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="关联产品">
          <el-select v-model="searchForm.productId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option
              v-for="item in productList"
              :key="item.id"
              :label="item.productName"
              :value="item.id"
            />
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
        <el-table-column prop="productName" label="关联产品" width="160">
          <template #default="{ row }">
            {{ row.productName || '-' }}
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
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
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
      :title="editingId ? '编辑映射规则' : '新增映射规则'"
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
        <el-form-item label="关联产品" prop="productId">
          <el-select
            v-model="formData.productId"
            placeholder="请选择产品"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in productList"
              :key="item.id"
              :label="item.productName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="映射配置" prop="mappingsConfig">
          <el-input
            v-model="formData.mappingsConfig"
            type="textarea"
            :rows="12"
            placeholder='请输入 JSON 格式的映射配置，例如：
{
  "propertyMappings": [
    {
      "sourceField": "temperature",
      "targetProperty": "Temperature",
      "dataType": "double",
      "transformExpression": "value / 10.0"
    }
  ],
  "eventMappings": [
    {
      "sourceField": "alarmCode",
      "targetEvent": "AlarmEvent",
      "conditionExpression": "alarmCode != 0"
    }
  ]
}'
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'
import { getAllProducts } from '@/api/product'
import type { Product } from '@/api/product'

/** 映射规则接口 */
interface MappingRule {
  id: number
  ruleName: string
  productId: number
  productName?: string
  mappingsConfig: string
  status: string
  description?: string
  createTime: string
}

// 列表数据
const loading = ref(false)
const rules = ref<MappingRule[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 产品列表
const productList = ref<Product[]>([])

// 搜索表单
const searchForm = reactive({
  ruleName: '',
  productId: undefined as number | undefined
})

// 对话框
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 表单数据
const formData = reactive({
  ruleName: '',
  productId: undefined as number | undefined,
  mappingsConfig: '',
  status: '1',
  description: ''
})

// JSON 格式校验器
const validateJson = (_rule: any, value: string, callback: Function) => {
  if (!value) {
    callback(new Error('请输入映射配置'))
    return
  }
  try {
    JSON.parse(value)
    callback()
  } catch {
    callback(new Error('请输入合法的 JSON 格式'))
  }
}

// 表单验证规则
const formRules: FormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择关联产品', trigger: 'change' }],
  mappingsConfig: [{ required: true, validator: validateJson, trigger: 'blur' }]
}

// 加载产品列表
async function loadProducts() {
  try {
    const data = await getAllProducts()
    productList.value = data.records || data.list || []
  } catch {
    productList.value = []
  }
}

// 加载列表
async function loadRules() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.ruleName) params.ruleName = searchForm.ruleName
    if (searchForm.productId) params.productId = searchForm.productId

    const data = await request.get('/mapping-rules', { params })
    rules.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载映射规则列表失败')
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
  searchForm.productId = undefined
  currentPage.value = 1
  loadRules()
}

function handleAdd() {
  editingId.value = null
  Object.assign(formData, {
    ruleName: '',
    productId: undefined,
    mappingsConfig: '',
    status: '1',
    description: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: MappingRule) {
  editingId.value = row.id
  Object.assign(formData, {
    ruleName: row.ruleName,
    productId: row.productId,
    mappingsConfig: row.mappingsConfig,
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
      await request.put(`/mapping-rules/${editingId.value}`, formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/mapping-rules', formData)
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

async function handleDelete(row: MappingRule) {
  try {
    await ElMessageBox.confirm(`确定删除映射规则 ${row.ruleName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/mapping-rules/${row.id}`)
    ElMessage.success('删除成功')
    loadRules()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadProducts()
  loadRules()
})
</script>

<style scoped>
/* 页面容器 */
.mapping-rule-page {
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
</style>
