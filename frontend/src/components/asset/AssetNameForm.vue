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
            <label class="form-label">
              资产名称
              <span class="required">*</span>
            </label>
            <input 
              v-model="formData.name"
              type="text" 
              class="input input-md"
              :class="{ 'input-error': !!formError }"
              required
              placeholder="请输入资产名称"
              @input="$emit('update:formError', '')"
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
                :disabled="loading || !!formError"
              >
                {{ loading ? '提交中...' : (isEdit ? '保存' : '确定') }}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ModalHeader from './ModalHeader.vue'

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
  },
  formError: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'submit', 'update:formError'])

const isEdit = computed(() => !!props.editData)

const formData = ref({
  id: props.editData?.id || null,
  name: props.editData?.name || '',
  description: props.editData?.description || ''
})

function handleClose() {
  emit('update:formError', '')
  emit('close')
}

function handleSubmit() {
  if (!formData.value.name.trim()) return
  emit('submit', { ...formData.value })
}
</script>
