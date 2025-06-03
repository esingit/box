<template>
  <ul class="common-list">
    <li class="common-list-header">
      <span class="list-col-type center">类型</span>
      <span class="list-col-count">数量</span>
      <span class="list-col-unit">单位</span>
      <span class="list-col-time">日期</span>
      <span class="list-col-remark">备注</span>
      <span class="list-col-action center">操作</span>
    </li>
    <li v-if="records.length === 0" class="common-list-item empty">暂无记录</li>
    <li v-for="(record, idx) in records" :key="record.id" class="common-list-item">
      <span class="list-col-type">{{ record.type }}</span>
      <span class="list-col-count">{{ record.count }}</span>
      <span class="list-col-unit">{{ record.unit }}</span>
      <span class="list-col-time">{{ record.finishTime ? record.finishTime.slice(0, 10) : '-' }}</span>
      <span class="list-col-remark">{{ record.remark }}</span>
      <span class="list-col-action">
        <button @click="$emit('edit', idx)" class="btn btn-gray" title="编辑">
          <LucideEdit size="18" style="vertical-align: middle;" />
        </button>
        <button @click="$emit('delete', idx)" class="btn btn-red" title="删除">
          <LucideTrash2 size="18" style="vertical-align: middle;" />
        </button>
      </span>
    </li>
  </ul>
</template>

<script setup>
const props = defineProps({
  records: Array
})
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next'
</script>
