<template>
  <n-layout class="page-container" style="height: 100vh; background: transparent;">
    <n-layout-header class="header" style="display: flex; justify-content: center; align-items: center; gap: 8px; background: transparent;">
      <Package
          class="logo-icon"
          :class="{ rotate: isRotating }"
          :size="40"
          @click="rotateBox"
          style="cursor: pointer;"
      />
      <h1 class="page-title" style="margin: 0;">BOX</h1>
    </n-layout-header>

    <n-layout-content class="content-wrapper" style="display: flex; justify-content: center; align-items: center; background: transparent;">
      <Dashboard v-if="userStore.isLoggedIn" />
    </n-layout-content>
  </n-layout>
</template>

<script setup>
import { ref } from 'vue';
import { Package } from 'lucide-vue-next';
import { NLayout, NLayoutHeader, NLayoutContent } from 'naive-ui';
import Dashboard from '@/components/dashboard/Dashboard.vue';
import { useUserStore } from '@/store/userStore';

const userStore = useUserStore();
const isRotating = ref(false);

function rotateBox() {
  isRotating.value = true;
  setTimeout(() => {
    isRotating.value = false;
  }, 1000);
}
</script>

<style scoped>
@keyframes rotateAnimation {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.rotate {
  animation: rotateAnimation 1s linear;
}
</style>
