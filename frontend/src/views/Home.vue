<template>
  <div class="home-container">
    <div class="home-content">
      <h1>欢迎使用 Box</h1>
      <LucideBox class="home-box-icon" :class="{ 'rotate': isRotating }" @click="rotateBox" :size="48" />
      <p>您的个人管理助手</p>
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

