<template>
  <div class="user-menu-container">
    <button class="user-menu-btn" @click="$emit('show-login')">
      <LucideLogIn class="btn-icon" />
      登录
    </button>

    <span class="divider-vertical"></span>

    <button class="user-menu-btn" @click="$emit('show-register')">
      <LucideUserPlus class="btn-icon" />
      注册
    </button>
  </div>
</template>

<script setup lang="ts">
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next'

defineEmits(['show-login', 'show-register'])
</script>

<style scoped>
.user-menu-container {
  display: flex;
  align-items: center;
  user-select: none;
  background-color: var(--bg-main);
  padding: 4px 12px;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-sm);
}

.user-menu-btn {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-xs) var(--space-md);
  border: none;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
}

.user-menu-btn:hover {
  background-color: var(--bg-hover);
  border-radius: var(--radius-full);
}
</style>
