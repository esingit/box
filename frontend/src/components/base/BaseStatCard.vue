<template>
  <div class="p-4 rounded-lg border border-gray-200 hover:shadow-md transition">
    <p class="text-sm text-gray-500">{{ title }}</p>
    <p :class="['mt-2 text-3xl font-bold ', highlightClass]">
      {{ amount }}
    </p>
    <p :class="[changeClass, 'mt-1 text-sm']">
      {{ prefix }}{{ change }}
    </p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  amount: string | number
  change: string | number
  prefix?: string
  changeClass?: string
  highlightClass?: string
}>()
</script>
