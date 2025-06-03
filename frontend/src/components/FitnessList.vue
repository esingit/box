<template>
  <ul class="common-list">
    <li class="common-list-header">
      <span class="list-col-type">类型</span>
      <span class="list-col-count">数量</span>
      <span class="list-col-unit">单位</span>
      <span class="list-col-time">日期</span>
      <span class="list-col-remark">备注</span>
      <span class="list-col-action center">操作</span>
    </li>
    <li v-if="records.length === 0" class="common-list-item empty">暂无记录</li>
    <li v-for="(record, idx) in records" :key="record.id || idx" class="common-list-item">
      <span class="list-col-type">{{ record.type }}</span>
      <span class="list-col-count">{{ record.count }}</span>
      <span class="list-col-unit">{{ record.unit }}</span>
      <span class="list-col-time">{{ record.finishTime ? record.finishTime.slice(0, 10) : '-' }}</span>
      <span class="list-col-remark">{{ record.remark }}</span>
      <span class="list-col-action">
        <button @click="$emit('edit', idx)" class="btn btn-white" title="编辑">
          <LucideEdit size="18" style="vertical-align: middle;" />
        </button>
        <button @click="$emit('delete', idx)" class="btn btn-red" title="删除">
          <LucideTrash2 size="18" style="vertical-align: middle;" />
        </button>
      </span>
    </li>
  </ul>
  <div v-if="showPagination && totalPages > 1" class="pagination-bar">
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
  records: Array,
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
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next'
import { computed } from 'vue'
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))
const showPagination = computed(() => props.total > props.pageSize)
</script>
