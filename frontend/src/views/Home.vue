<template>
  <div class="container flex-center">
    <div class="text-center">
      <div class="flex items-center justify-center">
        <LucideBox 
          class="title-icon cursor-pointer mr-2" 
          :class="{ 'animate-spin': isRotating }" 
          @click="rotateBox" 
          :size="40" 
        />
        <h1 class="page-title">BOX</h1>
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

