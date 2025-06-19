<template>
  <div class="flex flex-col bg-white rounded-md shadow border border-gray-200 overflow-hidden">
    <div class="overflow-x-auto">
      <AssetNamesTable
          :names="names"
          @edit="$emit('edit', $event)"
          @delete="$emit('delete', $event)"
          class="min-w-[600px]"
      />
    </div>

    <div
        v-if="total > 0"
        class="border-t border-gray-200 px-4 py-3 flex items-center justify-between bg-gray-50"
    >
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
import AssetNamesTable from './AssetNamesTable.vue'
import BasePagination from '@/components/base/BasePagination.vue'

defineProps({
  names: {
    type: Array,
    required: true,
  },
  current: {
    type: Number,
    required: true,
  },
  total: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    required: true,
  },
})

defineEmits(['edit', 'delete', 'page-change', 'page-size-change'])
</script>
