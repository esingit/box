<template>
  <div class="user-menu-container flex items-center">
    <button class="user-menu-btn h-14 px-2" @click="$emit('login')">
      <LucideLogIn :size="18" class="btn-icon mr-1" />
      登录
    </button>
    <span class="divider-vertical"></span>
    <button class="user-menu-btn h-14 px-2" @click="$emit('register')">
      <LucideUserPlus :size="18" class="btn-icon mr-1" />
      注册
    </button>
  </div>
</template>

<script setup>
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next';
import '@/assets/components.css';

defineEmits(['login', 'register']);
</script>