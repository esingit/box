<template>
  <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3 bg-white">
    <h3 class="text-lg font-medium text-gray-900">{{ title }}</h3>
    <button
        @click="$emit('close')"
        aria-label="关闭"
        class="text-gray-500 hover:text-gray-700 rounded-md p-1 transition"
    >
      <LucideX class="w-5 h-5" />
    </button>
  </div>
</template>

<script setup>
import { LucideX } from 'lucide-vue-next';

defineProps({
  title: { type: String, required: true }
});

defineEmits(['close']);
</script>
