<template>
  <div v-if="show" class="modal-overlay" @click.self="handleClose">
    <div class="modal-container modal-sm">
      <ModalHeader 
        :title="isEdit ? '编辑资产名称' : '新增资产名称'"
        @close="handleClose" 
      />

      <div class="modal-body">
        <form @submit.prevent="handleSubmit" class="form">
          <div class="form-group">
            <label class="form-label required">
              资产名称
            </label>
            <input 
              v-model="formData.name"
              type="text" 
              class="input input-md"
              :class="{ 'input-error': formError }"
              required
              placeholder="请输入资产名称"
              @input="formError = ''"
            />
            <div v-if="formError" class="form-error">{{ formError }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">描述</label>
            <input 
              v-model="formData.description"
              type="text" 
              class="input input-md"
              placeholder="请输入描述（可选）"
            />
          </div>

          <div class="modal-footer">
            <div class="form-actions">
              <button 
                type="button" 
                class="btn btn-text" 
                @click="handleClose"
              >
                取消
              </button>
              <button 
                type="submit" 
                class="btn btn-primary" 
                :class="{ 'btn-loading': loading }"
                :disabled="loading"
              >
                <span v-if="loading" class="btn-icon animate-spin">
                  <i class="lucide-loader-2" />
                </span>
                <span>{{ loading ? '处理中...' : '确定' }}</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import ModalHeader from './ModalHeader.vue'
import emitter from '@/utils/eventBus.js'

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  editData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'submit'])

const isEdit = computed(() => !!props.editData)

const formData = ref({
  name: '',
  description: ''
})
const formError = ref('')

// 监听编辑数据变化
watch(() => props.editData, (newVal) => {
  if (newVal) {
    formData.value = {
      name: newVal.name || '',
      description: newVal.description || ''
    }
  } else {
    formData.value = {
      name: '',
      description: ''
    }
  }
}, { immediate: true })

// 监听show和editData属性变化
watch([() => props.show, () => props.editData], ([showVal, editDataVal]) => {
  if (!showVal) {
    // 弹窗关闭时重置表单
    resetForm()
  } else if (editDataVal) {
    // 编辑模式下，填充表单数据
    formData.value = {
      name: editDataVal.name || '',
      description: editDataVal.description || ''
    }
  } else {
    // 新增模式下，重置表单
    resetForm()
  }
}, { immediate: true })

function resetForm() {
  formData.value = {
    name: '',
    description: ''
  }
}

function handleClose() {
  // 只需要触发关闭事件，表单重置会在watch中处理
  emit('close')
}

function handleSubmit() {
  // 表单验证
  const name = formData.value.name.trim()
  if (!name) {
    formError.value = '请输入资产名称'
    return
  }

  const submitData = {
    name,
    description: formData.value.description.trim() || '',
    id: props.editData?.id
  }
  
  emit('submit', submitData)
  
  // 不在这里关闭弹窗，让父组件在处理完提交后决定是否关闭
  // 父组件会在处理成功后调用close事件，我们在watch中处理表单重置
}
</script>
