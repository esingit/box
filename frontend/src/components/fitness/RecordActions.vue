<template>
  <div class="operations">
    <button @click="$emit('edit')" class="btn btn-outline" title="编辑">
      <LucideEdit size="16" />
    </button>
    <button @click="$emit('delete')" class="btn btn-danger" title="删除">
      <LucideTrash2 size="16" />
    </button>
  </div>
</template>

<script setup>
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next';

defineEmits(['edit', 'delete']);
</script>
