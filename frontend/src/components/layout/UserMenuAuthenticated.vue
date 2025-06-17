<template>
  <div ref="menuRef" class="relative select-none">
    <div class="menu-head">
      <button
          @click="toggleMenu"
          class="menu-btn-h"
          type="button"
          aria-haspopup="true"
          :aria-expanded="showMenu"
      >
        <LucideUser class="btn-icon"/>
        <span class="font-semibold max-w-[120px] truncate">{{ username }}</span>
        <LucideChevronDown
            class="w-4 h-4 text-gray-500 transition-transform duration-300"
            :style="{ transform: showMenu ? 'rotate(180deg)' : 'rotate(0deg)' }"
        />
      </button>
    </div>

    <transition name="fade-scale">
      <div
          v-if="showMenu"
          class="absolute right-0 mt-2 w-36 bg-white shadow-lg rounded-xl z-50 p-2"
          role="menu"
      >
        <button
            type="button"
            @click="onOpenProfile"
            class="menu-btn-h w-full"
            role="menuitem"
        >
          <LucideUserCircle class="btn-icon"/>
          设置
        </button>
        <button
            type="button"
            @click="onLogout"
            class="menu-btn-h w-full"
            role="menuitem"
        >
          <LucideLogOut class="btn-icon"/>
          注销
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import {
  LucideUser,
  LucideUserCircle,
  LucideLogOut,
  LucideChevronDown
} from 'lucide-vue-next'
import {ref, computed} from 'vue'
import {onClickOutside} from '@vueuse/core'

const props = defineProps<{ user: { username?: string } }>()
const emit = defineEmits(['open-profile', 'logout'])

const showMenu = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const username = computed(() => props.user?.username || '用户')

function toggleMenu() {
  showMenu.value = !showMenu.value
}

function closeMenu() {
  showMenu.value = false
}

function onOpenProfile() {
  emit('open-profile')
  closeMenu()
}

function onLogout() {
  emit('logout')
  closeMenu()
}

onClickOutside(menuRef, () => {
  if (showMenu.value) closeMenu()
})
</script>

<style scoped>
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.2s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
