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
          class="btn btn-text" 
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

const emit = defineEmits({
  cancel: null,
  submit: null
});

// 表单验证状态
const isFormValid = computed(() => {
  if (!formRef.value) return false;
  return formRef.value.isValid;
});

// 提交处理
function handleSubmit() {
  if (!isFormValid.value || props.loading) return;
  emit('submit');
}
</script>
