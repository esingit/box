<template>
  <div class="dashboard-wrapper px-4 py-6">
    <div class="grid gap-12">
      <FitnessStats />
      <AssetStats />
    </div>
  </div>
</template>

<script setup>
import FitnessStats from './components/FitnessStats.vue'
import AssetStats from './components/AssetStats.vue'
</script>
