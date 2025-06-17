<template>
  <div
      v-if="show"
      class="fixed inset-0 bg-black bg-opacity-30 flex items-center justify-center z-50"
      @click.self="handleClose"
  >
    <div
        class="bg-white dark:bg-gray-900 rounded-lg shadow-lg w-full max-w-md mx-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
    >
      <ModalHeader
          :title="isEdit ? '编辑资产名称' : '新增资产名称'"
          @close="handleClose"
          class="border-b border-gray-200 dark:border-gray-700"
      />

      <div class="p-6">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div>
            <label
                for="name"
                class="block text-gray-700 dark:text-gray-300 font-medium mb-1"
            >
              资产名称 <span class="text-red-500">*</span>
            </label>
            <input
                id="name"
                v-model="formData.name"
                type="text"
                required
                placeholder="请输入资产名称"
                :class="[
                'w-full rounded-md border px-3 py-2 text-gray-900 dark:text-gray-100 bg-gray-50 dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition',
                formError ? 'border-red-500' : 'border-gray-300 dark:border-gray-700'
              ]"
                @input="$emit('update:formError', '')"
            />
            <p v-if="formError" class="mt-1 text-sm text-red-600">{{ formError }}</p>
          </div>

          <div>
            <label
                for="description"
                class="block text-gray-700 dark:text-gray-300 font-medium mb-1"
            >
              描述
            </label>
            <input
                id="description"
                v-model="formData.description"
                type="text"
                placeholder="请输入描述（可选）"
                class="w-full rounded-md border border-gray-300 dark:border-gray-700 px-3 py-2 text-gray-900 dark:text-gray-100 bg-gray-50 dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
            />
          </div>

          <div class="flex justify-end space-x-3">
            <button
                type="button"
                @click="handleClose"
                class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
            >
              取消
            </button>
            <button
                type="submit"
                :disabled="loading || !!formError"
                :class="[
                'px-4 py-2 rounded-md text-white font-semibold transition',
                loading || formError ? 'bg-indigo-300 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700'
              ]"
            >
              {{ loading ? '提交中...' : isEdit ? '保存' : '确定' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import ModalHeader from './ModalHeader.vue'

const props = defineProps({
  show: Boolean,
  loading: Boolean,
  editData: Object,
  formError: String
})

const emit = defineEmits(['close', 'submit', 'update:formError'])

const isEdit = computed(() => !!props.editData)

const formData = ref({
  id: props.editData?.id || null,
  name: props.editData?.name || '',
  description: props.editData?.description || ''
})

// 同步editData变化（切换编辑对象时刷新表单）
watch(() => props.editData, (val) => {
  formData.value = {
    id: val?.id || null,
    name: val?.name || '',
    description: val?.description || ''
  }
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
