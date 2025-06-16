<!-- src/views/Home.vue -->
<template>
  <div class="flex flex-col h-full">
    <header class="flex items-center justify-center gap-2 px-4 py-3">
    <Package
          class="cursor-pointer transition-transform duration-500"
          :class="{ 'rotate-[360deg]': isRotating }"
          :size="40"
          @click="rotateBox"
      />
      <h1 class="text-2xl font-bold">BOX</h1>
    </header>

    <main class="flex-1 overflow-auto p-4">
      <Dashboard v-if="userStore.isLoggedIn" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Package } from 'lucide-vue-next'
import Dashboard from '@/components/dashboard/Dashboard.vue'
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const isRotating = ref(false)

function applyRotationAnimation(duration = 1000) {
  isRotating.value = true
  setTimeout(() => {
    isRotating.value = false
  }, duration)
}

function rotateBox() {
  applyRotationAnimation()
}
</script>

<style scoped>
.rotate-\[360deg\] {
  transform: rotate(360deg);
}
</style>
