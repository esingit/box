<template>
  <div class="relative w-full" @click="focusInput">
    <component
        :is="isTextareaType ? 'textarea' : 'input'"
        ref="inputRef"
        v-bind="$attrs"
        :value="inputDisplayValue"
        :type="isTextareaType ? undefined : type"
        :placeholder="computedPlaceholder"
        :disabled="disabled"
        :title="errorTooltip || title"
        :min="isNumberType ? min : undefined"
        :max="isNumberType ? max : undefined"
        :step="isNumberType ? step : undefined"
        @input="handleInput"
        @blur="handleBlur"
        class="input-base pr-12 appearance-none"
        :class="{
          'msg-error': showError,
          'min-h-[80px] resize-y': isTextareaType,
          'hide-number-arrows': isNumberType
        }"
    />

    <!-- 清除按钮 -->
    <button
        v-if="clearable && !disabled && hasValue"
        @click.stop="clearInput"
        type="button"
        tabindex="-1"
        title="清空"
        :class="[
          'absolute text-gray-400 hover:text-gray-600 transition',
          isTextareaType ? 'top-2 right-2' : 'top-1/2 -translate-y-1/2',
          isNumberType && !isTextareaType ? 'right-12' : 'right-4'
        ]"
    >
      <LucideX class="w-4 h-4" />
    </button>

    <!-- 数字调节按钮 -->
    <div
        v-if="isNumberType"
        class="absolute top-1/2 right-3 -translate-y-1/2 flex flex-col justify-center select-none"
        style="width: 28px; height: 32px;"
    >
      <button
          type="button"
          @mousedown="handleMouseDown('up', $event)"
          @mouseup="handleMouseUp"
          @mouseleave="handleMouseLeave"
          @contextmenu="handleContextMenu"
          @touchstart="handleTouchStart('up', $event)"
          @touchend="handleTouchEnd"
          @touchcancel="handleTouchEnd"
          class="flex-1 flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors user-select-none"
          :disabled="disabled || (max !== undefined && numericValue >= max)"
          tabindex="-1"
          title="增加"
      >
        <LucideChevronUp class="w-4 h-4" />
      </button>
      <button
          type="button"
          @mousedown="handleMouseDown('down', $event)"
          @mouseup="handleMouseUp"
          @mouseleave="handleMouseLeave"
          @contextmenu="handleContextMenu"
          @touchstart="handleTouchStart('down', $event)"
          @touchend="handleTouchEnd"
          @touchcancel="handleTouchEnd"
          class="flex-1 flex items-center justify-center text-gray-400 hover:text-gray-600 transition-colors user-select-none"
          :disabled="disabled || (min !== undefined && numericValue <= min)"
          tabindex="-1"
          title="减少"
      >
        <LucideChevronDown class="w-4 h-4" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { LucideX, LucideChevronUp, LucideChevronDown } from 'lucide-vue-next'
import Decimal from 'decimal.js'

// 设置 Decimal 的全局配置
Decimal.set({
  precision: 20,  // 精度
  rounding: Decimal.ROUND_HALF_UP  // 四舍五入
})

