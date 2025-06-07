<template>
  <div class="toast-container">
    <transition name="fade">
      <div v-if="visible" :class="['toast', `toast-${type}`]">
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

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
}

.toast {
  min-width: 200px;
  padding: 10px 20px;
  color: #fff;
  border-radius: 5px;
  margin-bottom: 10px;
  opacity: 0.9;
}

.toast-success {
  background-color: #4caf50;
}

.toast-error {
  background-color: #f44336;
}

.toast-info {
  background-color: #2196f3;
}

.toast-warning {
  background-color: #ff9800;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter, .fade-leave-to /* .fade-leave-active in <2.1.8 */ {
  opacity: 0;
}
</style>
