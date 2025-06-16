<template>
  <div class="pagination">
    <button 
      class="page-btn" 
      :disabled="currentPage <= 1" 
      @click="handlePageChange(currentPage - 1)"
    >
      上一页
    </button>
    
    <button 
      v-for="page in displayPages" 
      :key="page" 
      class="page-btn" 
      :class="{ active: page === currentPage }"
      @click="handlePageChange(page)"
    >
      {{ page }}
    </button>
    
    <button 
      class="page-btn" 
      :disabled="currentPage >= totalPages || totalPages === 0" 
      @click="handlePageChange(currentPage + 1)"
    >
      下一页
    </button>
    
    <select 
      class="form-select" 
      :value="pageSize" 
      @change="$emit('page-size-change', Number($event.target.value))"
    >
      <option v-for="size in pageSizeOptions" :key="size" :value="size">
        每页{{ size }}条
      </option>
    </select>
    
    <span class="pagination-info">共 {{ total }} 条</span>
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

// 计算要显示的页码
const displayPages = computed(() => {
  const pages = []
  const maxDisplayPages = 5 // 最多显示5个页码
  const half = Math.floor(maxDisplayPages / 2)
  
  let start = Math.max(1, currentPage.value - half)
  let end = Math.min(totalPages.value, start + maxDisplayPages - 1)
  
  if (end - start + 1 < maxDisplayPages) {
    start = Math.max(1, end - maxDisplayPages + 1)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  
  return pages
})

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



