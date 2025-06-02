<template>
  <form class="fitness-form" @submit.prevent="onSubmit">
    <select v-model="form.type" class="type-select">
      <option v-for="type in types" :key="type" :value="type">{{ type }}</option>
    </select>
    <input type="number" v-model.number="form.count" min="1" placeholder="数量" class="count-input" />
    <select v-model="form.unit" class="unit-select">
      <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
    </select>
    <input type="date" v-model="form.finishTime" class="date-input" required />
    <input type="text" v-model="form.remark" class="remark-input" placeholder="备注" />
    <button type="submit" :disabled="adding" class="add-btn">{{ adding ? '添加中...' : '添加' }}</button>
  </form>
</template>

<script setup>
import { toRefs } from 'vue'
const props = defineProps({
  form: Object,
  types: Array,
  units: Array,
  adding: Boolean
})
const emit = defineEmits(['submit'])
const { form, types, units, adding } = toRefs(props)
function onSubmit() {
  emit('submit')
}
</script>
