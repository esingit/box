<template>
  <div class="flex flex-col space-y-4">
    <!-- 表格部分 -->
    <div class="overflow-x-auto rounded-xl bg-white border border-gray-200 dark:border-gray-700">
      <AssetTable
          :records="records"
          @edit="$emit('edit', $event)"
          @delete="$emit('delete', $event)"
      />
    </div>

    <!-- 分页器 -->
    <div class="flex justify-end" v-if="total > 0">
      <BasePagination
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
import BasePagination from '@/components/base/BasePagination.vue'

defineProps({
  records: {
    type: Array,
    default: () => []
  },
  current: {
    type: Number,
    default: 1
  },
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  }
})

defineEmits(['edit', 'delete', 'page-change', 'page-size-change'])
</script>
