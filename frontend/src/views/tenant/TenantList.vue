<template>
  <div class="tenant-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户列表</span>
          <el-button type="primary" @click="handleAdd">新增租户</el-button>
        </div>
      </template>

      <el-table :data="tenants" style="width: 100%" v-loading="loading">
        <el-table-column prop="tenantCode" label="租户编码" width="150" />
        <el-table-column prop="tenantName" label="租户名称" width="180" />
        <el-table-column prop="contactEmail" label="联系邮箱" width="220" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTenants"
        @current-change="loadTenants"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tenants = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadTenants() {
  try {
    loading.value = true
    const data = await request.get('/tenants', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    tenants.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  ElMessage.info('新增租户功能开发中')
}

function handleEdit(row: any) {
  ElMessage.info(`编辑租户: ${row.tenantName}`)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除租户 ${row.tenantName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/tenants/${row.id}`)
    ElMessage.success('删除成功')
    loadTenants()
  } catch (error: any) {
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
.tenant-list-page {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