const props = withDefaults(defineProps<{
  modelValue?: string | number
  title?: string
  placeholder?: string
  type?: 'text' | 'number' | 'textarea' | 'password'
  disabled?: boolean
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
  min?: number
  max?: number
  step?: number | string
  precision?: number  // 显示精度（小数位数）
  trimZeros?: boolean  // 是否去除末尾的0
  longPressDelay?: number  // 长按延迟时间（ms）
  longPressSpeed?: number  // 长按初始速度（ms）
}>(), {
  title: '',
  placeholder: '',
  type: 'text',
  disabled: false,
  clearable: true,
  required: false,
  requiredMessage: '',
  step: 0.01,
  trimZeros: true,
  longPressDelay: 500,
  longPressSpeed: 150
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void
  (e: 'change', value: string | number): void
  (e: 'blur', event: FocusEvent): void
  (e: 'focus', event: FocusEvent): void
}>()

const inputRef = ref<HTMLInputElement | HTMLTextAreaElement | null>(null)
defineExpose({
  inputRef,
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur()
})

const isNumberType = computed(() => props.type === 'number')
const isTextareaType = computed(() => props.type === 'textarea')

const innerValue = ref(props.modelValue ?? '')

// 获取数字值
const numericValue = computed(() => {
  if (!isNumberType.value || !hasValue.value) return 0
  return Number(innerValue.value)
})

// 获取小数位数
const decimalPlaces = computed(() => {
  if (props.precision !== undefined) return props.precision

  const stepStr = String(props.step)
  if (stepStr === 'any') return 10  // 默认最多10位小数

  const parts = stepStr.split('.')
  return parts.length > 1 ? parts[1].length : 0
})

// 格式化数字
function formatNumber(value: number | string | Decimal): number {
  if (value === '' || value === null || value === undefined) return 0

  try {
    const decimal = value instanceof Decimal ? value : new Decimal(value)

    // 使用指定的小数位数
    let formatted = decimal.toFixed(decimalPlaces.value)

    // 如果需要去除末尾的0
    if (props.trimZeros) {
      formatted = parseFloat(formatted).toString()
    }

    return Number(formatted)
  } catch (e) {
    console.warn('Number format error:', e)
    return 0
  }
}

// 验证数字是否在范围内
function clampValue(value: Decimal): Decimal {
  let result = value

  if (props.min !== undefined && result.lessThan(props.min)) {
    result = new Decimal(props.min)
  }

  if (props.max !== undefined && result.greaterThan(props.max)) {
    result = new Decimal(props.max)
  }

  return result
}

// 显示值
const inputDisplayValue = computed(() => {
  if (!hasValue.value) return ''

  if (isNumberType.value && typeof innerValue.value === 'number') {
    return String(innerValue.value)
  }

  return String(innerValue.value)
})

const hasValue = computed(() =>
    innerValue.value !== '' && innerValue.value !== null && innerValue.value !== undefined
)

const showError = ref(false)

const errorTooltip = computed(() =>
    showError.value ? (props.requiredMessage || `请输入${props.title || '内容'}`) : ''
)

const computedPlaceholder = computed(() => {
  if (showError.value && props.required) {
    return `请输入${props.title || '内容'}`
  }
  return props.placeholder || `请输入${props.title || '内容'}`
})

defineOptions({ inheritAttrs: false })

watch(() => props.modelValue, (val) => {
  innerValue.value = val ?? ''
  if (val !== '' && val !== null && val !== undefined) {
    showError.value = false
  }
}, { immediate: true })

function clearInput() {
  if (props.disabled) return
  innerValue.value = ''
  emit('update:modelValue', '')
  emit('change', '')
  showError.value = props.required
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
    // 允许输入负号和小数点
    if (val === '-' || val === '.' || val === '-.') {
      innerValue.value = val
      return
    }

    // 验证是否是有效数字
    const num = Number(val)
    if (isNaN(num)) return

    // 暂时保存原始输入，不做格式化
    innerValue.value = num
    emit('update:modelValue', num)
  } else {
    innerValue.value = val
    emit('update:modelValue', val)
  }
}

// 失去焦点时进行格式化和范围检查
function handleBlur(event: FocusEvent) {
  validate()

  if (isNumberType.value && hasValue.value && typeof innerValue.value === 'number') {
    try {
      let decimal = new Decimal(innerValue.value)
      decimal = clampValue(decimal)

      const formatted = formatNumber(decimal)
      if (formatted !== innerValue.value) {
        innerValue.value = formatted
        emit('update:modelValue', formatted)
        emit('change', formatted)

        // 更新输入框显示
        nextTick(() => {
          if (inputRef.value) {
            inputRef.value.value = String(formatted)
          }
        })
      }
    } catch (e) {
      // 处理无效输入
      const fallbackValue = props.min ?? 0
      innerValue.value = fallbackValue
      emit('update:modelValue', fallbackValue)
      emit('change', fallbackValue)
    }
  }

  emit('blur', event)
}

function handleFocus(event: FocusEvent) {
  emit('focus', event)
}

// 步进控制相关
let interval: number | null = null
let initialTimeout: number | null = null
let isMouseDown = false
let currentDirection: 'up' | 'down' | null = null

