<template>
  <div>
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
            <EmptyState />
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
          <td>
            <RecordActions 
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
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next';
import EmptyState from './EmptyState.vue';
import RecordActions from './RecordActions.vue';

defineProps({
  records: {
    type: Array,
    required: true
  }
});

defineEmits(['edit', 'delete']);

const tableHeaders = [
  { key: 'type', label: '类型' },
  { key: 'count', label: '数量' },
  { key: 'unit', label: '单位' },
  { key: 'date', label: '日期' },
  { key: 'remark', label: '备注' },
  { key: 'actions', label: '操作', class: 'center' }
];

function formatDate(dateString) {
  return dateString ? dateString.slice(0, 10) : '-';
}
</script>
