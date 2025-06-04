<template>
  <div v-if="show" class="edit-modal">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">{{ title }}</h3>
        <button class="close-button" @click="$emit('cancel')">
          <LucideX class="close-icon" />
        </button>
      </div>
      <div class="input-group">
        <label class="input-label">类型</label>
        <select v-model="form.type" class="type-select select">
          <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
        </select>
      </div>
      <div class="input-group">
        <label class="input-label">数量</label>
        <input v-model.number="form.count" type="number" min="1" class="count-input input" required placeholder="数量" />
      </div>
      <div class="input-group">
        <label class="input-label">单位</label>
        <select v-model="form.unit" class="unit-select select">
          <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
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
