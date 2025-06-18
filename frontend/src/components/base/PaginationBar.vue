<!--src/components/base/PaginationBar.vue-->
<template>
  <div class="pagination flex items-center space-x-3 text-gray-700 dark:text-gray-300 select-none">
    <button
        class="page-btn px-3 py-1 rounded border border-gray-300 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
        :disabled="props.current <= 1"
        @click="handlePageChange(props.current - 1)"
    >
      上一页
    </button>

    <button
        v-for="page in displayPages"
        :key="page"
        class="page-btn px-3 py-1 rounded border cursor-pointer transition"
        :class="page === props.current
        ? 'bg-blue-600 text-white border-blue-600'
        : 'hover:bg-gray-100 border-gray-300'"
        @click="handlePageChange(page)"
    >
      {{ page }}
    </button>

    <button
        class="page-btn px-3 py-1 rounded border border-gray-300 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
        :disabled="props.current >= totalPages || totalPages === 0"
        @click="handlePageChange(props.current + 1)"
    >
      下一页
    </button>

    <select
        class="form-select border border-gray-300 rounded px-2 py-1 ml-4 bg-white text-gray-700 focus:outline-none focus:ring-1 focus:ring-blue-500 transition"
        :value="props.pageSize"
        @change="$emit('page-size-change', Number($event.target.value))"
    >
      <option v-for="size in pageSizeOptions" :key="size" :value="size">
        每页{{ size }}条
      </option>
    </select>

    <span class="pagination-info ml-2 text-gray-500 dark:text-gray-400 text-sm">
      共 {{ props.total }} 条
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const pageSizeOptions = [7, 10, 20, 50]

const props = defineProps({
  current: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 }
})

const emit = defineEmits(['page-change', 'page-size-change'])

const totalPages = computed(() => {
  if (props.total === 0) return 0
  return Math.ceil(props.total / props.pageSize)
})

const displayPages = computed(() => {
  const pages = []
  const maxDisplayPages = 5
  const half = Math.floor(maxDisplayPages / 2)

  if (totalPages.value === 0) return pages

  let start = Math.max(1, props.current - half)
  let end = Math.min(totalPages.value, start + maxDisplayPages - 1)

  if (end - start + 1 < maxDisplayPages) {
    start = Math.max(1, end - maxDisplayPages + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

function handlePageChange(page) {
  if (page < 1 || page > totalPages.value || page === props.current) return
  emit('page-change', page)
}
</script>

<style scoped>
.page-btn {
  user-select: none;
}
</style>
