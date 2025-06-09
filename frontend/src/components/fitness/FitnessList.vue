<template>
  <div class="list-component">
    <div class="table-container">
      <FitnessTable 
        :records="records"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>
    
    <div class="pagination-container">
      <PaginationBar
        v-if="total > 0"
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
import FitnessTable from './FitnessTable.vue';
import PaginationBar from 'components/common/PaginationBar.vue';

defineProps({
  records: {
    type: Array,
    required: true
  },
  current: {
    type: Number,
    default: 1
  },
  total: {
    type: Number,
    default: 0
  },
  pageSize: {
    type: Number,
    default: 10
  }
});

const emit = defineEmits(['page-change', 'page-size-change', 'edit', 'delete']);

function handleEdit(idx) {
  emit('edit', idx);
}

function handleDelete(idx) {
  emit('delete', idx);
}
</script>