<template>
  <div class="flex-1 overflow-auto">
    <AssetNameTable :list="list" :loadingList="loadingList" @edit="$emit('edit', $event)" @delete="$emit('delete', $event)" />
    <div class="flex" v-if="pagination.total > 0">
      <BasePagination
          :current="pagination.pageNo"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          @page-change="onPageChange"
          @page-size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import AssetNameTable from './AssetNameTable.vue'
import BasePagination from '@/components/base/BasePagination.vue'

const props = defineProps({
  list: Array,
  loadingList: Boolean,
  pagination: Object,
  totalPages: Number
})

const emit = defineEmits(['edit', 'delete', 'changePage'])
</script>
