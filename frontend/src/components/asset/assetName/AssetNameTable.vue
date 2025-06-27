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
  {key: 'name', label: '名称', resizable: true, defaultWidth: 400, type: 'text', sortable: true},
  {key: 'description', label: '描述', resizable: true, defaultWidth: 200, type: 'text', sortable: true},
  {key: 'actions', label: '操作', resizable: false, defaultWidth: 100, actions: true}
]

// 事件处理
function handleEdit(item: any) {
  emit('edit', item.id)
}

function handleDelete(item: any) {
  emit('delete', item)
}
</script>
