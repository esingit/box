<template>
  <div>
    <table class="table">
      <thead>
        <tr>
          <th v-for="header in tableHeaders" :key="header.key">
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
        <tr v-for="(record, idx) in records" :key="record.id || idx">
          <td>{{ record.assetName }}</td>
          <td>{{ record.assetTypeValue }}</td>
          <td :class="['amount-cell', record.amount > 0 ? 'positive' : 'negative']">
            {{ formatAmount(record.amount) }}
          </td>
          <td>{{ record.unitValue }}</td>
          <td>{{ record.locationValue }}</td>
          <td>{{ formatDate(record.acquireTime) }}</td>
          <td class="remark-cell">
            <span :title="record.remark">{{ record.remark || '-' }}</span>
          </td>
          <td class="operations">
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

const props = defineProps({
  records: {
    type: Array,
    required: true
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'assetName', label: '资产名称' },
  { key: 'assetType', label: '类型' },
  { key: 'amount', label: '金额' },
  { key: 'unit', label: '货币单位' },
  { key: 'location', label: '位置' },
  { key: 'time', label: '时间' },
  { key: 'remark', label: '备注' },
  { key: 'operations', label: '操作' }
]

function formatAmount(amount) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>
