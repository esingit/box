<template>
  <div class="list-component">
    <div class="table-container">
      <AssetTable
        :records="records"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>

    <div class="pagination-wrapper">
      <PaginationBar
        :current="current"
        :total="total"
        :page-size="pageSize"
        @page-change="$emit('page-change', $event)"
        @page-size-change="$emit('page-size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import AssetTable from './AssetTable.vue'
import PaginationBar from '@/components/PaginationBar.vue'

const props = defineProps({
  records: {
    type: Array,
    required: true
  },
  current: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    required: true
  }
})

defineEmits(['edit', 'delete', 'page-change', 'page-size-change'])
</script>
