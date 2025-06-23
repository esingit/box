<!-- src/views/Home.vue -->
<template>
  <header class="flex bg-white items-center justify-center gap-2 px-4 py-2">
    <Package
        class="cursor-pointer transition-transform duration-500"
        :class="{ 'rotate-[360deg]': isRotating }"
        :size="40"
        @click="rotateBox"
    />
    <h1 class="text-2xl font-bold">BOX</h1>
  </header>
  <div v-if="isLoggedIn" class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-6 rounded-xl">
    <main class="flex-1 overflow-auto">
      <!-- 用户已登录时显示看板 -->
      <Dashboard/>
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
