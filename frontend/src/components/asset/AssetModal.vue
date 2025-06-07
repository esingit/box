<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-container">
      <ModalHeader 
        :title="title" 
        @close="$emit('cancel')" 
      />
      
      <div class="modal-body">
        <AssetForm
          ref="formRef"
          :form="form"
          :asset-names="assetNames"
          :types="types"
          :units="units"
          :locations="locations"
          :remark-placeholder="remarkPlaceholder"
          @maintain="handleMaintainClick"
        />
      </div>

      <div class="modal-footer">
        <button class="btn btn-text" @click="$emit('cancel')">
          取消
        </button>
        <button
          class="btn btn-primary"
          :disabled="loading || !isFormValid"
          @click="handleSubmit"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </div>
  </div>

  <AssetNamesModal
    v-if="showNamesModal"
    :show="showNamesModal"
    @close="handleNamesModalClose"
    @refresh="handleNamesModalRefresh"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import ModalHeader from './ModalHeader.vue'
import AssetForm from './AssetForm.vue'
import AssetNamesModal from './AssetNamesModal.vue'

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  form: {
    type: Object,
    required: true
  },
  assetNames: {
    type: Array,
    required: true
  },
  types: {
    type: Array,
    required: true
  },
  units: {
    type: Array,
    required: true
  },
  locations: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  remarkPlaceholder: {
    type: String,
    default: '备注'
  }
})

const formRef = ref(null)
const showNamesModal = ref(false)
const emit = defineEmits(['submit', 'cancel', 'refresh-names'])

// 表单验证状态
const isFormValid = computed(() => {
  if (!formRef.value) return false
  return formRef.value.isValid
})

// 确保日期格式正确
const formatDate = (date) => {
  if (!date) return null
  if (date.includes('T')) return date
  return `${date}T00:00:00`
}

// 提交处理
function handleSubmit() {
  if (!isFormValid.value || props.loading) return
  const formData = { ...props.form }
  formData.acquireTime = formatDate(formData.acquireTime)
  emit('submit', formData)
}

function handleMaintainClick() {
  showNamesModal.value = true
}

function handleNamesModalClose() {
  showNamesModal.value = false
}

function handleNamesModalRefresh() {
  emit('refresh-names')
}

// 监听父组件的 show 属性变化
watch(() => props.show, (newVal) => {
  if (!newVal) {
    showNamesModal.value = false
  }
})
</script>

