<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-container">
      <ModalHeader 
        :title="title" 
        @close="$emit('cancel')" 
      />
      
      <div class="modal-body">
        <FitnessForm 
          ref="formRef"
          :form="form"
          :types="types"
          :units="units"
          :remark-placeholder="remarkPlaceholder"
        />
      </div>

      <div class="modal-footer">
        <button 
          class="btn btn-secondary" 
          @click="$emit('cancel')"
        >
          取消
        </button>
        <button 
          class="btn btn-primary" 
          :disabled="loading || !isFormValid" 
          @click="handleSubmit"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import FitnessForm from './FitnessForm.vue';
import ModalHeader from './ModalHeader.vue';

const formRef = ref(null);

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  form: {
    type: Object,
    required: true
  },
  types: {
    type: Array,
    default: () => []
  },
  units: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '记录'
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  remarkPlaceholder: {
    type: String,
    default: '备注'
  }
});

defineEmits(['submit', 'cancel']);

const isFormValid = computed(() => {
  return formRef.value?.isValid || false;
});

function handleSubmit() {
  if (isFormValid.value && !props.loading) {
    props.$emit('submit');
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-container {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.modal-body {
  padding: 1rem;
}

.modal-footer {
  padding: 1rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  border-top: 1px solid #eee;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  color: #333;
}

.btn-primary {
  background-color: #0066cc;
  border: 1px solid #0066cc;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #e4e4e4;
}

.btn-primary:hover:not(:disabled) {
  background-color: #0052a3;
}
</style>


