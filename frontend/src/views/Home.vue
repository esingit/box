<!-- src/views/Home.vue -->
<template>
  <div class="flex-col h-full">
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
      <!-- 用户已登录时显示看板 -->
      <Dashboard v-if="isLoggedIn"/>
    </main>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {Package} from 'lucide-vue-next'
import Dashboard from '@/components/dashboard/Dashboard.vue'
import {useAuth} from "@/composable/useAuth";

const auth = useAuth()

const {isLoggedIn} = auth
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
