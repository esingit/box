<template>
  <div v-if="show" class="edit-modal">
    <div class="modal-content">
      <button class="close-btn" @click="$emit('cancel')" title="关闭">
        <LucideX class="close-icon" />
      </button>
      <h3>编辑记录</h3>
      <select v-model="editForm.type" class="type-select fitness-select">
        <option v-for="type in types" :key="type" :value="type">{{ type }}</option>
      </select>
      <input type="number" v-model.number="editForm.count" min="1" class="count-input fitness-input" />
      <select v-model="editForm.unit" class="unit-select fitness-select">
        <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
      </select>
      <input type="date" v-model="editForm.finishTime" class="date-input fitness-input" required />
      <input type="text" v-model="editForm.remark" class="remark-input fitness-input" placeholder="备注" />
      <div class="modal-actions">
        <button @click="$emit('save')" class="add-btn">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { LucideX } from 'lucide-vue-next'
const props = defineProps({
  show: Boolean,
  editForm: Object,
  types: Array,
  units: Array
})
</script>
