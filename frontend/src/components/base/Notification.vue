<template>
  <!-- 通知容器 -->
  <div
      class="fixed top-4 right-4 flex flex-col space-y-2 z-50"
      style="min-width: 250px;"
  >
    <transition-group name="toast" tag="div">
      <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
          'rounded-md p-4 shadow-md text-white cursor-pointer select-none',
          toastClass(toast.type)
        ]"
          @click="removeToast(toast.id)"
      >
        {{ toast.message }}
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onBeforeUnmount } from 'vue'
import emitter from '@/utils/eventBus'

interface Toast {
  id: number
  message: string
  type: 'success' | 'info' | 'warning' | 'error'
  duration: number
}

const toasts = reactive<Toast[]>([])

let idCounter = 0

function toastClass(type: Toast['type']) {
  switch (type) {
    case 'success':
      return 'bg-green-500'
    case 'warning':
      return 'bg-yellow-500'
    case 'error':
      return 'bg-red-600'
    default:
      return 'bg-blue-500'
  }
}

function addToast(payload: string | { message: string; type?: string; duration?: number }) {
  let message = ''
  let type: Toast['type'] = 'info'
  let duration = 2000

  if (typeof payload === 'string') {
    message = payload
  } else if (payload && typeof payload === 'object') {
    message = payload.message || ''
    if (['success', 'info', 'warning', 'error'].includes(payload.type || '')) {
      type = payload.type as Toast['type']
    }
    duration = payload.duration || 2000
  }

  const id = ++idCounter
  toasts.push({ id, message, type, duration })

  setTimeout(() => {
    removeToast(id)
  }, duration)
}

function removeToast(id: number) {
  const index = toasts.findIndex(t => t.id === id)
  if (index !== -1) {
    toasts.splice(index, 1)
  }
}

function onNotify(payload: string | { message: string; type?: string; duration?: number }) {
  addToast(payload)
}

onMounted(() => {
  emitter.on('notify', onNotify)
})

onBeforeUnmount(() => {
  emitter.off('notify', onNotify)
})
</script>

<style scoped>
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
</style>
