<template>
  <div class="pagination-bar">
    <button 
      class="btn btn-white" 
      :disabled="currentPage <= 1" 
      @click="handlePageChange(currentPage - 1)"
    >
      上一页
    </button>
    <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
    <button 
      class="btn btn-white" 
      :disabled="currentPage >= totalPages || totalPages === 0" 
      @click="handlePageChange(currentPage + 1)"
    >
      下一页
    </button>
    <select 
      class="page-size-select" 
      :value="pageSize" 
      @change="handlePageSizeChange($event)"
    >
      <option v-for="size in pageSizeOptions" :key="size" :value="size">
        每页{{ size }}条
      </option>
    </select>
    <span class="total-count">共 {{ total }} 条</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
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

const emit = defineEmits(['page-change', 'page-size-change'])

// 页面大小选项
const pageSizeOptions = [10, 20, 50, 100]

// 计算总页数
const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

// 确保当前页在有效范围内
const currentPage = computed(() => {
  return Math.max(1, Math.min(props.current, totalPages.value))
})

// 处理页码变化
function handlePageChange(page) {
  const validPage = Math.max(1, Math.min(page, totalPages.value))
  if (validPage !== props.current) {
    emit('page-change', validPage)
  }
}

// 处理每页显示数量变化
function handlePageSizeChange(event) {
  const newSize = Number(event.target.value)
  if (newSize !== props.pageSize) {
    emit('page-size-change', newSize)
  }
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-white {
  padding: 6px 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.btn-white:disabled {
  color: #999;
  cursor: not-allowed;
}

.btn-white:not(:disabled):hover {
  border-color: #666;
}

.page-size-select {
  padding: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.total-count {
  color: #666;
}
</style>
