<!--/components/base/BaseNotice.vue-->
<template>
  <div
      class="fixed top-8 left-1/2 transform -translate-x-1/2 flex flex-col space-y-2 z-50 min-w-[250px]"
  >
    <transition-group name="toast" tag="div">
      <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
          'msg-block msg-block-transparent',
          toastTextClass(toast.type)
        ]"
          @click="removeToast(toast.id)"
          @mouseenter="pauseToast(toast.id)"
          @mouseleave="resumeToast(toast.id)"
      >
        <span>{{ toast.message }}</span>
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
  timeoutId?: ReturnType<typeof setTimeout>
  remaining?: number
  startTime?: number
}

const MAX_TOASTS = 5
const toasts = reactive<Toast[]>([])

let idCounter = 0

// 字体颜色根据类型分配
function toastTextClass(type: Toast['type']) {
  switch (type) {
    case 'success': return 'msg-success'
    case 'warning': return 'msg-warning'
    case 'error': return 'msg-error'
    default: return 'msg-info'
  }
}

function removeToast(id: number) {
  const index = toasts.findIndex(t => t.id === id)
  if (index !== -1) {
    clearTimeout(toasts[index].timeoutId)
    toasts.splice(index, 1)
  }
}

function setAutoRemove(toast: Toast) {
  toast.startTime = Date.now()
  toast.timeoutId = setTimeout(() => {
    removeToast(toast.id)
  }, toast.duration)
  toast.remaining = toast.duration
}

function addToast(payload: string | { message: string; type?: string; duration?: number }) {
  let message = ''
  let type: Toast['type'] = 'info'
  let duration = 3000

  if (typeof payload === 'string') {
    message = payload
  } else if (payload && typeof payload === 'object') {
    message = payload.message || ''
    if (['success', 'info', 'warning', 'error'].includes(payload.type || '')) {
      type = payload.type as Toast['type']
    }
    duration = payload.duration || 3000
  }

  if (toasts.find(t => t.message === message)) return

  if (toasts.length >= MAX_TOASTS) {
    removeToast(toasts[0].id)
  }

  const id = ++idCounter
  const newToast: Toast = { id, message, type, duration }
  toasts.push(newToast)
  setAutoRemove(newToast)
}

function pauseToast(id: number) {
  const toast = toasts.find(t => t.id === id)
  if (!toast || !toast.timeoutId) return

  clearTimeout(toast.timeoutId)
  const elapsed = Date.now() - (toast.startTime || 0)
  toast.remaining = (toast.remaining || toast.duration) - elapsed
}

function resumeToast(id: number) {
  const toast = toasts.find(t => t.id === id)
  if (!toast) return

  toast.startTime = Date.now()
  toast.timeoutId = setTimeout(() => {
    removeToast(toast.id)
  }, toast.remaining || 2000)
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
  transform: translateY(-100%);
}
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
</style>v
