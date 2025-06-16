<template>
  <!-- 这个组件不渲染任何内容 -->
  <div style="display:none;"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount } from 'vue'
import emitter from '@/utils/eventBus'
import { useNotification } from 'naive-ui'

const notification = useNotification()

function onNotify(payload: string | { message: string; type?: string; duration?: number }) {
  let message = ''
  let type = 'info'
  let duration = 2000

  if (typeof payload === 'string') {
    message = payload
  } else if (payload && typeof payload === 'object') {
    message = payload.message || ''
    type = payload.type || 'info'
    duration = payload.duration || 2000
  }

  // Naive UI 的通知类型映射
  const notifyTypes = ['success', 'info', 'warning', 'error']
  if (!notifyTypes.includes(type)) type = 'info'

  notification[type]({
    content: message,
    duration
  })
}

onMounted(() => {
  emitter.on('notify', onNotify)
})

onBeforeUnmount(() => {
  emitter.off('notify', onNotify)
})
</script>
