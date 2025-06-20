<!-- /components/fitness/BaseActions.vue -->
<template>
  <div class="flex items-center gap-2">
    <button
        @click="$emit('edit')"
        title="编辑"
        class="btn-outline h-8"
    >
      <LucidePencil class="w-4 h-4" />
    </button>
    <button
        @click="$emit('delete')"
        title="删除"
        class="btn-danger h-8"
    >
      <LucideTrash2 class="w-4 h-4" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next'

defineEmits(['edit', 'delete'])
</script>