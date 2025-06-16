<!-- components/layout/Sidebar.vue -->
<template>
  <aside
      :class="[
      'h-full bg-gray-800 text-white flex flex-col transition-all duration-300',
      isCollapsed ? 'w-16' : 'w-52'
    ]"
  >
    <!-- Logo -->
    <div
        class="h-16 flex items-center justify-center border-b border-gray-700 cursor-pointer"
        @click="toggleSidebar"
    >
      <Package
          :size="24"
          class="text-white transition-transform duration-500"
          :class="{ 'animate-spin': isRotating }"
      />
    </div>

    <!-- Menu -->
    <nav class="flex-1 overflow-y-auto py-2">
      <router-link
          v-for="(item, index) in menuItems"
          :key="index"
          :to="item.path"
          class="flex items-center gap-3 px-4 py-2 text-sm hover:bg-gray-700 transition-colors"
          :class="{
          'bg-gray-700 font-semibold': $route.path === item.path,
          'justify-center': isCollapsed
        }"
      >
        <component :is="item.icon" :size="20" />
        <span v-if="!isCollapsed" class="whitespace-nowrap">{{ item.title }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  LucideHome,
  Package,
  LucideDumbbell,
  LucideWallet
} from 'lucide-vue-next'

const props = defineProps<{
  isLoggedIn: boolean
}>()

const route = useRoute()
const isRotating = ref(false)
const isCollapsed = ref(false)

const menuItems = [
  { path: '/', title: '首页', icon: LucideHome },
  { path: '/fitness', title: '健身', icon: LucideDumbbell },
  { path: '/asset', title: '资产', icon: LucideWallet }
]

function toggleSidebar() {
  if (!props.isLoggedIn) return
  isRotating.value = true
  isCollapsed.value = !isCollapsed.value
  setTimeout(() => {
    isRotating.value = false
  }, 500)
}
</script>

<style scoped>
/* optional: disable spin animation interruption */
.animate-spin {
  animation: spin 0.5s linear;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
