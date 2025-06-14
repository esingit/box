<template>
  <div class="page-container">
    <div class="content-wrapper u-flex u-items-center">
      <div class="animate-fade">
        <div class="logo" @click="rotateBox">
          <LucideBox
            class="logo-icon"
            :class="{ 'rotate': isRotating }"
            :size="40"
          />
          <h1 class="page-title ml-2">BOX</h1>
        </div>
      </div>
        <Dashboard v-if="userStore.isLoggedIn"/>
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import {LucideBox} from 'lucide-vue-next';
import Dashboard from '@/components/dashboard/Dashboard.vue';
import {useUserStore} from '@/store/userStore';

const userStore = useUserStore();
const isRotating = ref(false);

function rotateBox() {
  isRotating.value = true;
  setTimeout(() => {
    isRotating.value = false;
  }, 1000);
}
</script>

