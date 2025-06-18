<!-- src/components/base/BaseDialogModal.vue -->
<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="400px"
      @update:visible="visible = $event"
  >
    <div class="whitespace-pre-line text-gray-700 dark:text-gray-300 mb-6">
      {{ message }}
    </div>

    <template #footer>
      <button
          class="btn-outline"
          @click="onCancel"
          :disabled="loading"
          type="button"
      >
        {{ cancelText }}
      </button>
      <button
          class="btn-primary ml-3"
          :class="{ 'btn-danger': type === 'danger' }"
          @click="handleConfirm"
          :disabled="loading"
          type="button"
      >
        {{ loading ? '处理中...' : confirmText }}
      </button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import emitter from '@/utils/eventBus'
import BaseModal from './BaseModal.vue'

// 状态
const visible = ref(false)
const title = ref('操作确认')
const message = ref('确定要执行此操作吗？')
const confirmText = ref('确定')
const cancelText = ref('取消')
const type = ref<'primary' | 'danger'>('primary')
const loading = ref(false)

// 回调
let confirmCallback: (() => Promise<void> | void) | null = null
let cancelCallback: (() => void) | null = null

// 弹出确认框
function showConfirm(options: {
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  type?: 'primary' | 'danger'
  onConfirm?: () => Promise<void> | void
  onCancel?: () => void
}) {
  title.value = options.title ?? '操作确认'
  message.value = options.message ?? '确定要执行此操作吗？'
  confirmText.value = options.confirmText ?? '确定'
  cancelText.value = options.cancelText ?? '取消'
  type.value = options.type ?? 'primary'
  confirmCallback = options.onConfirm ?? null
  cancelCallback = options.onCancel ?? null
  loading.value = false
  visible.value = true
}

// 取消按钮
function onCancel() {
  if (loading.value) return
  visible.value = false
  cancelCallback?.()
}

// 确认按钮
async function handleConfirm() {
  if (loading.value) return
  loading.value = true
  try {
    await confirmCallback?.()
  } finally {
    loading.value = false
    visible.value = false
  }
}

// 监听键盘 ESC 关闭
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && visible.value) {
    onCancel()
  }
}

// 生命周期钩子
onMounted(() => {
  emitter.on('confirm', showConfirm)
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  emitter.off('confirm', showConfirm)
  window.removeEventListener('keydown', onKeydown)
})
</script>
