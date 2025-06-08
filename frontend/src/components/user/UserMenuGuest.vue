<template>
  <div class="flex gap-2">
    <button class="btn btn-text" @click="$emit('login')">
      <LucideLogIn :size="16" class="btn-icon" />
      登录
    </button>
    <button class="btn btn-text" @click="$emit('register')">
      <LucideUserPlus :size="16" class="btn-icon" />
      注册
    </button>
  </div>
</template>

<script setup>
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next';

defineEmits(['login', 'register']);
</script>