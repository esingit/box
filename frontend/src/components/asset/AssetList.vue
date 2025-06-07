<template>
  <div class="asset-list">
    <table class="data-table">
      <thead>
        <tr>
          <th>资产名称</th>
          <th>类型</th>
          <th>金额</th>
          <th>货币单位</th>
          <th>位置</th>
          <th>时间</th>
          <th>备注</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(record, idx) in records" :key="record.id">
          <td>{{ record.assetName }}</td>
          <td>{{ record.assetTypeValue }}</td>
          <td>{{ record.amount }}</td>
          <td>{{ record.unitValue }}</td>
          <td>{{ record.locationValue }}</td>
          <td>{{ formatDate(record.acquireTime) }}</td>
          <td>{{ record.remark }}</td>
          <td class="operations">
            <button @click="$emit('edit', idx)" class="btn btn-primary" title="编辑">
              <LucideEdit size="18" style="vertical-align: middle;" />
            </button>
            <button @click="$emit('delete', idx)" class="btn btn-danger" title="删除">
              <LucideTrash2 size="18" style="vertical-align: middle;" />
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pagination-container">
      <PaginationBar 
        :current="current"
        :total="total"
        :page-size="pageSize"
        @page-change="$emit('page-change', $event)"
        @page-size-change="$emit('page-size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next'
import PaginationBar from '@/components/PaginationBar.vue'

const props = defineProps({
  records: {
    type: Array,
    required: true
  },
  current: {
    type: Number,
    required: true
  },
  total: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    required: true
  }
})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}
</script>
