<template>
  <div class="container flex-center">
    <div class="dashboard-wrapper">
      <div class="flex items-center justify-center mb-xl"
          @click="rotateBox">
        <LucideBox
            class="title-icon cursor-pointer mr-2"
            :class="{ 'animate-spin': isRotating }"
            :size="40"
        />
        <h1 class="page-title">BOX</h1>
      </div>
      <DashboardPanel v-if="userStore.isLoggedIn"/>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import {LucideBox} from 'lucide-vue-next';
import DashboardPanel from '@/components/dashboard/DashboardPanel.vue';
import {useUserStore} from '@/stores/userStore';

const userStore = useUserStore();
const isRotating = ref(false);

function rotateBox() {
  isRotating.value = true;
  setTimeout(() => {
    isRotating.value = false;
  }, 1000);
}
</script>

