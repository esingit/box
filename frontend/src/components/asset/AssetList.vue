<template>
  <div class="flex flex-col space-y-4">
    <div class="overflow-auto rounded border border-gray-200 dark:border-gray-700">
      <AssetTable
          :records="records"
          @edit="$emit('edit', $event)"
          @delete="$emit('delete', $event)"
      />
    </div>

    <div class="flex justify-end">
      <BasePaginationBar
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
import AssetTable from './AssetTable.vue'
import BasePaginationBar from '@/components/base/BasePaginationBar.vue'

defineProps({
  records: Array,
  current: Number,
  total: Number,
  pageSize: Number
})

defineEmits(['edit', 'delete', 'page-change', 'page-size-change'])
</script>

<style scoped>
.list-component {
  /* 你可以留空，全部用 Tailwind 管理样式 */
}
</style>
