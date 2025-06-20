<script setup lang="ts">
import {useAssetNameStore} from '@/store/assetNameStore'
import AssetTable from "@/components/asset/AssetTable.vue";
import BasePagination from "@/components/base/BasePagination.vue";

const store = useAssetNameStore()
const { list, pagination, loadingList } = storeToRefs(store)

const emit = defineEmits(['edit', 'delete', 'changePage', 'pageSizeChange'])
</script>

<template>
  <div class="flex flex-col space-y-4">
    <!-- 表格部分 -->
    <div class="overflow-x-auto rounded-xl bg-white border border-gray-200 dark:border-gray-700">
      <AssetNameTable
          :list="list"
          :loadingList="loadingList"
          @edit="$emit('edit', $event)"
          @delete="$emit('delete', $event)"
      />
    </div>
    <!-- 分页器 -->
    <div class="flex" v-if="pagination.total > 0">
      <BasePagination
          :current="pagination.pageNo"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          @page-change="$emit('changePage', $event)"
          @page-size-change="$emit('pageSizeChange', $event)"
      />
    </div>
  </div>
</template>
