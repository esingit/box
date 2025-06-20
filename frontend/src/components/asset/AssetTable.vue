<template>
  <BaseTable
      :columns="columns"
      :data="records"
      :loading="loading"
      @edit="handleEdit"
      @delete="handleDelete"
  >
  </BaseTable>
</template>

<script setup lang="ts">
import BaseTable from '@/components/base/BaseTable.vue'

const props = defineProps({
  records: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit', 'delete'])

const columns = [
  { key: 'assetName', label: '资产名称', resizable: true, defaultWidth: 250 },
  { key: 'assetTypeValue', label: '类型', resizable: true, defaultWidth: 120 },
  { key: 'amount', label: '金额', resizable: true, defaultWidth: 130 },
  { key: 'aseetLocationValue', label: '位置', resizable: true, defaultWidth: 120 },
  { key: 'time', label: '时间', resizable: true, defaultWidth: 120 },
  { key: 'remark', label: '备注', resizable: true, defaultWidth: 150 },
  { key: 'actions', label: '操作', resizable: false, defaultWidth: 100, actions: true }
]

function handleEdit(record) {
  emit('edit', record.id)
}

function handleDelete(record) {
  emit('delete', record)
}
</script>
