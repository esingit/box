<template>
  <div class="flex flex-col space-y-4">
    <!-- 表格部分 -->
    <div class="overflow-x-auto rounded-xl bg-white border border-gray-200 dark:border-gray-700">
      <AssetNameTable
          :records="list"
          :loading="loadingList"
          @edit="handleEdit"
          @delete="handleDelete"
      />
    </div>

    <!-- 分页器 -->
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
import { onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAssetNameStore } from '@/store/assetNameStore'
import AssetNameTable from './AssetNameTable.vue'
import BasePagination from '@/components/base/BasePagination.vue'

const emit = defineEmits<{
  (e: 'edit', recordId: number): void
  (e: 'delete', record: any): void
}>()

const store = useAssetNameStore()
const { list, pagination, loadingList } = storeToRefs(store)
const { loadList, setPageNo, setPageSize } = store

// 初次加载
onMounted(async () => {
  await loadList()
})

// 防止外部修改 pagination 后未刷新
watch(
    () => [pagination.value.pageNo, pagination.value.pageSize],
    async () => {
      await loadList()
    }
)

// 翻页
async function onPageChange(page: number) {
  if (page !== pagination.value.pageNo) {
    setPageNo(page)
    await loadList()
  }
}

// 每页条数变更
async function onPageSizeChange(size: number) {
  if (size !== pagination.value.pageSize) {
    setPageSize(size)
    await loadList()
  }
}

// 编辑事件，传递 recordId
function handleEdit(id: number) {
  emit('edit', id)
}

// 删除事件，传递完整 record
function handleDelete(record: any) {
  emit('delete', record)
}
</script>
