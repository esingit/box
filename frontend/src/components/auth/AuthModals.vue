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
  set: (v) => emit('update:showLogin', v)
})

const showRegister = computed({
  get: () => props.showRegister,
  set: (v) => emit('update:showRegister', v)
})

function switchToRegister() {
  showLogin.value = false
  showRegister.value = true
}

function switchToLogin() {
  showRegister.value = false
  showLogin.value = true
}
</script>
