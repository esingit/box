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
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
            />
          </td>
        </tr>
        <tr v-for="(record, idx) in records" :key="record.id || idx" class="table-row">
          <td class="cell-text">{{ record.typeValue }}</td>
          <td class="cell-number">{{ record.count }}</td>
          <td class="cell-text">{{ record.unitValue }}</td>
          <td class="cell-date">{{ formatDate(record.finishTime) }}</td>
          <td class="cell-remark">
            <span v-if="record.remark" class="remark-text" :title="record.remark">
              {{ record.remark }}
            </span>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="cell-actions">
            <RecordActions 
              :record="record"
              type="fitness"
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
import EmptyState from '@/components/common/EmptyState.vue';
import RecordActions from '@/components/common/RecordActions.vue';

defineProps({
  records: {
    type: Array,
    required: true
  }
});

defineEmits(['edit', 'delete']);

const tableHeaders = [
  { key: 'type', label: '类型', class: 'cell-text' },
  { key: 'count', label: '数量', class: 'cell-number' },
  { key: 'unit', label: '单位', class: 'cell-text' },
  { key: 'date', label: '日期', class: 'cell-date' },
  { key: 'remark', label: '备注', class: 'cell-remark' },
  { key: 'actions', label: '操作', class: 'cell-actions' }
];

function formatDate(dateString) {
  return dateString ? dateString.slice(0, 10) : '-';
}
</script>
