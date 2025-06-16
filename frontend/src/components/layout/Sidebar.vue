<template>
  <aside class="sidebar" :class="{ 'collapsed': isCollapsed }">
    <div class="logo" @click="toggleSidebar">
      <Package class="logo-icon" :class="{ 'animate-spin': isRotating }" :size="24" />
    </div>
    <nav class="menu">
      <router-link 
        v-for="(item, index) in menuItems" 
        :key="index"
        :to="item.path" 
        class="menu-item" 
        :class="{ 'active': $route.path === item.path }"
      >
        <component 
          :is="item.icon" 
          class="menu-icon" 
          :size="20"
        />
        <span class="menu-text">{{ item.title }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { LucideHome, Package, LucideDumbbell, LucideWallet } from 'lucide-vue-next'

const props = defineProps({
  isLoggedIn: {
    type: Boolean,
    default: false
  }
})

const menuItems = [
  { path: '/', title: '首页', icon: LucideHome },
  { path: '/fitness', title: '健身', icon: LucideDumbbell },
  { path: '/asset', title: '资产', icon: LucideWallet }
]

const isRotating = ref(false)
const isCollapsed = ref(false)

function toggleSidebar() {
  if (props.isLoggedIn) {
    isRotating.value = true
    isCollapsed.value = !isCollapsed.value
    setTimeout(() => {
      isRotating.value = false
    }, 500)
  }
}
</script>
