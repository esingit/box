<!-- src/components/base/editors/DateEditor.vue -->
<template>
  <BaseDateInput
      :model-value="modelValue"
      :type="dateType"
      :placeholder="column.placeholder"
      :clearable="column.clearable !== false"
      :disabled="column.readonly"
      :range="column.range"
      @update:model-value="handleChange"
      @blur="$emit('blur')"
  />
</template>

<script setup>
import { computed } from 'vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'

const props = defineProps({
  modelValue: [String, Date],
  column: Object,
  record: Object,
  index: Number
})

const emit = defineEmits(['update:modelValue', 'blur'])

// 计算日期类型
const dateType = computed(() => {
  // 根据列配置决定类型，默认为 date
  return props.column.dateType || 'date'
})

// 处理值变更
const handleChange = (value) => {
  emit('update:modelValue', value)
}
</script>