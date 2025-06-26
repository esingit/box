<!-- src/components/base/BaseDialogModal.vue -->
<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="400px"
      @update:visible="visible = $event"
      :zIndex="2100"
  >
    <!-- 消息内容 -->
    <div class="text-gray-700 dark:text-gray-300 whitespace-pre-line mb-6 leading-relaxed">
      <component v-if="isVNodeRef" :is="messageVNode" />
      <p v-else class="text-sm">{{ message }}</p>
    </div>

    <!-- 按钮组 -->
    <template #footer>
      <div class="flex justify-end gap-3">
        <BaseButton
            @click="onCancel"
            :disabled="loading"
            type="button"
            color="outline"
            class="min-w-[80px]"
        >
          {{ cancelText }}
        </BaseButton>
        <BaseButton
            :color="type === 'danger' ? 'danger' : 'primary'"
            :class="[
              'min-w-[80px]',
              loading ? 'opacity-50 cursor-not-allowed' : ''
            ]"
            @click="handleConfirm"
            :disabled="loading"
            type="button"
        >
          <span v-if="loading" class="flex items-center gap-2">
            <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            处理中...
          </span>
          <span v-else>{{ confirmText }}</span>
        </BaseButton>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, isVNode, type VNode } from 'vue'
import emitter from '@/utils/eventBus'
import BaseModal from './BaseModal.vue'
import BaseButton from './BaseButton.vue'

// 类型定义
interface ConfirmOptions {
  title?: string
  message?: string | VNode
  confirmText?: string
  cancelText?: string
  type?: 'primary' | 'danger'
  onConfirm?: () => Promise<void> | void
  onCancel?: () => void
}

// 弹窗状态
const visible = ref(false)
const title = ref('操作确认')
const message = ref('确定要执行此操作吗？')
const confirmText = ref('确定')
const cancelText = ref('取消')
const type = ref<'primary' | 'danger'>('primary')
const loading = ref(false)

// VNode 相关
const isVNodeRef = ref(false)
const messageVNode = ref<VNode | null>(null)

// 回调函数
let confirmCallback: (() => Promise<void> | void) | null = null
let cancelCallback: (() => void) | null = null

// 显示确认弹窗
function showConfirm(options: ConfirmOptions) {
  // 重置状态
  resetState()

  // 设置选项
  title.value = options.title ?? '操作确认'
  confirmText.value = options.confirmText ?? '确定'
  cancelText.value = options.cancelText ?? '取消'
  type.value = options.type ?? 'primary'
  confirmCallback = options.onConfirm ?? null
  cancelCallback = options.onCancel ?? null

  // 处理消息内容
  handleMessage(options.message)

  // 显示弹窗
  visible.value = true
}

// 处理消息内容
function handleMessage(msg?: string | VNode) {
  if (isVNode(msg)) {
    isVNodeRef.value = true
    messageVNode.value = msg
  } else {
    isVNodeRef.value = false
    message.value = msg ?? '确定要执行此操作吗？'
  }
}

// 重置状态
function resetState() {
  loading.value = false
  isVNodeRef.value = false
  messageVNode.value = null
}

// 取消操作
function onCancel() {
  if (loading.value) return

  visible.value = false
  cancelCallback?.()
  resetState()
}

// 确认操作
async function handleConfirm() {
  if (loading.value) return

  loading.value = true

  try {
    await confirmCallback?.()
    visible.value = false
    resetState()
  } catch (error) {
    // 如果确认操作失败，不关闭弹窗，让用户可以重试
    console.error('Confirm operation failed:', error)
  } finally {
    loading.value = false
  }
}

// 键盘事件处理
function onKeydown(e: KeyboardEvent) {
  if (!visible.value) return

  switch (e.key) {
    case 'Escape':
      onCancel()
      break
    case 'Enter':
      if (e.ctrlKey || e.metaKey) {
        handleConfirm()
      }
      break
  }
}

// 生命周期
onMounted(() => {
  emitter.on('confirm', showConfirm)
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  emitter.off('confirm', showConfirm)
  window.removeEventListener('keydown', onKeydown)
})
</script>