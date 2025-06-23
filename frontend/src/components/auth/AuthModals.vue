<!-- src/components/auth/AuthModals.vue -->
<template>
  <LoginModal
      :visible="showLogin"
      @update:visible="updateShowLogin"
      @login-success="handleLoginSuccess"
      @switch-to-register="switchToRegister"
  />

  <RegisterModal
      :visible="showRegister"
      @update:visible="updateShowRegister"
      @register-success="handleRegisterSuccess"
      @switch-to-login="switchToLogin"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import LoginModal from './LoginModal.vue'
import RegisterModal from './RegisterModal.vue'

const props = defineProps<{
  showLogin: boolean
  showRegister: boolean
}>()

// 🔥 使用 TypeScript 类型定义 emits
const emit = defineEmits<{
  'update:showLogin': [value: boolean]
  'update:showRegister': [value: boolean]
  'login-success': []
  'register-success': []
}>()

const showLogin = computed({
  get: () => props.showLogin,
  set: (val) => emit('update:showLogin', val)
})

const showRegister = computed({
  get: () => props.showRegister,
  set: (val) => emit('update:showRegister', val)
})

// 🔥 优化事件处理函数
function updateShowLogin(value: boolean) {
  showLogin.value = value
}

function updateShowRegister(value: boolean) {
  showRegister.value = value
}

function switchToRegister() {
  showLogin.value = false
  showRegister.value = true
}

function switchToLogin() {
  showRegister.value = false
  showLogin.value = true
}

function handleLoginSuccess() {
  // 登录成功后关闭所有弹窗
  showLogin.value = false
  showRegister.value = false
  emit('login-success')
}

function handleRegisterSuccess() {
  // 注册成功后关闭所有弹窗
  showLogin.value = false
  showRegister.value = false
  emit('register-success')
}
</script>