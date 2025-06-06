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
      <span class="list-col-type">{{ record.typeValue }}</span>
      <span class="list-col-count">{{ record.count }}</span>
      <span class="list-col-unit">{{ record.unitValue }}</span>
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
  <PaginationBar
    :current="current"
    :total="total"
    :pageSize="pageSize"
    @page-change="$emit('page-change', $event)"
    @page-size-change="$emit('page-size-change', $event)"
  />
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
import PaginationBar from '@/components/PaginationBar.vue'
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))
const showPagination = computed(() => props.total > props.pageSize)
</script>
