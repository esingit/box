<template>
  <div class="flex flex-col gap-2 w-64">
    <div
        v-for="(item, index) in skeletonItems"
        :key="index"
        :class="['rounded-md bg-gray-300 dark:bg-gray-700 animate-pulse', item.width]"
        class="h-4"
    ></div>
  </div>
</template>

<script setup>
const skeletonItems = [
  { width: 'w-2/5' },  // 40%
  { width: 'w-3/4' },  // 75%
  { width: 'w-1/3' }   // 33%
];
</script>
