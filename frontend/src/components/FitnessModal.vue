<template>
  <div v-if="show" class="edit-modal">
    <div class="modal-content">
      <button class="close-btn" @click="$emit('cancel')" title="关闭">
        <LucideX class="close-icon" />
      </button>
      <h3>{{ title }}</h3>
      <select v-model="form.type" class="type-select fitness-select">
        <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
      </select>
      <input v-model.number="form.count" type="number" min="1" class="count-input fitness-input" required placeholder="数量" />
      <select v-model="form.unit" class="unit-select fitness-select">
        <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
      </select>
      <input v-model="form.finishTime" type="date" class="date-input fitness-input" required />
      <input v-model="form.remark" type="text" class="remark-input fitness-input" :placeholder="remarkPlaceholder" />
      <div class="modal-actions">
        <button class="btn btn-white" :disabled="loading" @click="$emit('submit')">{{ confirmText }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { LucideX } from 'lucide-vue-next'
const props = defineProps({
  show: Boolean,
  form: Object,
  types: Array,
  units: Array,
  loading: Boolean,
  title: {
    type: String,
    default: '操作单据'
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  remarkPlaceholder: {
    type: String,
    default: '备注（可选）'
  }
})
const emit = defineEmits(['submit', 'cancel'])
</script>
