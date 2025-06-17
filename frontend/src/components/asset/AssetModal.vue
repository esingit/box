<template>
  <BaseModal
      :visible="show"
      :title="title"
      :widthClass="'w-full max-w-lg'"
      @update:visible="handleVisibleChange"
      width="500px"
  >
    <!-- 内容插槽 -->
    <div class="p-6 flex-1 overflow-auto">
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

    <!-- 底部插槽 -->
    <template #footer>
      <div class="flex justify-end space-x-4 p-4 border-t border-gray-200 dark:border-gray-700">
        <button
            class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
            @click="handleCancel"
        >
          取消
        </button>
        <button
            class="px-4 py-2 rounded-md text-white font-semibold transition
                 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 disabled:cursor-not-allowed"
            :disabled="loading || !isFormValid"
            @click="handleSubmit"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </template>

  </BaseModal>

  <AssetNamesModal
      v-if="showNamesModal"
      :show="showNamesModal"
      @close="handleNamesModalClose"
      @refresh="handleNamesModalRefresh"
  />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import AssetForm from './AssetForm.vue'
import AssetNamesModal from './AssetNamesModal.vue'

const props = defineProps({
  show: Boolean,
  title: String,
  form: Object,
  assetNames: Array,
  types: Array,
  units: Array,
  locations: Array,
  loading: { type: Boolean, default: false },
  confirmText: { type: String, default: '确定' },
  remarkPlaceholder: { type: String, default: '备注' }
})

const emit = defineEmits(['submit', 'cancel', 'refresh-names'])

const formRef = ref<any>(null)
const showNamesModal = ref(false)

const isFormValid = computed(() => {
  return formRef.value?.isValid ?? false
})

function formatDate(date: string | null) {
  if (!date) return null
  if (date.includes('T')) return date
  return `${date}T00:00:00`
}

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

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}

function handleVisibleChange(val: boolean) {
  if (!val) handleCancel()
  emit('update:visible', val)
}

watch(() => props.show, (newVal) => {
  if (!newVal) {
    showNamesModal.value = false
  }
})
</script>
