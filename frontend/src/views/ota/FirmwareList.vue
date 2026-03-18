<template>
  <div class="firmware-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">OTA 固件管理</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="产品">
          <el-select v-model="searchForm.productId" placeholder="全部产品" clearable style="width: 160px">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="searchForm.version" placeholder="请输入版本号" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="handleSearch">搜索</el-button>
          <el-button class="glass-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button class="glass-button" type="primary" @click="handleUpload">上传固件</el-button>
        <el-button class="glass-button" @click="loadFirmwares">刷新</el-button>
      </div>

      <!-- 固件列表 -->
      <el-table :data="firmwares" v-loading="loading" style="width: 100%">
        <el-table-column prop="firmwareName" label="固件名称" min-width="200" />
        <el-table-column prop="version" label="版本号" width="120">
          <template #default="{ row }">
            <el-tag class="glass-tag" type="primary">{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="所属产品" width="180" />
        <el-table-column prop="fileSize" label="文件大小" width="100">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="signMethod" label="签名方式" width="100" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button class="glass-button" size="small" @click="handleDownload(row)">下载</el-button>
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
          @size-change="loadFirmwares"
          @current-change="loadFirmwares"
        />
      </div>
    </el-card>

    <!-- 上传固件对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传固件" width="600px" :close-on-click-modal="false">
      <el-form :model="uploadForm" :rules="uploadRules" ref="uploadFormRef" label-width="100px">
        <el-form-item label="所属产品" prop="productId">
          <el-select v-model="uploadForm.productId" placeholder="请选择产品" style="width: 100%">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="固件名称" prop="firmwareName">
          <el-input v-model="uploadForm.firmwareName" placeholder="请输入固件名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="uploadForm.version" placeholder="如: 1.0.0" />
        </el-form-item>
        <el-form-item label="签名方式" prop="signMethod">
          <el-select v-model="uploadForm.signMethod" placeholder="请选择签名方式" style="width: 100%">
            <el-option label="不签名" value="NONE" />
            <el-option label="MD5" value="MD5" />
            <el-option label="SHA256" value="SHA256" />
            <el-option label="RSA" value="RSA" />
          </el-select>
        </el-form-item>
        <el-form-item label="固件文件" prop="file" ref="fileFormItemRef">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :file-list="fileList"
            accept=".bin,.hex,.elf"
          >
            <el-button class="glass-button" type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .bin、.hex、.elf 格式，文件大小不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input
            v-model="uploadForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入版本更新内容、修复的问题等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadInstance, type UploadUserFile, type UploadFile, type UploadProps } from 'element-plus'
import request from '@/utils/request'
import * as otaApi from '@/api/ota'
import type { Firmware } from '@/api/ota'

// 类型定义
interface Firmware {
  id: number
  firmwareName: string
  version: string
  productId: number
  productName?: string
  fileSize: number
  signMethod: string
  filePath: string
  description?: string
  uploadTime: string
}

interface Product {
  id: number
  name: string
}

// 列表数据
const loading = ref(false)
const firmwares = ref<Firmware[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 产品列表
const products = ref<Product[]>([])

// 搜索表单
const searchForm = reactive({
  productId: undefined as number | undefined,
  version: ''
})

// 上传对话框
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()
const fileFormItemRef = ref()
const fileList = ref<UploadUserFile[]>([])

// 上传表单
const uploadForm = reactive({
  productId: undefined as number | undefined,
  firmwareName: '',
  version: '',
  signMethod: 'NONE',
  description: '',
  file: null as File | null
})

// 上传表单验证规则
const uploadRules: FormRules = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  firmwareName: [{ required: true, message: '请输入固件名称', trigger: 'blur' }],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+$/, message: '版本号格式不正确，如：1.0.0', trigger: 'blur' }
  ],
  signMethod: [{ required: true, message: '请选择签名方式', trigger: 'change' }],
  file: [{ required: true, message: '请选择固件文件', trigger: 'change' }]
}

// 加载固件列表
async function loadFirmwares() {
  try {
    loading.value = true
    const data = await otaApi.getFirmwareList({
      page: currentPage.value,
      size: pageSize.value,
      productId: searchForm.productId
    })
    firmwares.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载固件列表失败')
  } finally {
    loading.value = false
  }
}

// 加载产品列表
async function loadProducts() {
  try {
    const data = await request.get('/products', { params: { pageNum: 1, pageSize: 1000 } })
    products.value = (data.records || data.list || []).map((p: any) => ({
      id: p.id,
      name: p.productName || p.name
    }))
  } catch (error) {
    console.error('加载产品列表失败', error)
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadFirmwares()
}

// 重置
function handleReset() {
  searchForm.productId = undefined
  searchForm.version = ''
  currentPage.value = 1
  loadFirmwares()
}

// 打开上传对话框
function handleUpload() {
  Object.assign(uploadForm, {
    productId: undefined,
    firmwareName: '',
    version: '',
    signMethod: 'NONE',
    description: '',
    file: null
  })
  fileList.value = []
  uploadDialogVisible.value = true
}

// 文件选择变化
const handleFileChange: UploadProps['onChange'] = (uploadFile: UploadFile) => {
  if (uploadFile.raw) {
    uploadForm.file = uploadFile.raw
    if (!uploadForm.firmwareName) {
      uploadForm.firmwareName = uploadFile.name.replace(/\.(bin|hex|elf)$/, '')
    }
  }
}

// 文件超出限制
const handleExceed: UploadProps['onExceed'] = (files) => {
  ElMessage.warning('只能上传一个文件')
}

// 确认上传
async function confirmUpload() {
  try {
    await uploadFormRef.value?.validate()
    if (!uploadForm.file) {
      ElMessage.warning('请选择固件文件')
      return
    }

    uploading.value = true
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    formData.append('productId', String(uploadForm.productId))
    formData.append('firmwareName', uploadForm.firmwareName)
    formData.append('version', uploadForm.version)
    formData.append('signMethod', uploadForm.signMethod)
    if (uploadForm.description) {
      formData.append('description', uploadForm.description)
    }

    await otaApi.uploadFirmware(uploadForm as any, uploadForm.file!)
    ElMessage.success('固件上传成功')
    uploadDialogVisible.value = false
    loadFirmwares()
  } catch (error: any) {
    if (error !== false) {
      ElMessage.error(error.message || '上传失败')
    }
  } finally {
    uploading.value = false
  }
}

// 下载固件
async function handleDownload(row: Firmware) {
  try {
    const blob = await otaApi.downloadFirmware(row.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${row.firmwareName}_${row.version}.bin`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('开始下载固件')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 删除固件
async function handleDelete(row: Firmware) {
  try {
    await ElMessageBox.confirm(`确定删除固件 ${row.firmwareName} (${row.version}) 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await otaApi.deleteFirmware(row.id)
    ElMessage.success('删除成功')
    loadFirmwares()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 格式化文件大小
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

onMounted(() => {
  loadFirmwares()
  loadProducts()
})
</script>

<style scoped>
/* 页面容器 */
.firmware-list-page {
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

/* 上传提示 */
.el-upload__tip {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}
</style>
