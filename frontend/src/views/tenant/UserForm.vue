<template>
  <!-- 新增/编辑用户对话框 -->
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑用户' : '新增用户'"
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
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="formData.username"
          placeholder="请输入用户名"
          :disabled="isEdit"
        />
      </el-form-item>
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
          <el-option label="平台管理员" value="platform_admin" />
          <el-option label="租户管理员" value="tenant_admin" />
          <el-option label="普通用户" value="user" />
        </el-select>
      </el-form-item>
      <!-- 仅新增时需要输入密码 -->
      <el-form-item v-if="!isEdit" label="密码" prop="password">
        <el-input
          v-model="formData.password"
          type="password"
          show-password
          placeholder="请输入密码"
        />
      </el-form-item>
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
import { createUser, updateUser } from '@/api/user'
import type { User } from '@/api/user'

// Props
const props = defineProps<{
  modelValue: boolean
  editData?: User | null
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
  username: '',
  realName: '',
  role: 'user',
  password: ''
})

// 表单验证规则
const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 监听 editData 变化，填充表单
watch(() => props.editData, (val) => {
  if (val) {
    isEdit.value = true
    editingId.value = val.id
    Object.assign(formData, {
      username: val.username,
      realName: val.realName,
      role: val.role,
      password: ''
    })
  } else {
    isEdit.value = false
    editingId.value = null
    Object.assign(formData, {
      username: '',
      realName: '',
      role: 'user',
      password: ''
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
      await updateUser(editingId.value, {
        realName: formData.realName,
        role: formData.role
      })
      ElMessage.success('更新成功')
    } else {
      await createUser({
        username: formData.username,
        realName: formData.realName,
        role: formData.role,
        password: formData.password
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
