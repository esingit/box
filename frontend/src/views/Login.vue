<template>
  <div class="auth-form-container">
    <h2>登录</h2>
    <LoginForm />
  </div>
</template>

<script setup>
import LoginForm from '../components/LoginForm.vue'
</script>

<style scoped>
.auth-form-container {
  max-width: 360px;
  margin: 40px auto;
  padding: 30px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 12px #ccc;
}
</style>