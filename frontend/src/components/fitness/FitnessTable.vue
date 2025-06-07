<template>
  <div class="table-container">
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
        <tr v-for="(record, idx) in records" :key="record.id || idx">
          <td>{{ record.typeValue }}</td>
          <td class="count-cell">{{ record.count }}</td>
          <td>{{ record.unitValue }}</td>
          <td>{{ formatDate(record.finishTime) }}</td>
          <td class="remark-cell">
            <span :title="record.remark">{{ record.remark || '-' }}</span>
          </td>
          <td class="operations">
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
  { key: 'type', label: '类型' },
  { key: 'count', label: '数量', class: 'count-cell' },
  { key: 'unit', label: '单位' },
  { key: 'date', label: '日期' },
  { key: 'remark', label: '备注', class: 'remark-cell' },
  { key: 'actions', label: '操作', class: 'operations' }
];

function formatDate(dateString) {
  return dateString ? dateString.slice(0, 10) : '-';
}
</script>
