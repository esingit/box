<template>
  <div class="flex flex-col space-y-4">
    <div class="overflow-x-auto rounded border border-gray-200 bg-white shadow-sm">
      <FitnessTable
          :records="list"
          :loading="loadingList"
          @edit="handleEdit"
          @delete="handleDelete"
      />
    </div>

    <div class="flex justify-end">
      <PaginationBar
          v-if="pagination.total > 0"
          :current="pagination.current"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          @page-change="onPageChange"
          @page-size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useFitnessStore } from '@/store/fitnessStore'
import FitnessTable from './FitnessTable.vue'
import PaginationBar from '@/components/base/PaginationBar.vue'

const emit = defineEmits<{
  (e: 'edit', recordId: number): void
  (e: 'delete', recordId: number): void
}>()

const store = useFitnessStore()
const { list, pagination, loadingList } = storeToRefs(store)
const { loadList, changePage, changePageSize } = store

// 初次加载列表
onMounted(async () => {
  await loadList()
})

// 翻页
async function onPageChange(page: number) {
  await changePage(page)
}

// 每页条数改变
async function onPageSizeChange(size: number) {
  await changePageSize(size)
}

// 编辑和删除事件透传
function handleEdit(id: number) {
  emit('edit', id)
}
function handleDelete(id: number) {
  emit('delete', id)
}
</script>
