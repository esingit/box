<template>
  <transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
  >
    <div
        v-if="visible"
        class="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50"
        @click.self="onCancel"
        tabindex="0"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg shadow-lg w-full max-w-sm p-6 relative">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold text-gray-900 dark:text-gray-100">{{ title }}</h3>
          <button
              class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              @click="onCancel"
              title="关闭"
              type="button"
          >
            <LucideX size="20" />
          </button>
        </div>
        <div class="mb-6 whitespace-pre-line text-gray-700 dark:text-gray-300">{{ message }}</div>
        <div class="flex justify-end space-x-3">
          <button
              class="px-4 py-2 rounded border border-gray-300 hover:bg-gray-100 dark:border-gray-600 dark:hover:bg-gray-700 transition"
              @click="onCancel"
              :disabled="loading"
              type="button"
          >
            {{ cancelText }}
          </button>
          <button
              class="px-4 py-2 rounded bg-blue-600 text-white hover:bg-blue-700 disabled:bg-blue-400 disabled:cursor-not-allowed transition"
              :class="type === 'danger' ? 'bg-red-600 hover:bg-red-700 disabled:bg-red-400' : ''"
              @click="handleConfirm"
              :disabled="loading"
              type="button"
          >
            {{ loading ? '处理中...' : confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { LucideX } from 'lucide-vue-next'
import emitter from '@/utils/eventBus.ts'

const visible = ref(false)
const title = ref('操作确认')
const message = ref('确定要执行此操作吗？')
const confirmText = ref('确定')
const cancelText = ref('取消')
const type = ref('primary')
const loading = ref(false)

let confirmCallback = null
let cancelCallback = null

function showConfirm(opts) {
  title.value = opts.title || '操作确认'
  message.value = opts.message || '确定要执行此操作吗？'
  confirmText.value = opts.confirmText || '确定'
  cancelText.value = opts.cancelText || '取消'
  type.value = opts.type || 'primary'
  confirmCallback = opts.onConfirm
  cancelCallback = opts.onCancel
  visible.value = true
  loading.value = false
  focusTrap()
}

function onCancel() {
  if (loading.value) return
  visible.value = false
  cancelCallback && cancelCallback()
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

function onKeydown(e) {
  if (e.key === 'Escape') {
    onCancel()
  }
}

// 绑定 esc 全局监听
onMounted(() => {
  emitter.on('confirm', showConfirm)
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  emitter.off('confirm', showConfirm)
  window.removeEventListener('keydown', onKeydown)
})

// 简单聚焦处理，避免按键无法触发
function focusTrap() {
  setTimeout(() => {
    const container = document.querySelector('.fixed.inset-0')
    if (container) container.focus()
  }, 50)
}
</script>
