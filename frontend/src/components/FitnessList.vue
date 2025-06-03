<template>
  <ul class="fitness-list">
    <li class="fitness-header">
      <span class="item-type center">类型</span>
      <span class="item-count">数量</span>
      <span class="item-unit">单位</span>
      <span class="item-time">日期</span>
      <span class="item-remark">备注</span>
      <span class="item-action center">操作</span>
    </li>
    <li v-if="records.length === 0" class="fitness-item empty">暂无记录</li>
    <li v-for="(record, idx) in records" :key="record.id" class="fitness-item">
      <span class="item-type">{{ record.type }}</span>
      <span class="item-count">{{ record.count }}</span>
      <span class="item-unit">{{ record.unit }}</span>
      <span class="item-time">{{ record.finishTime ? record.finishTime.slice(0, 10) : '-' }}</span>
      <span class="item-remark">{{ record.remark }}</span>
      <span class="item-action">
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
