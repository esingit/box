<template>
  <div class="auth-form-container">
    <h2>注册</h2>
    <RegisterForm />
  </div>
</template>

<script setup>
import RegisterForm from '../components/RegisterForm.vue'
// 可以在这里添加错误处理逻辑，检查请求返回的 400 错误信息
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