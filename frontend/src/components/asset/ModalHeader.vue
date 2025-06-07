<template>
  <div class="modal-header">
    <h3 class="modal-title">{{ title }}</h3>
    <button class="close-button" @click="$emit('close')">
      <LucideX class="close-icon" />
    </button>
  </div>
</template>

<script setup>
import { LucideX } from 'lucide-vue-next'

defineProps({
  title: {
    type: String,
    required: true
  }
})

defineEmits(['close'])
</script>
