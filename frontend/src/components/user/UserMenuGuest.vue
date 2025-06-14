<template>
  <div class="user-menu-container u-flex u-items-center">
    <button class="user-menu-btn" @click="$emit('login')">
      <LucideLogIn class="btn-icon" />
      登录
    </button>
    <span class="divider-vertical"></span>
    <button class="user-menu-btn" @click="$emit('register')">
      <LucideUserPlus class="btn-icon" />
      注册
    </button>
  </div>
</template>

<script setup>
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next';
import '@/assets/styles/components.css';

defineEmits(['login', 'register']);
</script>