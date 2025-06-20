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
    type: 'text'
  },
  {
    key: 'count',
    label: '数量',
    resizable: true,
    defaultWidth: 100,
    type: 'count'
  },
  {
    key: 'finishTime',
    label: '日期',
    resizable: true,
    defaultWidth: 200,
    type: 'date'
  },
  {
    key: 'remark',
    label: '备注',
    resizable: true,
    defaultWidth: 200,
    type: 'text'
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
