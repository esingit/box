<!-- src/components/base/BaseDialogModal.vue -->
<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="400px"
      @update:visible="visible = $event"
  >
    <div class="msg-strong text-gray-700 dark:text-gray-300 whitespace-pre-line mb-6">
      <!-- 支持字符串或 VNode -->
      <component v-if="isVNodeRef" :is="messageVNode" />
      <p v-else>{{ message }}</p>
    </div>

    <template #footer>
      <button
          class="ml-4 btn-outline"
          @click="onCancel"
          :disabled="loading"
          type="button"
      >
        {{ cancelText }}
      </button>
      <button
          class="ml-4 btn-primary"
          :class="[
          loading ? 'opacity-50 cursor-not-allowed' : '',
          type === 'danger' ? 'btn-danger' : ''
        ]"
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
import { ref, onMounted, onUnmounted, isVNode, type VNode } from 'vue'
import emitter from '@/utils/eventBus'
import BaseModal from './BaseModal.vue'

// 弹窗状态
const visible = ref(false)
const title = ref('操作确认')
const message = ref<string>('确定要执行此操作吗？')
const confirmText = ref('确定')
const cancelText = ref('取消')
const type = ref<'primary' | 'danger'>('primary')
const loading = ref(false)

const isVNodeRef = ref(false)
const messageVNode = ref<VNode | null>(null)

let confirmCallback: (() => Promise<void> | void) | null = null
let cancelCallback: (() => void) | null = null

function showConfirm(options: {
  title?: string
  message?: string | VNode
  confirmText?: string
  cancelText?: string
  type?: 'primary' | 'danger'
  onConfirm?: () => Promise<void> | void
  onCancel?: () => void
}) {
  title.value = options.title ?? '操作确认'
  confirmText.value = options.confirmText ?? '确定'
  cancelText.value = options.cancelText ?? '取消'
  type.value = options.type ?? 'primary'
  confirmCallback = options.onConfirm ?? null
  cancelCallback = options.onCancel ?? null
  loading.value = false
  visible.value = true

  if (isVNode(options.message)) {
    isVNodeRef.value = true
    messageVNode.value = options.message
  } else {
    isVNodeRef.value = false
    message.value = options.message ?? '确定要执行此操作吗？'
  }
}

function onCancel() {
  if (loading.value) return
  visible.value = false
  cancelCallback?.()
}

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

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && visible.value) onCancel()
}

onMounted(() => {
  emitter.on('confirm', showConfirm)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  emitter.off('confirm', showConfirm)
  window.removeEventListener('keydown', onKeydown)
})
</script>
