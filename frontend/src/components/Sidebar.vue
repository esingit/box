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
      <router-link to="/inventory" class="menu-item" active-class="active">
        <LucideClipboardList class="icon" /> <span class="menu-text">盘点</span>
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { ref } from 'vue'
import { LucideHome, LucideBox, LucideDumbbell, LucideClipboardList } from 'lucide-vue-next'

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

<style scoped>
.sidebar {
  width: 220px;
  background: #f0f0f3;
  border-radius: 12px 0 0 12px;
  display: flex;
  flex-direction: column;
  padding: 20px 10px;
  box-sizing: border-box;
  transition: all 0.5s ease;
  position: relative;
}

.sidebar.collapsed {
  width: 0;
  padding: 20px 0;
}

.logo {
  padding-bottom: 20px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.5s ease;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.sidebar.collapsed .logo {
  left: 10px;
  transform: translateX(0);
  padding: 8px;
  border-radius: 8px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: #333;
  transition: all 0.5s ease;
}

.logo-icon.rotate {
  transform: rotate(360deg);
}

.menu {
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  gap: 15px;
  flex-grow: 1;
  transition: all 0.5s ease;
  overflow: hidden;
}

.sidebar.collapsed .menu {
  opacity: 0;
  visibility: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  color: #666;
  font-weight: 500;
  text-decoration: none;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s;
  white-space: nowrap;
}

.menu-item:hover {
  background-color: #e0e0e0;
  color: #666;
}

.menu-item.active {
  background-color: #d4d4d4;
  color: #333;
  font-weight: 500;
  box-shadow: none;
}

.menu-text {
  transition: all 0.5s ease;
}

.sidebar.collapsed .menu-text {
  opacity: 0;
  width: 0;
}

.icon {
  margin-right: 8px;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.logout-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  color: #666;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.logout-btn:hover {
  background-color: #ddd;
  color: #000;
}
</style>
