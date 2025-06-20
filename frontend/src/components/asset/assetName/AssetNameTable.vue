<template>
  <BaseTable
      :columns="columns"
      :data="list"
      :loading="loadingList"
      storage-key="assetNameTableColumnWidths"
      @edit="handleEdit"
      @delete="handleDelete"
  >
  </BaseTable>
</template>

<script setup lang="ts">
import BaseTable from '@/components/base/BaseTable.vue'

const props = defineProps({
  list: {
    type: Array,
    default: () => []
  },
  loadingList: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit', 'delete'])

// 列定义
const columns = [
  { key: 'name', label: '名称', resizable: true, defaultWidth: 200 },
  { key: 'description', label: '描述', resizable: true, defaultWidth: 250 },
  { key: 'actions', label: '操作', resizable: false, actions: true }
]

// 事件处理
function handleEdit(item) {
  emit('edit', item)
}

function handleDelete(item) {
  emit('delete', item)
}
</script>
