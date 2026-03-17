<template>
  <!-- 新增/编辑设备对话框 -->
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑设备' : '新增设备'"
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
      <el-form-item label="设备编码" prop="deviceCode">
        <el-input
          v-model="formData.deviceCode"
          placeholder="请输入设备编码"
          :disabled="isEdit"
        />
      </el-form-item>
      <el-form-item label="设备名称" prop="deviceName">
        <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
      </el-form-item>
      <el-form-item label="所属产品" prop="productId">
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
      <el-form-item label="协议类型" prop="protocolType">
        <el-select v-model="formData.protocolType" placeholder="请选择协议类型" style="width: 100%">
          <el-option label="TCP" value="TCP" />
          <el-option label="MQTT" value="MQTT" />
          <el-option label="CoAP" value="COAP" />
          <el-option label="HTTP" value="HTTP" />
        </el-select>
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
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createDevice, updateDevice } from '@/api/device'
import type { Device } from '@/api/device'
import { getAllProducts } from '@/api/product'
import type { Product } from '@/api/product'

// Props
const props = defineProps<{
  modelValue: boolean
  editData?: Device | null
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

// 产品列表（用于下拉选择）
const productList = ref<Product[]>([])

// 表单数据
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive({
  deviceCode: '',
  deviceName: '',
  productId: undefined as number | undefined,
  protocolType: 'MQTT'
})

// 表单验证规则
const formRules: FormRules = {
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择所属产品', trigger: 'change' }],
  protocolType: [{ required: true, message: '请选择协议类型', trigger: 'change' }]
}

// 监听 editData 变化，填充表单
watch(() => props.editData, (val) => {
  if (val) {
    isEdit.value = true
    editingId.value = val.id
    Object.assign(formData, {
      deviceCode: val.deviceCode,
      deviceName: val.deviceName,
      productId: val.productId,
      protocolType: val.protocolType
    })
  } else {
    isEdit.value = false
    editingId.value = null
    Object.assign(formData, {
      deviceCode: '',
      deviceName: '',
      productId: undefined,
      protocolType: 'MQTT'
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
      await updateDevice(editingId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createDevice(formData)
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

// 加载产品列表
async function loadProducts() {
  try {
    const data = await getAllProducts()
    productList.value = data.records || data.list || []
  } catch {
    productList.value = []
  }
}

onMounted(() => {
  loadProducts()
})
</script>
