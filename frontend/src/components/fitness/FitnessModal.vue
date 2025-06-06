<template>
  <div v-if="show" class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal-container">
      <div class="modal-header">
        <h3 class="modal-title">{{ title }}</h3>
        <button class="close-button" @click="$emit('cancel')">
          <LucideX class="close-icon" />
        </button>
      </div>
      <div class="modal-body">
        <div class="input-group">
          <label class="input-label">类型</label>
          <select v-model="form.typeId" class="type-select select">
            <option v-for="type in types" :key="type.id" :value="type.id">{{ type.value1 }}</option>
          </select>
        </div>
        <div class="input-group">
          <label class="input-label">数量</label>
          <input v-model.number="form.count" type="number" min="0.01" step="0.01" class="count-input input" required placeholder="数量" />
        </div>
        <div class="input-group">
          <label class="input-label">单位</label>
          <select v-model="form.unitId" class="unit-select select">
            <option v-for="unit in units" :key="unit.id" :value="unit.id">{{ unit.value1 }}</option>
          </select>
        </div>
        <div class="input-group">
          <label class="input-label">完成日期</label>
          <input v-model="form.finishTime" type="date" class="date-input input" required />
        </div>
        <div class="input-group">
          <label class="input-label">备注</label>
          <input v-model="form.remark" type="text" class="remark-input input" :placeholder="remarkPlaceholder" />
        </div>
        <div class="modal-actions">
          <button class="btn btn-primary" :disabled="loading" @click="$emit('submit')">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { LucideX } from 'lucide-vue-next'
const props = defineProps({
  show: Boolean,
  form: Object,
  types: {
    type: Array,
    default: () => []
  },
  units: {
    type: Array,
    default: () => []
  },
  loading: Boolean,
  title: {
    type: String,
    default: '操作记录'
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

<style scoped>
.input-group {
  margin-bottom: var(--spacing-lg);
}

.input-label {
  display: block;
  margin-bottom: var(--spacing-sm);
  color: var(--text-primary);
  font-weight: 500;
}

.input,
.select {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.input:focus,
.select:focus {
  border-color: var(--text-primary);
  outline: none;
}

.input[type="number"] {
  appearance: textfield;
  -moz-appearance: textfield;
}

.input[type="number"]::-webkit-outer-spin-button,
.input[type="number"]::-webkit-inner-spin-button {
  -webkit-appearance: none;
  appearance: none;
  margin: 0;
}

/* 响应式调整 */
@media (max-width: 640px) {
  .modal-content {
    width: 95%;
    padding: var(--spacing-md);
  }
}
</style>
