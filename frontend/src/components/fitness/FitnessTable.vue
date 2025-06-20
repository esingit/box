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
    tooltip: (row: any) => row.typeValue
  },
  {
    key: 'count',
    label: '数量',
    resizable: true,
    defaultWidth: 200,
    type: 'count',
    tooltip: (row: any) => `${row.count} ${row.unitValue || ''}`.trim()
  },
  {
    key: 'finishTime',
    label: '日期',
    resizable: true,
    defaultWidth: 300,
    type: 'date',
    value: (row: any) => row.finishTime?.slice(0, 10) || '-',
    tooltip: (row: any) => row.finishTime?.slice(0, 10)
  },
  {
    key: 'remark',
    label: '备注',
    resizable: true,
    defaultWidth: 500,
    type: 'text',
    tooltip: (row: any) => row.remark
  },
  {
    key: 'actions',
    label: '操作',
    resizable: false,
    defaultWidth: 30,
    actions: true
  }
]
</script>
