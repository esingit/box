<template>
  <BaseModal
      :visible="visible"
      :title="title"
      @update:visible="handleClose"
      width="500px"
  >
    <FitnessForm
        ref="fitnessFormRef"
        :form="form"
        :remark-placeholder="remarkPlaceholder"
        @submit="onInnerSubmit"
        @update:form="updateForm"
    />

    <template #footer>
      <div class="flex justify-end gap-4">
        <button
            class="text-gray-600 hover:text-gray-900 transition"
            @click="handleClose"
        >
          取消
        </button>
        <button
            class="bg-blue-600 text-white px-4 py-2 rounded disabled:bg-blue-300 disabled:cursor-not-allowed transition"
            :disabled="loading || !isValid"
            @click="submitForm"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'
import BaseModal from '@/components/base/BaseModal.vue'
import FitnessForm from './FitnessForm.vue'

const props = defineProps({
  visible: Boolean,
  form: Object,
  loading: Boolean,
  title: String,
  confirmText: String,
  remarkPlaceholder: String,
})

const emit = defineEmits(['close', 'submit', 'update:form'])

const fitnessFormRef = ref<InstanceType<typeof FitnessForm> | null>(null)

const isValid = computed(() => {
  const f = props.form
  return f?.typeId && f?.count && f?.unitId && f?.finishTime
})

function handleClose() {
  emit('close')
}

function updateForm(formData: any) {
  emit('update:form', formData)
}

// 外层点击确认时，调用内层暴露的 submit 方法触发表单校验提交
async function submitForm() {
  if (props.loading) return

  console.log('开始提交表单') // 添加调试日志

  try {
    await nextTick() // 确保 DOM 更新完成
    if (fitnessFormRef.value?.submit) {
      await fitnessFormRef.value.submit()
    } else {
      console.error('表单 ref 或 submit 方法不存在')
    }
  } catch (error) {
    console.error('提交表单时出错:', error)
  }
}

// 内层表单提交成功时触发
function onInnerSubmit(formData: any) {
  emit('submit', formData)
}
</script>