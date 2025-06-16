<template>
  <LoginModal
      v-model:visible="showLogin"
      @login-success="emit('login-success')"
      @switch-to-register="switchToRegister"
  />

  <RegisterModal
      v-model:visible="showRegister"
      @register-success="emit('register-success')"
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

const emit = defineEmits([
  'update:showLogin',
  'update:showRegister',
  'login-success',
  'register-success'
])

const showLogin = computed({
  get: () => props.showLogin,
  set: (val) => emit('update:showLogin', val)
})

const showRegister = computed({
  get: () => props.showRegister,
  set: (val) => emit('update:showRegister', val)
})

function switchToRegister() {
  if (showLogin.value) {
    showLogin.value = false
    showRegister.value = true
  }
}

function switchToLogin() {
  if (showRegister.value) {
    showRegister.value = false
    showLogin.value = true
  }
}
</script>
