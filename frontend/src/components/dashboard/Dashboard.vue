<template>
  <div class="dashboard-wrapper">
    <n-space vertical size="large" class="grid-layout">
      <FitnessStats />
      <AssetStats />
    </n-space>
  </div>
</template>

<script setup>
import FitnessStats from './components/FitnessStats.vue';
import AssetStats from './components/AssetStats.vue';
</script>
