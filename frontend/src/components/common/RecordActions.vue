<template>
  <div class="operations">
    <button class="action-btn" @click="$emit('edit')" title="编辑">
      <LucidePencil :size="16" />
    </button>
    <button class="action-btn delete" @click="handleDelete" title="删除">
      <LucideTrash2 :size="16" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next';
import emitter from '@/utils/eventBus.js';

const emit = defineEmits(['edit', 'delete']);

function handleDelete() {
  emitter.emit('confirm', {
    title: '删除确认',
    message: '确定要删除这条记录吗？',
    type: 'danger',
    confirmText: '删除',
    onConfirm: () => emit('delete')
  });
}
</script>
