<template>
  <div class="user-menu-container">
    <button class="user-menu-btn" @click="$emit('toggle')">
      <LucideUser :size="18" /> {{ username }}
    </button>
    <div v-if="showMenu" class="dropdown-menu">
      <button class="dropdown-item" @click="$emit('open-profile')">
        <LucideUserCircle :size="16" class="dropdown-item-icon" />设置
      </button>
      <button class="dropdown-item" @click="$emit('logout')">
        <LucideLogOut :size="16" class="dropdown-item-icon" />注销
      </button>
    </div>
  </div>
</template>

<script setup>
import { LucideUser, LucideUserCircle, LucideLogOut } from 'lucide-vue-next';

defineProps({
  username: String,
  showMenu: Boolean
});

defineEmits(['toggle', 'open-profile', 'logout']);
</script>