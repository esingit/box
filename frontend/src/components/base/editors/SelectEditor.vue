<template>
  <div class="relative w-full">
    <input
        type="text"
        :value="displayValue"
        readonly
        class="w-full px-2 py-1 border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent cursor-pointer"
        :placeholder="column?.placeholder || '请选择'"
        @click="openDropdown"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'

export default {
  name: 'SelectEditor',
  props: {
    modelValue: {
      type: [String, Number, Array],
      default: null
    },
    column: {
      type: Object,
      default: () => ({})
    },
    record: {
      type: Object,
      default: () => ({})
    },
    index: {
      type: Number,
      default: null
    }
  },
  emits: ['update:model-value', 'blur'],
  setup(props, { emit }) {
    // 计算显示值
    const displayValue = computed(() => {
      const options = props.column?.options || []
      const value = props.modelValue

      if (!value) return ''

      if (Array.isArray(value)) {
        const labels = options
            .filter(opt => value.includes(String(opt.value)))
            .map(opt => opt.label)
        return labels.join(', ')
      } else {
        const option = options.find(opt => String(opt.value) === String(value))
        return option ? option.label : value
      }
    })

    // 打开下拉框
    function openDropdown() {
      // 这里只是一个占位符，实际上我们不会实现下拉功能
      // 因为我们正在调试问题
      console.log('打开下拉框')
    }

    return {
      displayValue,
      openDropdown
    }
  }
}
</script>