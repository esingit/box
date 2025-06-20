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
  {key: 'assetName', label: '资产名称', resizable: true, defaultWidth: 250, type: 'text'},
  {key: 'assetTypeValue', label: '类型', resizable: true, defaultWidth: 120, type: 'text'},
  {key: 'amount', label: '金额', resizable: true, defaultWidth: 130, type: 'count'},
  {key: 'assetLocationValue', label: '位置', resizable: true, defaultWidth: 120, type: 'text'},
  {key: 'acquireTime', label: '时间', resizable: true, defaultWidth: 120, type: 'date'},
  {key: 'remark', label: '备注', resizable: true, defaultWidth: 150, type: 'text'},
  {key: 'actions', label: '操作', resizable: false, defaultWidth: 30, actions: true}
]

function handleEdit(record: any) {
  emit('edit', record.id)
}

function handleDelete(record: any) {
  emit('delete', record)
}
</script>
