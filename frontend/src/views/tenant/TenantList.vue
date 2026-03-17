<template>
  <div class="tenant-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">租户列表</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="租户名称">
          <el-input v-model="searchForm.tenantName" placeholder="请输入租户名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="handleSearch">搜索</el-button>
          <el-button class="glass-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button class="glass-button" type="primary" @click="handleAdd">新增租户</el-button>
      </div>

      <el-table :data="tenants" style="width: 100%" v-loading="loading">
        <el-table-column prop="tenantCode" label="租户编码" width="150" />
        <el-table-column prop="tenantName" label="租户名称" width="180" />
        <el-table-column prop="contactEmail" label="联系邮箱" width="220" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button class="glass-button" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              class="glass-button"
              size="small"
              :type="row.status === '1' ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === '1' ? '禁用' : '启用' }}
            </el-button>
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
          @size-change="loadTenants"
          @current-change="loadTenants"
        />
      </div>
    </el-card>

    <!-- 新增/编辑租户对话框 -->
    <TenantForm
      v-model="formDialogVisible"
      :editData="currentEditTenant"
      @success="loadTenants"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTenantList, enableTenant, disableTenant, deleteTenant } from '@/api/tenant'
import type { Tenant } from '@/api/tenant'
import TenantForm from './TenantForm.vue'

const loading = ref(false)
const tenants = ref<Tenant[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  tenantName: ''
})

// 表单对话框
const formDialogVisible = ref(false)
const currentEditTenant = ref<Tenant | null>(null)

// 加载租户列表
async function loadTenants() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.tenantName) params.tenantName = searchForm.tenantName

    const data = await getTenantList(params as any)
    tenants.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadTenants()
}

function handleReset() {
  searchForm.tenantName = ''
  currentPage.value = 1
  loadTenants()
}

function handleAdd() {
  currentEditTenant.value = null
  formDialogVisible.value = true
}

function handleEdit(row: Tenant) {
  currentEditTenant.value = { ...row }
  formDialogVisible.value = true
}

/** 切换租户启用/禁用状态 */
async function handleToggleStatus(row: Tenant) {
  const action = row.status === '1' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}租户 ${row.tenantName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    if (row.status === '1') {
      await disableTenant(row.id)
    } else {
      await enableTenant(row.id)
    }
    ElMessage.success(`${action}成功`)
    loadTenants()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

async function handleDelete(row: Tenant) {
  try {
    await ElMessageBox.confirm(`确定删除租户 ${row.tenantName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteTenant(row.id)
    ElMessage.success('删除成功')
    loadTenants()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadTenants()
})
</script>

<style scoped>
/* 页面容器 */
.tenant-list-page {
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
