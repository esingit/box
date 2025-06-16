<!-- AuthModals.vue -->
<script setup>
import { computed } from 'vue'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'

const props = defineProps({
  showLogin: Boolean,
  showRegister: Boolean
})
const emit = defineEmits([
  'update:showLogin',
  'update:showRegister',
  'login-success',
  'register-success'
])

const showLogin = computed({
  get: () => props.showLogin,
  set: val => emit('update:showLogin', val)
})

const showRegister = computed({
  get: () => props.showRegister,
  set: val => emit('update:showRegister', val)
})
</script>

<template>
  <LoginForm
      v-model:visible="showLogin"
      @login-success="emit('login-success', $event)"
  />
  <RegisterForm
      v-model:visible="showRegister"
      @register-success="emit('register-success', $event)"
  />
</template>
