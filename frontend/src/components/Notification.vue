<template>
  <transition name="fade">
    <div v-if="visible" :class="['notification', type]">
      {{ message }}
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import mitt from '../utils/eventBus'

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

<style scoped>
.notification {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  min-width: 180px;
  max-width: 80vw;
  background: #f7f7f8;
  color: #222;
  border-radius: 14px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10);
  padding: 16px 40px;
  font-size: 16px;
  font-weight: 500;
  z-index: 3000;
  text-align: center;
  border: 1.5px solid #e0e0e0;
  letter-spacing: 0.5px;
  transition: all 0.2s;
  opacity: 0.98;
  backdrop-filter: blur(2px);
}
.notification.success {
  color: #10b981;
  border-color: #e0e0e0;
  background: linear-gradient(90deg, #f7f7f8 80%, #ededed 100%);
}
.notification.error {
  color: #e74c3c;
  border-color: #e0e0e0;
  background: linear-gradient(90deg, #f7f7f8 80%, #ededed 100%);
}
.notification.info {
  color: #888;
  border-color: #e0e0e0;
  background: linear-gradient(90deg, #f7f7f8 80%, #ededed 100%);
}
.notification.warning {
  color: #b3a369;
  border-color: #e0e0e0;
  background: linear-gradient(90deg, #f7f7f8 80%, #ededed 100%);
}
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}
</style>
