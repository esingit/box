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
  <div v-if="isLoggedIn">
      <!-- 用户已登录时显示看板 -->
      <Dashboard/>
  </div>
</template>

<script setup="ts">
import {ref} from 'vue'
import {storeToRefs} from 'pinia'
import {Package} from 'lucide-vue-next'
import Dashboard from '@/components/dashboard/Dashboard.vue'
import {useUserStore} from '@/store/userStore'

const userStore = useUserStore()
const {isLoggedIn} = storeToRefs(userStore)
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
