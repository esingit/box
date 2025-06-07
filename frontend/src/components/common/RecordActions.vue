<template>
  <div class="operations">
    <button class="action-btn edit" @click="$emit('edit')" title="编辑">
      <LucidePencil :size="20" />
    </button>
    <button class="action-btn delete" @click="handleDelete" title="删除">
      <LucideTrash2 :size="20" />
    </button>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next';
import emitter from '@/utils/eventBus.js';

const props = defineProps({
  record: {
    type: Object,
    required: true
  },
  type: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['edit', 'delete']);

function handleDelete() {
  let confirmMessage = '';
  
  // 根据不同类型显示不同的确认消息
  if (props.type === 'fitness') {
    confirmMessage = `确定要删除这条${props.record.typeValue}记录吗？\n数量：${props.record.count}${props.record.unitValue}`;
  } else if (props.type === 'asset') {
    confirmMessage = `确定要删除这条${props.record.assetName}记录吗？\n金额：${props.record.amount}${props.record.unitValue}`;
  } else if (props.type === 'asset-name') {
    confirmMessage = `确定要删除资产名称"${props.record.name}"吗？`;
  }

  emitter.emit('confirm', {
    title: '删除确认',
    message: confirmMessage,
    type: 'danger',
    confirmText: '删除',
    onConfirm: () => emit('delete')
  });
}
</script>
