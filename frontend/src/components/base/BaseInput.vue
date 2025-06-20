<template>
  <div
      class="relative w-full"
      @click="focusInputManually"
  >
    <input
        ref="inputRef"
        autocomplete="on"
        :value="innerValue"
        :type="type"
        :placeholder="placeholder"
        :disabled="disabled"
        :title="errorTooltip"
        :class="[
          'input-base pr-8',
          showError ? 'msg-error' : ''
        ]"
        @input="onInput"
        @change="onInput"
        @blur="handleBlur"
    />

    <!-- 清除按钮 -->
    <button
        v-if="clearable && innerValue"
        @click.stop="clear"
        class="absolute right-2 top-1/2 -translate-y-1/2 z-10 text-gray-400 hover:text-gray-600 transition"
        title="清空"
        type="button"
    >
      <LucideX class="w-4 h-4" />
    </button>

    <!-- 错误提示 -->
    <p v-if="showError" class="msg-error">
      {{ requiredMessage || '此项为必填' }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
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
  placeholder: '请输入内容',
  type: 'text',
  disabled: false,
  clearable: true,
  required: false,
  requiredMessage: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const inputRef = ref<HTMLInputElement | null>(null)
defineExpose({ inputRef })

const innerValue = computed({
  get: () => props.modelValue ?? '',
  set: val => emit('update:modelValue', val)
})

const showError = ref(false)

const errorTooltip = computed(() =>
    showError.value ? (props.requiredMessage || '此项为必填') : ''
)

// 清空并聚焦
function clear() {
  emit('update:modelValue', '')
  showError.value = false
  inputRef.value?.focus()
}

// 输入事件
function onInput(e: Event) {
  const val = (e.target as HTMLInputElement).value
  emit('update:modelValue', val)
}

// 失焦校验
function handleBlur() {
  if (props.required && innerValue.value.trim() === '') {
    showError.value = true
  } else {
    showError.value = false
  }
}

// 监听外部值变化，清除错误
watch(() => props.modelValue, () => {
  if (innerValue.value.trim() !== '') {
    showError.value = false
  }
})

// 解决自动填充内容无法同步的问题
onMounted(() => {
  nextTick(() => {
    const el = inputRef.value
    if (el && el.value && el.value !== innerValue.value) {
      emit('update:modelValue', el.value)
    }
  })
})

// 点击外层时强制聚焦和光标
function focusInputManually() {
  const el = inputRef.value
  if (!el || props.disabled) return
  el.focus()
  // hack：强制光标显示，解决部分浏览器点击无光标问题
  const val = el.value
  el.value = ''
  el.value = val
}
</script>
