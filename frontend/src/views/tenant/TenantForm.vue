<template>
  <!-- 新增/编辑租户对话框 -->
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑租户' : '新增租户'"
    width="560px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="租户名称" prop="tenantName">
        <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
      </el-form-item>
      <el-form-item label="联系邮箱" prop="contactEmail">
        <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
      </el-form-item>
      <!-- 仅新增时需要填写管理员信息 -->
      <template v-if="!isEdit">
        <el-divider content-position="left">管理员账号</el-divider>
        <el-form-item label="管理员账号" prop="adminUsername">
          <el-input v-model="formData.adminUsername" placeholder="请输入管理员登录账号" />
        </el-form-item>
        <el-form-item label="管理员密码" prop="adminPassword">
          <el-input
            v-model="formData.adminPassword"
            type="password"
            show-password
            placeholder="请输入管理员密码"
          />
        </el-form-item>
      </template>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button class="glass-button" type="primary" @click="handleSubmit" :loading="submitLoading">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createTenant, updateTenant } from '@/api/tenant'
import type { Tenant } from '@/api/tenant'

// Props
const props = defineProps<{
  modelValue: boolean
  editData?: Tenant | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'success'): void
}>()

// 对话框可见性双向绑定
const visible = ref(false)
watch(() => props.modelValue, val => { visible.value = val })
watch(visible, val => { emit('update:modelValue', val) })

// 是否编辑模式
const isEdit = ref(false)
const editingId = ref<number | null>(null)

// 表单数据
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({
  tenantName: '',
  contactEmail: '',
  adminUsername: '',
  adminPassword: ''
})

// 表单验证规则
const formRules: FormRules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  adminUsername: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  adminPassword: [
    { required: true, message: '请输入管理员密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 监听 editData 变化，填充表单
watch(() => props.editData, (val) => {
  if (val) {
    isEdit.value = true
    editingId.value = val.id
    Object.assign(formData, {
      tenantName: val.tenantName,
      contactEmail: val.contactEmail,
      adminUsername: '',
      adminPassword: ''
    })
  } else {
    isEdit.value = false
    editingId.value = null
    Object.assign(formData, {
      tenantName: '',
      contactEmail: '',
      adminUsername: '',
      adminPassword: ''
    })
  }
})

// 关闭对话框时重置表单
function handleClose() {
  formRef.value?.resetFields()
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    if (isEdit.value && editingId.value) {
      // 编辑只提交租户基本信息
      await updateTenant(editingId.value, {
        tenantName: formData.tenantName,
        contactEmail: formData.contactEmail
      })
      ElMessage.success('更新成功')
    } else {
      // 新增需要同时传管理员信息
      await createTenant({
        tenantName: formData.tenantName,
        contactEmail: formData.contactEmail,
        adminUsername: formData.adminUsername,
        adminPassword: formData.adminPassword
      })
      ElMessage.success('创建成功')
    }
    visible.value = false
    emit('success')
  } catch (error: unknown) {
    const err = error as Error
    if (err.message) {
      ElMessage.error(err.message)
    }
  } finally {
    submitLoading.value = false
  }
}
</script>
