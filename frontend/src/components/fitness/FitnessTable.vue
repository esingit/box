<template>
  <BaseTable
      :columns="columns"
      :data="records"
      :loading="loading"
      storage-key="fitnessTableColumnWidths"
      @edit="(row: any) => emit('edit', row.id)"
      @delete="(row: any) => emit('delete', row)"
  >
  </BaseTable>
</template>

<script setup lang="ts">
import BaseTable from '@/components/base/BaseTable.vue'

const props = defineProps<{
  records: any[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'edit', id: number): void
  (e: 'delete', record: any): void
}>()

const columns = [
  {
    key: 'typeValue',
    label: '类型',
    resizable: true,
    defaultWidth: 200,
    type: 'text',
    sortable: true
  },
  {
    key: 'count',
    label: '数值',
    resizable: true,
    defaultWidth: 150,
    type: 'count',
    sortable: true
  },
  {
    key: 'finishTime',
    label: '日期',
    resizable: true,
    defaultWidth: 200,
    type: 'date',
    sortable: true
  },
  {
    key: 'remark',
    label: '备注',
    resizable: true,
    defaultWidth: 250,
    type: 'text',
    sortable: true
  },
  {
    key: 'actions',
    label: '操作',
    resizable: false,
    defaultWidth: 100,
    actions: true
  }
]
</script>
