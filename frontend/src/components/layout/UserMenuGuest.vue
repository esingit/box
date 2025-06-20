<!-- components/layout/UserMenuGuest.vue -->
<template>
  <div class="menu-head">
    <BaseButton
        title="登录"
        color="text"
        :icon="LucideLogIn"
        @click="$emit('show-login')"
    />

    <div class="vertical-line"></div>

    <BaseButton
        title="注册"
        color="text"
        :icon="LucideUserPlus"
        @click="$emit('show-register')"
    />
  </div>
</template>

<script setup lang="ts">
import { LucideLogIn, LucideUserPlus } from 'lucide-vue-next'
defineEmits(['show-login', 'show-register'])
</script>
