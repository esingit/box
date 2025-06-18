<!-- src/components/base/PaginationBar.vue -->
<template>
  <div class="flex items-center space-x-3 text-gray-700 select-none justify-end">
<!-- 上一页 -->
    <button
        class="btn-outline"
        :disabled="current <= 1"
        @click="changePage(current - 1)"
    >
      上一页
    </button>

    <!-- 页码按钮 -->
    <button
        v-for="page in pagesToShow"
        :key="page"
        class="px-4 py-4 rounded-full border cursor-pointer transition"
        :class="page === current
        ? 'btn-primary'
        : 'btn-outline'"
        @click="changePage(page)"
    >
      {{ page }}
    </button>

    <!-- 下一页 -->
    <button
        class="btn-outline"
        :disabled="current >= totalPages || totalPages === 0"
        @click="changePage(current + 1)"
    >
      下一页
    </button>

    <!-- 每页条数选择 -->
    <select
        class="btn-outline select-none"
        :value="pageSize"
        @change="$emit('page-size-change', Number($event.target.value))"
    >
      <option v-for="size in pageSizeOptions" :key="size" :value="size">
        每页{{ size }}条
      </option>
    </select>

    <!-- 总条数 -->
    <span class="text-gray-500 text-sm whitespace-nowrap">
      共 {{ total }} 条
    </span>
  </div>
</template>

<script setup>
import {computed} from 'vue'

const props = defineProps({
  current: {type: Number, default: 1},
  total: {type: Number, default: 0},
  pageSize: {type: Number, default: 10}
})

const emit = defineEmits(['page-change', 'page-size-change'])

const pageSizeOptions = [7, 10, 20, 50]

const totalPages = computed(() => (props.total ? Math.ceil(props.total / props.pageSize) : 0))

const pagesToShow = computed(() => {
  const maxPages = 5
  const pages = []
  if (totalPages.value === 0) return pages

  let start = Math.max(1, props.current - Math.floor(maxPages / 2))
  let end = Math.min(totalPages.value, start + maxPages - 1)
  if (end - start + 1 < maxPages) {
    start = Math.max(1, end - maxPages + 1)
  }

  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function changePage(page) {
  if (page < 1 || page > totalPages.value || page === props.current) return
  emit('page-change', page)
}
</script>

<style scoped>
/* 禁止选中文字 */
button {
  user-select: none;
}
</style>
