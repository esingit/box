<template>
  <div ref="menuRef" class="relative select-none">
    <div class="menu-head">
      <BaseButton
          @click="toggleMenu"
          type="button"
          color="text"
          aria-haspopup="true"
          :aria-expanded="showMenu"
      >
        <LucideUser class="btn-icon"/>
        <span class="font-semibold max-w-[120px] truncate">{{ username }}</span>
        <LucideChevronDown
            class="w-4 h-4 text-gray-500 transition-transform duration-300"
            :style="{ transform: showMenu ? 'rotate(180deg)' : 'rotate(0deg)' }"
        />
      </BaseButton>
    </div>

    <transition name="fade-scale">
      <div
          v-if="showMenu"
          class="absolute right-0 mt-2 w-48 bg-white shadow-lg rounded-xl z-50 p-2"
          role="menu"
      >
        <BaseButton
            type="button"
            title="设置"
            color="text"
            block
            :icon="LucideUserCircle"
            @click="onOpenProfile"
        />
        <BaseButton
            type="button"
            title="注销"
            color="text"
            block
            :icon="LucideLogOut"
            @click="onLogout"
        />
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
