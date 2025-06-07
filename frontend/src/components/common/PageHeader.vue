<template>
  <div class="page-title">
    <component :is="icon" class="title-icon" color="#222" size="24" />
    <span>{{ title }}</span>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  icon: {
    type: Object,
    required: true
  }
});
</script>
