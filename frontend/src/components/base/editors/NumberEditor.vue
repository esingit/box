<!-- src/components/base/editors/NumberEditor.vue -->
<template>
  <input
      :value="modelValue"
      type="number"
      :step="column.step || '0.01'"
      :class="inputClass"
      :placeholder="column.placeholder"
      :readonly="column.readonly"
      @input="handleInput"
      @blur="$emit('blur')"
  />
</template>

<script setup>
import { computed } from 'vue'

defineProps({
  modelValue: [String, Number],
  column: Object,
  record: Object,
  index: Number
})

const emit = defineEmits(['update:modelValue', 'blur'])

const inputClass = computed(() =>
    'w-full px-2 py-1 border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent'
)

const handleInput = (e) => {
  const value = e.target.value === '' ? null : Number(e.target.value)
  emit('update:modelValue', value)
}
</script>