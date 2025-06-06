<template>
  <div class="pagination-bar">
    <button 
      class="btn btn-white" 
      :disabled="currentPage <= 1" 
      @click="handlePageChange(currentPage - 1)"
    >
      上一页
    </button>
    <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
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
      @change="$emit('page-size-change', Number($event.target.value))"
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

const pageSizeOptions = [7, 10, 20, 50]

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
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.btn {
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:hover:not(:disabled) {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.page-size-select {
  padding: 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
}

.page-info,
.total-count {
  color: #606266;
}
</style>
