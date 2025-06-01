<template>
  <div>
    <h1 class="centered-title">
      <Box/>
      BOX
    </h1>
    <transition name="slide-fade">
      <div v-if="showAuth" class="auth-form-container">
        <LoginForm v-if="activeTab === 'login'"/>
        <RegisterForm v-else/>
      </div>
    </transition>
  </div>
</template>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {Box} from 'lucide-vue-next'
import LoginForm from '../components/LoginForm.vue'
import RegisterForm from '../components/RegisterForm.vue'
import {emitter} from '../utils/eventBus'

const activeTab = ref('login')
const showAuth = ref(false)

// 监听事件
onMounted(() => {
  emitter.on('show-auth', (type) => {
    activeTab.value = type
    showAuth.value = true
  })

  emitter.on('login-success', () => {
    showAuth.value = false
  })
})

// 清理事件监听
onUnmounted(() => {
  emitter.off('show-auth')
  emitter.off('login-success')
})
</script>
