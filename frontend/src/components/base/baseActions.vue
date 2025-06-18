<!-- /components/fitness/RecordActions.vue -->
<template>
  <div class="flex items-center gap-2">
    <button
        @click="$emit('edit')"
        title="编辑"
        class="icon-button text-gray-600 hover:bg-gray-100"
    >
      <LucidePencil class="w-4 h-4" />
    </button>
    <button
        @click="$emit('delete')"
        title="删除"
        class="icon-button text-red-500 hover:bg-red-100"
    >
      <LucideTrash2 class="w-4 h-4" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next'

defineEmits(['edit', 'delete'])
</script>