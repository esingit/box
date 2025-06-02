<template>
  <div v-if="show" class="edit-modal">
    <div class="modal-content">
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
        <button @click="$emit('cancel')" class="delete-btn">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  show: Boolean,
  editForm: Object,
  types: Array,
  units: Array
})
</script>
