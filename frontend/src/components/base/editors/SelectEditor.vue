<!-- src/components/base/editors/SelectEditor.vue -->
<template>
  <div class="relative w-full">
    <BaseSelect
        :model-value="normalizedValue"
        :title="column.label || column.title || ''"
        :options="column.options || []"
        :multiple="column.multiple"
        :clearable="column.clearable !== false"
        :placeholder="column.placeholder"
        :searchable="column.searchable !== false"
        :direction="dropdownDirection"
        class="w-full"
        @update:model-value="handleChange"
        @blur="handleBlur"
        @dropdown-open="handleDropdownOpen"
        @dropdown-close="handleDropdownClose"
    />
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'

const props = defineProps({
  modelValue: [String, Number, Array],
  column: Object,
  record: Object,
  index: Number
})

const emit = defineEmits(['update:modelValue', 'blur'])

// 下拉方向
const dropdownDirection = ref('down')
const selectRef = ref(null)

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

const handleBlur = () => {
  emit('blur')
}

// 处理下拉框打开
const handleDropdownOpen = () => {
  // 计算下拉方向
  calculateDropdownDirection()

  // 通知父组件（表格）当前有下拉框打开
  if (props.index !== undefined) {
    const event = new CustomEvent('table-dropdown-open', {
      detail: { rowIndex: props.index },
      bubbles: true
    })
    selectRef.value?.dispatchEvent(event)
  }
}

// 处理下拉框关闭
const handleDropdownClose = () => {
  // 通知父组件（表格）下拉框已关闭
  if (props.index !== undefined) {
    const event = new CustomEvent('table-dropdown-close', {
      detail: { rowIndex: props.index },
      bubbles: true
    })
    selectRef.value?.dispatchEvent(event)
  }
}

// 计算下拉方向
const calculateDropdownDirection = () => {
  if (!selectRef.value) return

  const rect = selectRef.value.getBoundingClientRect()
  const viewportHeight = window.innerHeight
  const spaceBelow = viewportHeight - rect.bottom
  const spaceAbove = rect.top

  // 如果下方空间不足且上方空间更大，则向上展开
  dropdownDirection.value = spaceBelow < 300 && spaceAbove > spaceBelow ? 'up' : 'down'
}

// 监听窗口大小变化
const handleResize = () => {
  if (selectRef.value) {
    calculateDropdownDirection()
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>