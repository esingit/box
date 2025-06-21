<!-- components/layout/Sidebar.vue -->
<template>
  <aside :class="sidebarClass">
    <!-- Logo -->
    <div
        class="h-12 flex items-center justify-center border-gray-200 cursor-pointer relative"
        @click="toggleSidebar"
    >
      <Package
          :size="24"
          :class="logoClass"
      />
    </div>

    <!-- Menu -->
    <nav class="flex-1 overflow-y-auto py-2">
      <router-link
          v-for="(item, index) in menuItems"
          :key="index"
          :to="item.path"
          class="menu-btn"
          :class="getItemClass(item.path)"
      >
        <component :is="item.icon" :size="20"
                   :class="{ 'opacity-0 scale-0 transition-all duration-500': isCollapsed }"
        />
        <span class="whitespace-nowrap">{{ item.title }}</span>
      </router-link>
    </nav>
  </aside>

  <!-- 悬浮的箱子图标 -->
  <div
      v-if="isCollapsed"
      class="fixed top-3 left-3 z-50 cursor-pointer animate-bounce-in"
      @click="toggleSidebar"
  >
    <Package
        :size="24"
        class="text-gray-800 hover:text-gray-600 transition-colors drop-shadow-md"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { useRoute } from 'vue-router'
import {
  LucideHome,
  Package,
  LucideDumbbell,
  LucideWallet
} from 'lucide-vue-next'

const props = defineProps<{ isLoggedIn: boolean }>()

const route = useRoute()
const isRotating = ref(false)
const isCollapsed = ref(false)

// 注入父组件的方法
const setSidebarCollapsed = inject('setSidebarCollapsed') as (collapsed: boolean) => void

const menuItems = [
  { path: '/', title: '首页', icon: LucideHome },
  { path: '/fitness', title: '健身', icon: LucideDumbbell },
  { path: '/asset', title: '资产', icon: LucideWallet }
]

const sidebarClass = computed(() =>
    [
      'h-full bg-[var(--bg-sub)] text-gray-800 flex flex-col transition-all duration-500 ease-in-out border-r border-gray-200 fixed z-40',
      isCollapsed.value ? 'w-0  overflow-visible' : 'w-32'
    ].join(' ')
)

const logoClass = computed(() => [
  'text-gray-800 transition-all duration-500',
  isRotating.value && 'animate-spin',
  isCollapsed.value && 'opacity-0 scale-0'
].filter(Boolean).join(' '))

const getItemClass = (path: string) => ({
  'bg-gray-100': route.path === path,
})

function toggleSidebar() {
  if (!props.isLoggedIn) return
  isRotating.value = true
  isCollapsed.value = !isCollapsed.value

  // 通知父组件侧边栏状态变化
  if (setSidebarCollapsed) {
    setSidebarCollapsed(isCollapsed.value)
  }

  setTimeout(() => {
    isRotating.value = false
  }, 500)
}
</script>

<style scoped>
@keyframes bounce-in {
  0% {
    transform: translateX(-100px) scale(0);
    opacity: 0;
  }
  50% {
    transform: translateX(-10px) scale(1.1);
    opacity: 1;
  }
  100% {
    transform: translateX(0) scale(1);
    opacity: 1;
  }
}

.animate-bounce-in {
  animation: bounce-in 0.6s ease-out forwards;
}
</style>