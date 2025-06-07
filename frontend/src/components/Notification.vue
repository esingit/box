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
import mitt from '@/utils/eventBus'

const visible = ref(false)
const message = ref('')
const type = ref('success')
let timer = null

function show(msg, t = 'success', duration = 2000) {
  message.value = msg
  type.value = t
  visible.value = true
  clearTimeout(timer)
  timer = setTimeout(() => {
    visible.value = false
  }, duration)
}

onMounted(() => {
  mitt.on('notify', show)
})
</script>
