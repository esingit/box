<template>
  <div class="fitness-list-component">
    <FitnessTable 
      :records="records"
      @edit="handleEdit"
      @delete="handleDelete"
    />
    
    <div class="pagination-container">
      <PaginationBar
        :current="current"
        :total="total"
        :pageSize="pageSize"
        @page-change="$emit('page-change', $event)"
        @page-size-change="$emit('page-size-change', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import FitnessTable from './FitnessTable.vue';
import PaginationBar from '@/components/PaginationBar.vue';

const props = defineProps({
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

defineEmits(['page-change', 'page-size-change', 'edit', 'delete']);

const totalPages = computed(() => Math.ceil(props.total / props.pageSize));
const showPagination = computed(() => props.total > props.pageSize);

function handleEdit(idx) {
  emit('edit', idx);
}

function handleDelete(idx) {
  emit('delete', idx);
}
</script>


