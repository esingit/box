<template>
  <BaseModal :visible="show" :title="title" @update:visible="handleCancel" width="500px">
    <FitnessForm
        ref="formRef"
        :form="form"
        :types="types"
        :units="units"
        :remark-placeholder="remarkPlaceholder"
    />

    <template #footer>
      <div class="flex justify-end gap-4">
        <button
            class="text-gray-600 hover:text-gray-900 transition"
            @click="handleCancel"
        >
          取消
        </button>
        <button
            class="bg-blue-600 text-white px-4 py-2 rounded disabled:bg-blue-300 disabled:cursor-not-allowed transition"
            :disabled="loading || !isFormValid"
            @click="handleSubmit"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import BaseModal from '@/components/base/BaseModal.vue'
import FitnessForm from './FitnessForm.vue';

const props = defineProps({
  show: Boolean,
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

const emit = defineEmits(['cancel', 'submit']);

const formRef = ref(null);

const isFormValid = computed(() => {
  if (!formRef.value) return false;
  return formRef.value.isValid;
});

function handleCancel() {
  emit('cancel');
}

function handleSubmit() {
  if (!isFormValid.value || props.loading) return;
  emit('submit');
}
</script>
