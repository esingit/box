<template>
  <div class="relative w-full" @click="focusInput">
    <component
        :is="isTextareaType ? 'textarea' : 'input'"
        ref="inputRef"
        :value="inputDisplayValue"
        :type="isTextareaType ? undefined : type"
        :placeholder="computedPlaceholder"
        :disabled="disabled"
        :title="errorTooltip || title"
        :min="isNumberType ? min : undefined"
        :max="isNumberType ? max : undefined"
        :step="isNumberType ? step : undefined"
        autocomplete="on"
        @input="handleInput"
        @blur="validate"
        class="input-base pr-12 appearance-none"
        :class="{
        'msg-error': showError,
        'min-h-[80px] resize-y': isTextareaType
      }"
    />

    <!-- 清除按钮（支持 textarea） -->
    <button
        v-if="clearable && !disabled && hasValue"
        @click.stop="clearInput"
        type="button"
        tabindex="-1"
        title="清空"
        :class="[
        'absolute text-gray-400 hover:text-gray-600 transition',
        isTextareaType ? 'top-2 right-2' : 'top-1/2 -translate-y-1/2',
        isNumberType && !isTextareaType ? 'right-10' : 'right-5'
      ]"
    >
      <LucideX class="w-4 h-4" />
    </button>

    <!-- 数字调节按钮 -->
    <div
        v-if="isNumberType"
        class="absolute top-1/2 right-2 -translate-y-1/2 flex flex-col justify-center"
        style="width: 28px; height: 32px;"
    >
      <button
          type="button"
          @mousedown.prevent="stepHeld('up')"
          @mouseup="stopHold"
          @mouseleave="stopHold"
          class="flex-1 flex items-center justify-center text-gray-400 hover:text-gray-600"
          :disabled="disabled"
          tabindex="-1"
      >
        <LucideChevronUp class="w-4 h-4" />
      </button>
      <button
          type="button"
          @mousedown.prevent="stepHeld('down')"
          @mouseup="stopHold"
          @mouseleave="stopHold"
          class="flex-1 flex items-center justify-center text-gray-400 hover:text-gray-600"
          :disabled="disabled"
          tabindex="-1"
      >
        <LucideChevronDown class="w-4 h-4" />
      </button>
    </div>
  </div>

  <p v-if="showError" class="msg-error mt-1 ml-1">
    {{ requiredMessage || `请输入${title || '内容'}` }}
  </p>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { LucideX, LucideChevronUp, LucideChevronDown } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: string | number
  title?: string
  placeholder?: string
  type?: 'text' | 'number' | 'textarea'
  disabled?: boolean
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
  min?: number
  max?: number
  step?: number
}>(), {
  title: '',
  placeholder: '',
  type: 'text',
  disabled: false,
  clearable: true,
  required: false,
  requiredMessage: '',
  step: 1
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
}>()

const inputRef = ref<HTMLInputElement | HTMLTextAreaElement | null>(null)
defineExpose({ inputRef })

const isNumberType = computed(() => props.type === 'number')
const isTextareaType = computed(() => props.type === 'textarea')

const innerValue = ref(props.modelValue ?? '')

const inputDisplayValue = computed(() =>
    innerValue.value === null || innerValue.value === undefined ? '' : String(innerValue.value)
)

const hasValue = computed(() =>
    innerValue.value !== '' && innerValue.value !== null && innerValue.value !== undefined
)

const showError = ref(false)

const errorTooltip = computed(() =>
    showError.value ? (props.requiredMessage || `请输入${props.title || '内容'}`) : ''
)

const computedPlaceholder = computed(() =>
    props.placeholder || `请输入${props.title || '内容'}`
)

watch(() => props.modelValue, (val) => {
  innerValue.value = val ?? ''
  if (val !== '' && val !== null && val !== undefined) {
    showError.value = false
  }
})

function clearInput() {
  if (props.disabled) return
  innerValue.value = ''
  emit('update:modelValue', '')
  showError.value = false
  inputRef.value?.focus()
}

function handleInput(e: Event) {
  if (props.disabled) return
  const val = (e.target as HTMLInputElement | HTMLTextAreaElement).value
  if (val === '') {
    innerValue.value = ''
    emit('update:modelValue', '')
    return
  }
  if (isNumberType.value) {
    const num = Number(val)
    if (isNaN(num)) return
    let final = num
    if (props.min !== undefined && final < props.min) final = props.min
    if (props.max !== undefined && final > props.max) final = props.max
    innerValue.value = final
    emit('update:modelValue', final)
  } else {
    innerValue.value = val
    emit('update:modelValue', val)
  }
}

let interval: number | null = null

function stepOnce(direction: 'up' | 'down') {
  const current = Number(innerValue.value) || 0
  const step = props.step ?? 1
  const next = direction === 'up' ? current + step : current - step
  if (props.max !== undefined && next > props.max) return
  if (props.min !== undefined && next < props.min) return
  innerValue.value = next
  emit('update:modelValue', next)
}

function stepHeld(direction: 'up' | 'down') {
  stepOnce(direction)
  stopHold()
  interval = window.setInterval(() => stepOnce(direction), 150)
}

function stopHold() {
  if (interval !== null) {
    clearInterval(interval)
    interval = null
  }
}

function validate() {
  const val = innerValue.value
  showError.value = props.required && (val === '' || val === null || val === undefined)
}

function focusInput() {
  if (props.disabled) return
  const el = inputRef.value
  if (!el) return
  el.focus()
  const val = el.value
  el.value = ''
  el.value = val
}

onMounted(() => {
  nextTick(() => {
    const el = inputRef.value
    if (el?.value && el.value !== inputDisplayValue.value) {
      handleInput({ target: el } as unknown as Event)
    }
  })
})
</script>

<style scoped>
input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type='number'] {
  -moz-appearance: textfield;
}
</style>
