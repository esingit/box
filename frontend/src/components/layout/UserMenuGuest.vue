<!-- components/layout/UserMenuGuest.vue -->
<template>
  <div class="menu-head">
    <button
        @click="$emit('show-login')"
        class="menu-btn-h"
    >
      <LucideLogIn class="btn-icon" />
      登录
    </button>

    <div class="vertical-line"></div>

    <button
        @click="$emit('show-register')"
        class="menu-btn-h"
    >
      <LucideUserPlus class="btn-icon" />
      注册
    </button>
  </div>
</template>

<script setup lang="ts">
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next'
defineEmits(['show-login', 'show-register'])
</script>
