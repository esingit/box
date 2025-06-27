<!--src/components/asset/AssetTable.vue-->
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
  {key: 'assetName', label: '资产名称', resizable: true, defaultWidth: 350, type: 'text', sortable: true},
  {key: 'assetTypeValue', label: '类型', resizable: true, defaultWidth: 80, type: 'text', sortable: true},
  {key: 'amount', label: '金额', resizable: true, defaultWidth: 120, type: 'count', sortable: true},
  {key: 'assetLocationValue', label: '位置', resizable: true, defaultWidth: 80, type: 'text', sortable: true},
  {key: 'acquireTime', label: '时间', resizable: true, defaultWidth: 100, type: 'date', sortable: true},
  {key: 'remark', label: '备注', resizable: true, defaultWidth: 180, type: 'text', sortable: true},
  {key: 'actions', label: '操作', resizable: false, defaultWidth: 100, actions: true}
]

function handleEdit(record: any) {
  emit('edit', record.id)
}

function handleDelete(record: any) {
  emit('delete', record)
}
</script>
