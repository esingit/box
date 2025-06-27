<!-- src/components/base/editors/DateEditor.vue -->
<template>
  <div class="relative w-full">
    <BaseDateInput
        :model-value="formattedValue"
        :type="dateType"
        :placeholder="placeholder"
        :clearable="column.clearable !== false"
        :disabled="column.readonly"
        :range="column.range"
        :min="minDate"
        :max="maxDate"
        :disabled-dates="disabledDates"
        :shortcuts="shortcuts"
        :format="displayFormat"
        @update:model-value="handleChange"
        @blur="handleBlur"
        @focus="handleFocus"
        class="w-full"
    />

    <!-- 快速操作按钮 -->
    <div v-if="showQuickActions && !column.readonly" class="absolute right-8 top-1/2 -translate-y-1/2 flex gap-1">
      <button
          v-if="showTodayButton"
          @click.stop="setToday"
          class="px-2 py-1 text-xs text-gray-600 hover:text-green-600 transition-colors"
          title="今天"
      >
        今天
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'

const props = defineProps({
  modelValue: [String, Date],
  column: Object,
  record: Object,
  index: Number
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus'])

const isFocused = ref(false)

// 日期格式配置
const dateFormats = {
  date: {
    format: 'YYYY-MM-DD',
    display: 'YYYY年MM月DD日',
    placeholder: '请选择日期'
  },
  datetime: {
    format: 'YYYY-MM-DD HH:mm:ss',
    display: 'YYYY年MM月DD日 HH:mm',
    placeholder: '请选择日期时间'
  },
  month: {
    format: 'YYYY-MM',
    display: 'YYYY年MM月',
    placeholder: '请选择月份'
  },
  year: {
    format: 'YYYY',
    display: 'YYYY年',
    placeholder: '请选择年份'
  }
}

// 计算日期类型
const dateType = computed(() => {
  return props.column.dateType || 'date'
})

// 计算显示格式
const displayFormat = computed(() => {
  if (props.column.format) {
    return props.column.format
  }
  return dateFormats[dateType.value]?.display || 'YYYY-MM-DD'
})

// 计算占位符
const placeholder = computed(() => {
  if (props.column.placeholder) {
    return props.column.placeholder
  }
  return dateFormats[dateType.value]?.placeholder || '请选择'
})

// 格式化值
const formattedValue = computed(() => {
  if (!props.modelValue) return ''

  // 如果是字符串，尝试解析
  if (typeof props.modelValue === 'string') {
    // 验证日期格式
    const datePatterns = [
      /^\d{4}-\d{2}-\d{2}$/,
      /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/,
      /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}$/
    ]

    if (datePatterns.some(pattern => pattern.test(props.modelValue))) {
      return props.modelValue
    }
  }

  // 如果是 Date 对象，转换为字符串
  if (props.modelValue instanceof Date) {
    return formatDate(props.modelValue)
  }

  return props.modelValue
})

// 最小日期
const minDate = computed(() => {
  if (props.column.min) {
    return props.column.min
  }

  // 根据业务逻辑设置默认最小日期
  if (props.column.key === 'finishTime' && props.record?.acquireTime) {
    return props.record.acquireTime
  }

  return null
})

// 最大日期
const maxDate = computed(() => {
  if (props.column.max) {
    return props.column.max
  }

  // 根据业务逻辑设置默认最大日期
  if (props.column.key === 'acquireTime' && props.record?.finishTime) {
    return props.record.finishTime
  }

  return null
})

// 禁用的日期
const disabledDates = computed(() => {
  if (typeof props.column.disabledDate === 'function') {
    return props.column.disabledDate
  }

  // 默认禁用逻辑
  return (date) => {
    // 禁用未来日期（可根据需求调整）
    if (props.column.disableFuture && date > new Date()) {
      return true
    }

    // 禁用过去日期
    if (props.column.disablePast && date < new Date()) {
      return true
    }

    return false
  }
})

// 快捷选项
const shortcuts = computed(() => {
  if (props.column.shortcuts === false) {
    return []
  }

  if (Array.isArray(props.column.shortcuts)) {
    return props.column.shortcuts
  }

  // 默认快捷选项
  const defaultShortcuts = [
    {
      text: '今天',
      value: () => new Date()
    },
    {
      text: '昨天',
      value: () => {
        const date = new Date()
        date.setDate(date.getDate() - 1)
        return date
      }
    },
    {
      text: '一周前',
      value: () => {
        const date = new Date()
        date.setDate(date.getDate() - 7)
        return date
      }
    }
  ]

  if (dateType.value === 'datetime') {
    defaultShortcuts.push({
      text: '现在',
      value: () => new Date()
    })
  }

  return defaultShortcuts
})

// 是否显示快速操作
const showQuickActions = computed(() => {
  return props.column.quickActions !== false && isFocused.value
})

// 是否显示今天按钮
const showTodayButton = computed(() => {
  return dateType.value === 'date' || dateType.value === 'datetime'
})

// 格式化日期
function formatDate(date) {
  if (!date) return ''

  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  switch (dateType.value) {
    case 'datetime':
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    case 'month':
      return `${year}-${month}`
    case 'year':
      return `${year}`
    default:
      return `${year}-${month}-${day}`
  }
}

// 处理值变更
const handleChange = (value) => {
  // 验证日期有效性
  if (value) {
    const date = new Date(value)
    if (isNaN(date.getTime())) {
      console.warn('Invalid date:', value)
      return
    }
  }

  emit('update:modelValue', value)
}

// 处理失焦
const handleBlur = () => {
  isFocused.value = false
  emit('blur')
}

// 处理聚焦
const handleFocus = () => {
  isFocused.value = true
  emit('focus')
}

// 设置为今天
const setToday = () => {
  const today = formatDate(new Date())
  handleChange(today)
}
</script>