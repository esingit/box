<template>
  <aside class="sidebar" :class="{ 'collapsed': isCollapsed }">
    <div class="logo" @click="toggleSidebar">
      <LucideBox class="logo-icon" :class="{ 'rotate': isRotating }" />
    </div>
    <nav class="menu">
      <router-link to="/" class="menu-item" active-class="active">
        <LucideHome class="icon" /> <span class="menu-text">首页</span>
      </router-link>
      <router-link to="/fitness" class="menu-item" active-class="active">
        <LucideDumbbell class="icon" /> <span class="menu-text">健身</span>
      </router-link>
      <router-link to="/asset" class="menu-item" active-class="active">
        <LucideWallet class="icon" /> <span class="menu-text">资产</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { LucideHome, LucideBox, LucideDumbbell, LucideWallet } from 'lucide-vue-next'

const props = defineProps({
  isLoggedIn: {
    type: Boolean,
    default: false
  }
})

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
