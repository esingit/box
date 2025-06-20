<template>
  <form @submit.prevent="onSubmit" class="space-y-4">
    <div>
      <label for="name" class="block text-sm font-medium text-gray-700">
        资产名称 <span class="text-red-500">*</span>
      </label>
      <input
          id="name"
          v-model="localFormData.name"
          type="text"
          placeholder="请输入资产名称"
          class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2
               placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          @input="clearError"
          autocomplete="off"
      />
      <p v-if="formError" class="mt-1 text-sm text-red-600">{{ formError }}</p>
    </div>

    <div>
      <label for="description" class="block text-sm font-medium text-gray-700">
        资产描述
      </label>
      <textarea
          id="description"
          v-model="localFormData.description"
          rows="3"
          placeholder="请输入资产描述（选填）"
          class="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2
               placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
          @input="clearError"
      ></textarea>
    </div>

    <div class="flex justify-end space-x-3">
      <button
          type="button"
          @click="closeModal"
          class="btn-outline"
      >
        取消
      </button>

      <button
          type="submit"
          :disabled="loadingSubmit"
          class="btn-primary"
      >
        {{ isEdit ? '保存' : '新增' }}
      </button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, toRefs, watch } from 'vue'

const props = defineProps<{
  formData: { id: number | null; name: string; description: string }
  loadingSubmit: boolean
  formError: string
  isEdit: boolean
}>()

const emit = defineEmits(['submit', 'closeModal', 'update:formData', 'clearError'])

const localFormData = reactive({
  id: props.formData.id,
  name: props.formData.name,
  description: props.formData.description
})

// 同步props.formData变动到localFormData
watch(
    () => props.formData,
    (newVal) => {
      localFormData.id = newVal.id
      localFormData.name = newVal.name
      localFormData.description = newVal.description
    },
    { deep: true }
)

function onSubmit() {
  emit('submit')
}

function closeModal() {
  emit('closeModal')
}

function clearError() {
  if (props.formError) {
    emit('clearError')
  }
}

// 双向绑定外层formData，实时同步数据给父组件
watch(
    () => ({ ...localFormData }),
    (newVal) => {
      emit('update:formData', { ...newVal })
    },
    { deep: true }
)
</script>
