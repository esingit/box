<template>
  <div class="flex space-x-2">
    <button
        @click="$emit('edit', props.record)"
        title="编辑"
        class="flex items-center justify-center w-8 h-8 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
        aria-label="编辑"
    >
      <LucidePencil class="text-gray-700 dark:text-gray-300" size="20" />
    </button>
    <button
        @click="handleDelete"
        title="删除"
        class="flex items-center justify-center w-8 h-8 rounded hover:bg-red-100 dark:hover:bg-red-800 transition-colors"
        aria-label="删除"
    >
      <LucideTrash2 class="text-red-600 dark:text-red-400" size="20" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next';
import emitter from '@/utils/eventBus.ts';

const props = defineProps({
  record: { type: Object, required: true },
  type: { type: String, required: true }
});
const emit = defineEmits(['edit', 'delete']);

function handleDelete() {
  let confirmMessage = '';

  if (props.type === 'fitness') {
    confirmMessage = `确定要删除这条${props.record.typeValue || ''}记录吗？\n数量：${props.record.count || ''}${props.record.unitValue || ''}`;
  } else if (props.type === 'asset') {
    confirmMessage = `确定要删除这条${props.record.assetName || ''}记录吗？\n金额：${props.record.amount || ''}${props.record.unitValue || ''}`;
  } else if (props.type === 'asset-name') {
    confirmMessage = `确定要删除资产名称"${props.record.name || ''}"吗？`;
  } else {
    confirmMessage = '确定要删除该记录吗？';
  }

  emitter.emit('confirm', {
    title: '删除确认',
    message: confirmMessage,
    type: 'danger',
    confirmText: '删除',
    onConfirm: () => emit('delete', props.record)
  });
}
</script>
