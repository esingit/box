<template>
  <div class="pagination-bar">
    <button class="btn btn-white" :disabled="current === 1" @click="$emit('page-change', current - 1)">上一页</button>
    <span>第 {{ current }} / {{ totalPages }} 页</span>
    <button class="btn btn-white" :disabled="current === totalPages || totalPages === 0" @click="$emit('page-change', current + 1)">下一页</button>
    <select class="page-size-select" :value="pageSize" @change="$emit('page-size-change', Number($event.target.value))">
      <option v-for="size in [7, 10, 20, 50]" :key="size" :value="size">每页{{ size }}条</option>
    </select>
    <span class="total-count">共 {{ total }} 条</span>
  </div>
</template>

<script setup>
const props = defineProps({
  current: {
    type: Number,
    default: 1
  },
  total: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  }
})
import { computed } from 'vue'
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))
</script>
