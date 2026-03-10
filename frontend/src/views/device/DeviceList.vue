<template>
  <div class="device-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备列表</span>
          <el-button type="primary" @click="handleAdd">新增设备</el-button>
        </div>
      </template>

      <el-table :data="devices" style="width: 100%" v-loading="loading" class="glass-card">
        <el-table-column prop="deviceCode" label="设备编码" width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="180" />
        <el-table-column prop="protocolType" label="协议类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.protocolType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="online" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.online ? 'success' : 'info'">
              {{ row.online ? '在线' : '离线' }}
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

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadDevices"
        @current-change="loadDevices"
        class="glass-pagination"
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
const devices = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadDevices() {
  try {
    loading.value = true
    const data = await request.get('/devices', {
      params: {
        page: currentPage.value,
        size: pageSize.value
      }
    })
    devices.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  ElMessage.info('新增设备功能开发中')
}

function handleEdit(row: any) {
  ElMessage.info(`编辑设备: ${row.deviceName}`)
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除设备 ${row.deviceName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.delete(`/devices/${row.id}`)
    ElMessage.success('删除成功')
    loadDevices()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadDevices()
})
</script>

<style scoped>
.device-list-page {
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

.el-pagination {
  margin-top: auto;
  flex-shrink: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
