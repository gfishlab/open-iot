<template>
  <div class="user-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">用户列表</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" style="margin-bottom: 16px">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 140px">
            <el-option label="平台管理员" value="platform_admin" />
            <el-option label="租户管理员" value="tenant_admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="glass-button" type="primary" @click="handleSearch">搜索</el-button>
          <el-button class="glass-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button class="glass-button" type="primary" @click="handleAdd">新增用户</el-button>
      </div>

      <!-- 用户列表 -->
      <el-table :data="users" v-loading="loading" style="width: 100%">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="150" />
        <el-table-column prop="role" label="角色" width="140">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="getRoleTagType(row.role)">
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="glass-tag" :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button class="glass-button" size="small" @click="handleEdit(row)">编辑</el-button>
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 新增/编辑用户对话框 -->
    <UserForm
      v-model="formDialogVisible"
      :editData="currentEditUser"
      @success="loadUsers"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserList } from '@/api/user'
import type { User } from '@/api/user'
import UserForm from './UserForm.vue'

// 列表数据
const loading = ref(false)
const users = ref<User[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  username: '',
  role: ''
})

// 表单对话框
const formDialogVisible = ref(false)
const currentEditUser = ref<User | null>(null)

// 加载用户列表
async function loadUsers() {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.role) params.role = searchForm.role

    const data = await getUserList(params as any)
    users.value = data.records || data.list || []
    total.value = data.total || 0
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadUsers()
}

function handleReset() {
  searchForm.username = ''
  searchForm.role = ''
  currentPage.value = 1
  loadUsers()
}

function handleAdd() {
  currentEditUser.value = null
  formDialogVisible.value = true
}

function handleEdit(row: User) {
  currentEditUser.value = { ...row }
  formDialogVisible.value = true
}

/** 获取角色显示文字 */
function getRoleText(role: string): string {
  const map: Record<string, string> = {
    platform_admin: '平台管理员',
    tenant_admin: '租户管理员',
    user: '普通用户'
  }
  return map[role] || role
}

/** 获取角色标签类型 */
function getRoleTagType(role: string): string {
  const map: Record<string, string> = {
    platform_admin: 'danger',
    tenant_admin: 'warning',
    user: ''
  }
  return map[role] || ''
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
/* 页面容器 */
.user-list-page {
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
