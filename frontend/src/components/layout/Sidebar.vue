<template>
  <!-- 折叠侧边栏 -->
  <aside :class="sidebarClass">
    <!-- 顶部 Logo 区 -->
    <div
        class="h-12 flex items-center justify-center cursor-pointer relative"
        @click="toggleSidebar"
    >
      <Package
          :size="24"
          :class="logoClass"
      />
    </div>

    <!-- 菜单列表 -->
    <nav class="flex-1 overflow-y-auto space-y-1 mt-2">
      <router-link
          v-for="(item, index) in menuItems"
          :key="index"
          :to="item.path"
          class="menu-btn m-2 gap-2"
          :class="getItemClass(item.path)"
      >
        <component :is="item.icon" :size="20" />
        <span
            class="whitespace-nowrap transition-all"
            :class="{ 'opacity-0 w-0 scale-0': !sidebarStore.isExpanded }"
        >
          {{ item.title }}
        </span>
      </router-link>
    </nav>
  </aside>

  <!-- 悬浮图标：收起时展示 -->
  <div
      v-if="!sidebarStore.isExpanded"
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
import { Package, LucideHome, LucideDumbbell, LucideWallet } from 'lucide-vue-next'
import { useSidebarStore } from '@/store/sidebarStore'

// 注入登录状态
const props = defineProps<{ isLoggedIn: boolean }>()

const route = useRoute()
const sidebarStore = useSidebarStore()
const isRotating = ref(false)

// 通知父组件的 inject 函数
const setSidebarCollapsed = inject('setSidebarCollapsed') as (collapsed: boolean) => void

// 菜单数据
const menuItems = [
  { path: '/', title: '首页', icon: LucideHome },
  { path: '/fitness', title: '健身', icon: LucideDumbbell },
  { path: '/asset', title: '资产', icon: LucideWallet }
]

// 容器样式
const sidebarClass = computed(() => [
  'h-full bg-white border-r border-gray-200 text-gray-800 flex flex-col transition-all duration-300 ease-in-out fixed z-40',
  !sidebarStore.isExpanded ? 'w-0 overflow-visible' : 'w-32'
].join(' '))

// Logo 图标样式
const logoClass = computed(() => [
  'text-gray-800 transition-all duration-300',
  isRotating.value && 'animate-spin',
  !sidebarStore.isExpanded && 'opacity-0 scale-0'
].filter(Boolean).join(' '))

// 当前激活菜单项
const getItemClass = (path: string) => ({
  'bg-[var(--bg-btn-hover)]': route.path === path,
})

// 切换收起/展开
function toggleSidebar() {
  if (!props.isLoggedIn) return
  sidebarStore.toggleSidebar()
  isRotating.value = true

  // 通知父组件
  setSidebarCollapsed?.(!sidebarStore.isExpanded)

  setTimeout(() => {
    isRotating.value = false
  }, 400)
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
  animation: bounce-in 0.5s ease-out forwards;
}
</style>