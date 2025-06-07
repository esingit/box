<template>
  <div>
    <button class="user-auth-link" @click="$emit('login')">
      <LogIn :size="18" />
      登录
    </button>
    <span class="auth-separator">|</span>
    <button class="user-auth-link" @click="$emit('register')">
      <UserPlus :size="18" />
      注册
    </button>
  </div>
</template>

<script setup>
import { LogIn, UserPlus } from 'lucide-vue-next';

defineEmits(['login', 'register']);
</script>
