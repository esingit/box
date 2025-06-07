<template>
  <div>
    <button class="user-menu-btn" @click="$emit('toggle')" @mousedown.stop>
      <User :size="18" class="user-icon" /> {{ username }}
    </button>
    <transition name="fade">
      <div v-if="showMenu" class="dropdown-menu" @mousedown.stop>
        <button class="dropdown-item" @click="$emit('open-profile')">
          <UserCircle :size="16" class="dropdown-item-icon" />设置
        </button>
        <button class="dropdown-item" @click="$emit('logout')">
          <LogOut :size="16" class="dropdown-item-icon" />注销
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { User, UserCircle, LogOut } from 'lucide-vue-next';

defineProps({
  username: String,
  showMenu: Boolean
});

defineEmits(['toggle', 'open-profile', 'logout']);
</script>
