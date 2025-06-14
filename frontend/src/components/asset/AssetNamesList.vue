<template>
  <div class="list-component">
    <div class="table-container">
      <AssetNamesTable
        :names="names"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
    
    <div class="pagination-container">
      <PaginationBar
        v-if="total > 0"
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
import AssetNamesTable from './AssetNamesTable.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

defineProps({
  names: {
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
