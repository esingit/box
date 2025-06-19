<template>
  <div class="relative w-full">
    <input
        ref="inputRef"
        v-model="innerValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :title="errorTooltip"
        :class="[
        'input-base pr-5',
        showError ? 'border-red-500 ring-1 ring-red-500' : ''
      ]"
        @blur="onBlur"
    />

    <!-- 清空按钮 -->
    <button
        v-if="clearable && innerValue"
        @click="clear"
        class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition"
        title="清空"
    >
      <LucideX class="w-4 h-4" />
    </button>

    <!-- 错误提示文字 -->
    <p v-if="showError" class="text-xs text-red-500 mt-1 select-none">
      {{ requiredMessage || '此项为必填' }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { LucideX } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  type?: string
  disabled?: boolean
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
}>(), {
  modelValue: '',
  placeholder: '请输入内容',
  type: 'text',
  disabled: false,
  clearable: true,
  required: false,
  requiredMessage: ''
})

const emit = defineEmits(['update:modelValue'])

const innerValue = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

const inputRef = ref<HTMLInputElement | null>(null)

const isEmpty = computed(() => innerValue.value.trim?.() === '')

const showError = ref(false)

const errorTooltip = computed(() => (showError.value ? (props.requiredMessage || '此项为必填') : ''))

function clear() {
  emit('update:modelValue', '')
  showError.value = false
}

// 失焦时触发校验
function onBlur() {
  if (props.required && isEmpty.value) {
    showError.value = true
    // 自动聚焦
    nextTick(() => inputRef.value?.focus())
  } else {
    showError.value = false
  }
}

// 如果外部 modelValue 改变了，自动重置错误状态
watch(() => props.modelValue, () => {
  if (!isEmpty.value) showError.value = false
})
</script>
