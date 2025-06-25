<!-- src/components/base/editors/SelectEditor.vue -->
<template>
  <div class="relative">
    <BaseSelect
        :model-value="normalizedValue"
        :title="column.label || column.title || ''"
        :options="column.options || []"
        :multiple="column.multiple"
        :clearable="column.clearable"
        :placeholder="column.placeholder"
        class="w-full relative z-2500"
        @update:model-value="handleChange"
        @blur="$emit('blur')"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'

const props = defineProps({
  modelValue: [String, Number, Array],
  column: Object,
  record: Object,
  index: Number
})

const emit = defineEmits(['update:modelValue', 'blur'])

const normalizedValue = computed(() => {
  if (props.column.multiple) {
    if (props.modelValue === null || props.modelValue === undefined) {
      return []
    }
    return Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue]
  } else {
    return props.modelValue || null
  }
})

const handleChange = (value) => {
  emit('update:modelValue', value)
}
</script>