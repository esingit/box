<template>
  <div class="container flex-center">
    <div class="text-center">
      <h1 class="page-title">欢迎使用 Box</h1>
      <div class="flex-center">
        <LucideBox 
          class="title-icon cursor-pointer" 
          :class="{ 'animate-spin': isRotating }" 
          @click="rotateBox" 
          :size="48" 
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { LucideBox } from 'lucide-vue-next';

const isRotating = ref(false);

function rotateBox() {
  isRotating.value = true;
  setTimeout(() => {
    isRotating.value = false;
  }, 1000);
}
</script>

