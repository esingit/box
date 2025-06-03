<template>
  <div v-if="show" class="edit-modal">
    <div class="modal-content">
      <h3>添加单据</h3>
      <form class="fitness-form" @submit.prevent="onSubmit">
        <select v-model="form.type" class="type-select" required>
          <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
        </select>
        <input v-model.number="form.count" type="number" min="1" class="count-input" required placeholder="数量" />
        <select v-model="form.unit" class="unit-select" required>
          <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
        </select>
        <input v-model="form.finishTime" type="date" class="date-input" required />
        <input v-model="form.remark" type="text" class="remark-input" placeholder="备注（可选）" />
      </form>
      <div class="modal-actions">
        <button class="add-btn" :disabled="adding" @click="onSubmit">确定</button>
        <button class="delete-btn" type="button" @click="$emit('cancel')">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { toRefs, reactive, watch } from 'vue'
const props = defineProps({
  show: Boolean,
  form: Object,
  types: Array,
  units: Array,
  adding: Boolean
})
const emit = defineEmits(['submit', 'cancel'])
const { show, form, types, units, adding } = toRefs(props)

function onSubmit() {
  emit('submit')
}
</script>
