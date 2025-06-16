<!-- components/layout/UserMenuGuest.vue -->
<template>
  <div class="flex items-center justify-end w-full px-3 py-1.5 relative select-none bg-white">
    <button
        @click="$emit('show-login')"
        class="flex items-center gap-1.5 px-3 py-1 text-sm text-gray-700 hover:bg-gray-100 rounded-full transition"
    >
      <LucideLogIn class="w-4 h-4 text-gray-500" />
      登录
    </button>

    <div class="w-px h-5 mx-2 bg-gray-300"></div>

    <button
        @click="$emit('show-register')"
        class="flex items-center gap-1.5 px-3 py-1 text-sm text-gray-700 hover:bg-gray-100 rounded-full transition"
    >
      <LucideUserPlus class="w-4 h-4 text-gray-500" />
      注册
    </button>
  </div>
</template>

<script setup lang="ts">
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next'
defineEmits(['show-login', 'show-register'])
</script>
