<template>
  <div class="operations">
    <button class="action-btn" @click="$emit('edit')" title="编辑">
      <LucidePencil :size="16" />
    </button>
    <button class="action-btn" @click="$emit('delete')" title="删除">
      <LucideTrash2 :size="16" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next';

defineEmits(['edit', 'delete']);
</script>