function stepOnce(direction: 'up' | 'down') {
  if (props.disabled) return

  try {
    const current = new Decimal(innerValue.value || 0)
    const stepValue = props.step === 'any' ? 1 : Number(props.step)
    const step = new Decimal(stepValue)

    let next = direction === 'up'
        ? current.plus(step)
        : current.minus(step)

    // 应用范围限制
    next = clampValue(next)

    // 检查是否已经到达边界
    if (props.max !== undefined && direction === 'up' && next.greaterThanOrEqualTo(props.max)) {
      next = new Decimal(props.max)
    }
    if (props.min !== undefined && direction === 'down' && next.lessThanOrEqualTo(props.min)) {
      next = new Decimal(props.min)
    }

    // 格式化结果
    const formatted = formatNumber(next)

    if (formatted !== innerValue.value) {
      innerValue.value = formatted
      emit('update:modelValue', formatted)
      emit('change', formatted)

      // 更新输入框显示
      nextTick(() => {
        if (inputRef.value) {
          inputRef.value.value = String(formatted)
        }
      })
    }
  } catch (e) {
    console.error('Step calculation error:', e)
  }
}

function startStep(direction: 'up' | 'down') {
  if (props.disabled) return

  // 检查边界条件
  if (direction === 'up' && props.max !== undefined && numericValue.value >= props.max) return
  if (direction === 'down' && props.min !== undefined && numericValue.value <= props.min) return

  isMouseDown = true
  currentDirection = direction

  // 立即执行一次
  stepOnce(direction)

  // 清除之前的定时器
  stopStep()

  // 设置初始延迟
  initialTimeout = window.setTimeout(() => {
    if (isMouseDown && currentDirection === direction) {
      startContinuousStep(direction)
    }
  }, props.longPressDelay)
}

function startContinuousStep(direction: 'up' | 'down') {
  let currentInterval = props.longPressSpeed

  const continuousStep = () => {
    if (!isMouseDown || currentDirection !== direction) return

    // 检查边界条件
    if (direction === 'up' && props.max !== undefined && numericValue.value >= props.max) {
      stopStep()
      return
    }
    if (direction === 'down' && props.min !== undefined && numericValue.value <= props.min) {
      stopStep()
      return
    }

    stepOnce(direction)

    // 逐渐加快速度，但有最小间隔限制
    currentInterval = Math.max(50, currentInterval - 5)

    interval = window.setTimeout(continuousStep, currentInterval)
  }

  continuousStep()
}

function stopStep() {
  isMouseDown = false
  currentDirection = null

  if (initialTimeout !== null) {
    clearTimeout(initialTimeout)
    initialTimeout = null
  }

  if (interval !== null) {
    clearTimeout(interval)
    interval = null
  }
}

// 鼠标事件处理
function handleMouseDown(direction: 'up' | 'down', event: MouseEvent) {
  event.preventDefault()
  startStep(direction)
}

function handleMouseUp() {
  stopStep()
}

function handleMouseLeave() {
  stopStep()
}

function handleContextMenu(event: MouseEvent) {
  event.preventDefault()
  stopStep()
}

// 触摸事件处理（移动端支持）
function handleTouchStart(direction: 'up' | 'down', event: TouchEvent) {
  event.preventDefault()
  startStep(direction)
}

function handleTouchEnd() {
  stopStep()
}

function validate() {
  const val = innerValue.value
  showError.value = props.required && (val === '' || val === null || val === undefined)
}

function focusInput() {
  if (props.disabled) return
  inputRef.value?.focus()
}

// 全局事件监听（防止鼠标在按钮外释放时状态错误）
function handleGlobalMouseUp() {
  if (isMouseDown) {
    stopStep()
  }
}

onMounted(() => {
  document.addEventListener('mouseup', handleGlobalMouseUp)
  document.addEventListener('touchend', handleGlobalMouseUp)

  nextTick(() => {
    const el = inputRef.value
    if (el?.value && el.value !== inputDisplayValue.value) {
      handleInput({ target: el } as unknown as Event)
    }
  })
})

onBeforeUnmount(() => {
  stopStep()
  document.removeEventListener('mouseup', handleGlobalMouseUp)
  document.removeEventListener('touchend', handleGlobalMouseUp)
})
</script>

<style scoped>
/* 隐藏数字输入框的上下箭头 */
.hide-number-arrows::-webkit-outer-spin-button,
.hide-number-arrows::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.hide-number-arrows {
  -moz-appearance: textfield;
}

/* 错误状态下的 placeholder 样式 */
.msg-error::placeholder {
  color: var(--color-error);
}

/* 防止用户选择按钮文本 */
.user-select-none {
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
}

/* 按钮悬停效果 */
.transition-colors {
  transition: color 0.15s ease-in-out;
}
</style>