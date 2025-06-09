<template>
  <div class="data-table">
    <table class="table">
      <thead>
        <tr>
          <th v-for="header in tableHeaders" :key="header.key" :class="header.class">
            {{ header.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="records.length === 0">
          <td :colspan="tableHeaders.length">
            <EmptyState
              icon="Wallet"
              message="暂无资产记录"
              description="点击上方的添加记录按钮开始记录"
            />
          </td>
        </tr>
        <tr v-for="(record, idx) in records" :key="record.id || idx" class="table-row">
          <td class="cell-text">{{ record.assetName }}</td>
          <td class="cell-text">{{ record.assetTypeValue }}</td>
          <td class="cell-number">
            <span :class="{ 'negative': record.amount < 0 }">
              {{ formatAmount(record.amount) }}
            </span>
            {{ record.unitValue }}
          </td>
          <td class="cell-text">{{ record.locationValue }}</td>
          <td class="cell-date">{{ formatDate(record.acquireTime) }}</td>
          <td class="cell-remark">
            <span v-if="record.remark" class="remark-text" :title="record.remark">
              {{ record.remark }}
            </span>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="cell-actions">
            <RecordActions 
              :record="record"
              type="asset"
              @edit="$emit('edit', idx)"
              @delete="$emit('delete', idx)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
import RecordActions from '@/components/common/RecordActions.vue'

defineProps({
  records: {
    type: Array,
    required: true,
    default: () => []
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'assetName', label: '资产名称', class: 'cell-text' },
  { key: 'assetType', label: '类型', class: 'cell-text' },
  { key: 'amount', label: '金额', class: 'cell-number' },
  { key: 'location', label: '位置', class: 'cell-text' },
  { key: 'time', label: '时间', class: 'cell-date' },
  { key: 'remark', label: '备注', class: 'cell-remark' },
  { key: 'actions', label: '操作', class: 'cell-actions' }
]

function formatAmount(amount) {
  if (!amount) return '0.00'
  const formatted = new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Math.abs(amount))
  return (amount < 0 ? '-' : '') + formatted
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}
</script>
