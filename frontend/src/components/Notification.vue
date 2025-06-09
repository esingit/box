<template>
  <div class="toast-container">
    <transition name="fade">
      <div v-if="visible" :class="['toast', `toast-${type}`, 'flex-center']">
        {{ message }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import emitter from '@/utils/eventBus'

const visible = ref(false)
const message = ref('')
const type = ref('success')
let timer = null

function show(msgOrObj, t = 'success', duration = 2000) {
  if (typeof msgOrObj === 'object' && msgOrObj !== null) {
    message.value = msgOrObj.message || ''
    type.value = msgOrObj.type || 'success'
    duration = msgOrObj.duration || duration
  } else {
    message.value = msgOrObj
    type.value = t
  }
  visible.value = true
  clearTimeout(timer)
  timer = setTimeout(() => {
    visible.value = false
  }, duration)
}

onMounted(() => {
  emitter.on('notify', show)
})
</script>
