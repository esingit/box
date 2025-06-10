<template>
  <div class="page-container">
    <div class="content-wrapper flex-center">
      <div class="text-center animate-fade">
        <div class="logo" @click="rotateBox">
          <LucideBox
            class="logo-icon"
            :class="{ 'rotate': isRotating }"
            :size="40"
          />
          <h1 class="page-title ml-2">BOX</h1>
        </div>
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

