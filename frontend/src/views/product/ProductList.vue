<template>
  <div class="product-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">产品列表</span>
          <el-button type="primary" @click="handleAdd">新增产品</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="产品名称">
          <el-input v-model="searchForm.productName" placeholder="请输入产品名称" clearable style="width: 192px" />
        </el-form-item>
        <el-form-item label="协议类型">
          <el-select v-model="searchForm.protocolType" placeholder="请选择" clearable style="width: 128px">
            <el-option label="MQTT" value="MQTT" />
            <el-option label="HTTP" value="HTTP" />
            <el-option label="CoAP" value="CoAP" />
            <el-option label="LwM2M" value="LwM2M" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 产品列表 -->
      <el-table :data="products" v-loading="loading" style="width: 100%">
        <el-table-column prop="productKey" label="产品Key" width="150" />
        <el-table-column prop="productName" label="产品名称" width="180" />
        <el-table-column prop="productType" label="产品类型" width="100">
          <template #default="{ row }: { row: Product }">
            <el-tag>{{ row.productType === 'DEVICE' ? '设备' : '网关' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="protocolType" label="协议类型" width="100" />
        <el-table-column prop="nodeType" label="节点类型" width="100">
          <template #default="{ row }: { row: Product }">
            <el-tag type="info">{{ row.nodeType === 'DIRECT' ? '直连' : '网关' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataFormat" label="数据格式" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }: { row: Product }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="{ row }: { row: Product }">
            <el-button size="small" @click="handleDetail(row)">详情</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleThingModel(row)">物模型</el-button>
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
          @size-change="loadProducts"
          @current-change="loadProducts"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="formData.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="产品类型" prop="productType">
          <el-select v-model="formData.productType" placeholder="请选择产品类型" style="width: 100%">
            <el-option label="设备" value="DEVICE" />
            <el-option label="网关" value="GATEWAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="协议类型" prop="protocolType">
          <el-select v-model="formData.protocolType" placeholder="请选择协议类型" style="width: 100%">
            <el-option label="MQTT" value="MQTT" />
            <el-option label="HTTP" value="HTTP" />
            <el-option label="CoAP" value="CoAP" />
            <el-option label="LwM2M" value="LwM2M" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-select v-model="formData.nodeType" placeholder="请选择节点类型" style="width: 100%">
            <el-option label="直连设备" value="DIRECT" />
            <el-option label="网关设备" value="GATEWAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据格式" prop="dataFormat">
          <el-select v-model="formData.dataFormat" placeholder="请选择数据格式" style="width: 100%">
            <el-option label="JSON" value="JSON" />
            <el-option label="XML" value="XML" />
            <el-option label="二进制" value="BINARY" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 物模型编辑对话框 -->
    <el-dialog
      v-model="thingModelDialogVisible"
      title="物模型定义"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-tabs v-model="thingModelTab">
        <el-tab-pane label="属性定义" name="properties">
          <div style="min-height: 200px">
            <el-button type="primary" size="small" @click="addProperty" style="margin-bottom: 8px">
              添加属性
            </el-button>
            <el-table :data="thingModelData.properties" border size="small">
              <el-table-column prop="identifier" label="标识符" width="120">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-input v-model="row.identifier" size="small" placeholder="如: temperature" />
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" width="120">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-input v-model="row.name" size="small" placeholder="如: 温度" />
                </template>
              </el-table-column>
              <el-table-column prop="dataType" label="数据类型" width="100">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-select v-model="row.dataType" size="small">
                    <el-option label="int" value="int" />
                    <el-option label="float" value="float" />
                    <el-option label="string" value="string" />
                    <el-option label="boolean" value="boolean" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column prop="unit" label="单位" width="80">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-input v-model="row.unit" size="small" placeholder="如: ℃" />
                </template>
              </el-table-column>
              <el-table-column prop="min" label="最小值" width="80">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-input v-model="row.min" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="max" label="最大值" width="80">
                <template #default="{ row }: { row: PropertyDefinition }">
                  <el-input v-model="row.max" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button type="danger" size="small" link @click="removeProperty($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="事件定义" name="events">
          <div style="min-height: 200px">
            <el-button type="primary" size="small" @click="addEvent" style="margin-bottom: 8px">
              添加事件
            </el-button>
            <el-table :data="thingModelData.events" border size="small">
              <el-table-column prop="identifier" label="标识符" width="150">
                <template #default="{ row }: { row: EventDefinition }">
                  <el-input v-model="row.identifier" size="small" placeholder="如: high_temp_alert" />
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" width="150">
                <template #default="{ row }: { row: EventDefinition }">
                  <el-input v-model="row.name" size="small" placeholder="如: 高温告警" />
                </template>
              </el-table-column>
              <el-table-column prop="type" label="事件类型" width="120">
                <template #default="{ row }: { row: EventDefinition }">
                  <el-select v-model="row.type" size="small">
                    <el-option label="信息" value="info" />
                    <el-option label="告警" value="alert" />
                    <el-option label="故障" value="fault" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button type="danger" size="small" link @click="removeEvent($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="服务定义" name="services">
          <div style="min-height: 200px">
            <el-button type="primary" size="small" @click="addService" style="margin-bottom: 8px">
              添加服务
            </el-button>
            <el-table :data="thingModelData.services" border size="small">
              <el-table-column prop="identifier" label="标识符" width="150">
                <template #default="{ row }: { row: ServiceDefinition }">
                  <el-input v-model="row.identifier" size="small" placeholder="如: reboot" />
                </template>
              </el-table-column>
              <el-table-column prop="name" label="名称" width="150">
                <template #default="{ row }: { row: ServiceDefinition }">
                  <el-input v-model="row.name" size="small" placeholder="如: 重启" />
                </template>
              </el-table-column>
              <el-table-column prop="callType" label="调用方式" width="120">
                <template #default="{ row }: { row: ServiceDefinition }">
                  <el-select v-model="row.callType" size="small">
                    <el-option label="同步" value="sync" />
                    <el-option label="异步" value="async" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button type="danger" size="small" link @click="removeService($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="thingModelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveThingModel" :loading="thingModelLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
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

const router = useRouter()

// 列表数据
const loading = ref(false)
const products = ref<Product[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  productName: '',
  protocolType: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('新增产品')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const editingId = ref<number | null>(null)

// 表单数据
const formData = reactive({
  productName: '',
  productType: 'DEVICE',
  protocolType: 'MQTT',
  nodeType: 'DIRECT',
  dataFormat: 'JSON',
  status: '1'
})

// 表单验证规则
const formRules: FormRules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  productType: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
  protocolType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  dataFormat: [{ required: true, message: '请选择数据格式', trigger: 'change' }]
}

// 物模型对话框
const thingModelDialogVisible = ref(false)
const thingModelTab = ref('properties')
const thingModelLoading = ref(false)
const currentProductId = ref<number | null>(null)
const thingModelData = reactive<ThingModel>({
  properties: [],
  events: [],
  services: []
})

// 加载产品列表
async function loadProducts(): Promise<void> {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.productName) {
      params.productName = searchForm.productName
    }
    if (searchForm.protocolType) {
      params.protocolType = searchForm.protocolType
    }
    const data = await request.get('/products', { params })
    products.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载产品列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch(): void {
  currentPage.value = 1
  loadProducts()
}

// 重置
function handleReset(): void {
  searchForm.productName = ''
  searchForm.protocolType = ''
  currentPage.value = 1
  loadProducts()
}

// 新增
function handleAdd(): void {
  dialogTitle.value = '新增产品'
  editingId.value = null
  Object.assign(formData, {
    productName: '',
    productType: 'DEVICE',
    protocolType: 'MQTT',
    nodeType: 'DIRECT',
    dataFormat: 'JSON',
    status: '1'
  })
  dialogVisible.value = true
}

// 编辑
function handleEdit(row: Product): void {
  dialogTitle.value = '编辑产品'
  editingId.value = row.id
  Object.assign(formData, {
    productName: row.productName,
    productType: row.productType,
    protocolType: row.protocolType,
    nodeType: row.nodeType,
    dataFormat: row.dataFormat,
    status: row.status
  })
  dialogVisible.value = true
}

// 提交表单
async function handleSubmit(): Promise<void> {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (editingId.value) {
      await request.put(`/products/${editingId.value}`, formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/products', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadProducts()
  } catch (error: unknown) {
    const err = error as Error
    if (err.message) {
      ElMessage.error(err.message)
    }
  } finally {
    submitLoading.value = false
  }
}

// 查看详情
function handleDetail(row: Product): void {
  router.push(`/product/${row.id}`)
}

// 删除
async function handleDelete(row: Product): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除产品 ${row.productName} 吗？如果有关联设备将无法删除。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/products/${row.id}`)
    ElMessage.success('删除成功')
    loadProducts()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const err = error as Error
      ElMessage.error(err.message || '删除失败')
    }
  }
}

// 物模型编辑
async function handleThingModel(row: Product): Promise<void> {
  currentProductId.value = row.id
  thingModelTab.value = 'properties'
  try {
    const data = await request.get(`/products/${row.id}/thing-model`)
    thingModelData.properties = data?.properties || []
    thingModelData.events = data?.events || []
    thingModelData.services = data?.services || []
  } catch {
    thingModelData.properties = []
    thingModelData.events = []
    thingModelData.services = []
  }
  thingModelDialogVisible.value = true
}

// 添加属性
function addProperty(): void {
  thingModelData.properties.push({
    identifier: '',
    name: '',
    dataType: 'float',
    unit: '',
    min: '',
    max: ''
  })
}

// 移除属性
function removeProperty(index: number): void {
  thingModelData.properties.splice(index, 1)
}

// 添加事件
function addEvent(): void {
  thingModelData.events.push({
    identifier: '',
    name: '',
    type: 'alert'
  })
}

// 移除事件
function removeEvent(index: number): void {
  thingModelData.events.splice(index, 1)
}

// 添加服务
function addService(): void {
  thingModelData.services.push({
    identifier: '',
    name: '',
    callType: 'async'
  })
}

// 移除服务
function removeService(index: number): void {
  thingModelData.services.splice(index, 1)
}

// 保存物模型
async function handleSaveThingModel(): Promise<void> {
  if (!currentProductId.value) return
  try {
    thingModelLoading.value = true
    // 过滤掉空的条目
    const payload: ThingModel = {
      properties: thingModelData.properties.filter(p => p.identifier && p.name),
      events: thingModelData.events.filter(e => e.identifier && e.name),
      services: thingModelData.services.filter(s => s.identifier && s.name)
    }
    await request.put(`/products/${currentProductId.value}/thing-model`, payload)
    ElMessage.success('物模型保存成功')
    thingModelDialogVisible.value = false
  } catch (error: unknown) {
    const err = error as Error
    ElMessage.error(err.message || '保存失败')
  } finally {
    thingModelLoading.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
/* 页面容器 */
.product-list-page {
  padding: 24px;
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

/* 分页容器 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
