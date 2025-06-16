<template>
  <div
      class="user-menu-container"
      ref="menuRef"
      v-click-outside="handleOutsideClick"
  >
    <button class="user-button" @click="toggleMenu">
      <LucideUser class="icon" />
      <span class="username">{{ username }}</span>
      <LucideChevronDown
          class="arrow"
          size="16"
          :style="{ transform: showMenu ? 'rotate(180deg)' : 'rotate(0deg)' }"
      />
    </button>

    <transition name="fade-scale">
      <div v-if="showMenu" class="dropdown-menu">
        <button class="dropdown-item" @click="emit('open-profile')">
          <LucideUserCircle size="18" class="dropdown-item-icon" /> 设置
        </button>
        <button class="dropdown-item" @click="emit('logout')">
          <LucideLogOut size="18" class="dropdown-item-icon" /> 注销
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import {
  LucideUser,
  LucideUserCircle,
  LucideLogOut,
  LucideChevronDown
} from 'lucide-vue-next'
import { ref } from 'vue'
import { onClickOutside } from '@vueuse/core'

const props = defineProps({
  user: Object
})
const emit = defineEmits(['open-profile', 'logout'])

const menuRef = ref(null)
const showMenu = ref(false)

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function handleOutsideClick() {
  showMenu.value = false
}

onClickOutside(menuRef, handleOutsideClick)

const username = computed(() => props.user?.username || '用户')
</script>

<style scoped>
.user-menu-container {
  position: relative;
  display: inline-block;
  user-select: none;
}

.user-button {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: transparent;
  border: none;
  cursor: pointer;
  padding: 6px 12px;
  font-size: 14px;
  color: #444;
  border-radius: var(--radius-full);
  transition: background-color 0.2s ease;
}

.user-button:hover {
  background-color: #f0f0f0;
}

.icon {
  stroke: #666;
  width: 20px;
  height: 20px;
}

.username {
  font-weight: 600;
  white-space: nowrap;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.arrow {
  stroke: #666;
  transition: transform 0.3s ease;
}

.dropdown-menu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 8px;
  min-width: 140px;
  background: #fff;
  box-shadow: 0 8px 16px rgb(0 0 0 / 0.1);
  border-radius: 6px;
  padding: 8px 0;
  z-index: 10000;
  user-select: none;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  color: #333;
  background: transparent;
  border: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;
  border-radius: 4px;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-item-icon {
  stroke: #888;
  width: 18px;
  height: 18px;
}
</style>
