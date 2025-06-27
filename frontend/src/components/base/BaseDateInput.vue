<!--src/components/base/BaseDateInput.vue-->
<template>
  <div class="relative w-full" ref="container">
    <input
        ref="inputRef"
        type="text"
        readonly
        :value="displayValue"
        :placeholder="placeholderText"
        :class="[
          'input-base pr-12 appearance-none bg-white text-black rounded-md w-full cursor-pointer',
          showError ? 'input-error placeholder-red' : 'input-normal'
        ]"
        @click.stop="togglePopover"
        @blur="onBlur"
    />
    <!-- 清除按钮 -->
    <button
        v-if="clearable && hasValue"
        @click.stop="clearAll"
        type="button"
        aria-label="清空输入"
        class="absolute right-12 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 z-10"
    >
      <LucideX class="w-4 h-4" />
    </button>
    <!-- 日历图标 -->
    <div class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
      <LucideCalendar class="w-4 h-4 text-gray-400" />
    </div>

    <!-- 弹窗 -->
    <div
        v-if="open"
        ref="popoverRef"
        class="absolute z-50 mt-2 p-5 w-[520px] bg-white rounded-xl shadow-lg text-gray-800 select-none border"
        style="left: 0; top: 100%;"
        @click.stop
    >
      <!-- 快捷选项 -->
      <div class="flex space-x-3 mb-4" v-if="showQuickOptions">
        <BaseButton
            v-for="opt in quickOptions"
            :key="opt.label"
            class="flex-1 py-1 text-xs"
            :class="opt.isActive ? 'bg-blue-100 text-gray-800 border-blue-300' : ''"
            type="button"
            color="outline"
            @click="applyQuick(opt)"
        >
          {{ opt.label }}
        </BaseButton>
      </div>

      <!-- 选择区域 -->
      <div v-if="range" class="flex space-x-6">
        <div class="flex-1">
          <div class="text-sm mb-2 font-semibold text-gray-800">{{ startLabel }}</div>
          <BaseCalendar
              v-if="showDate"
              :modelValue="rangeStart"
              :maxDate="rangeEnd"
              @update:modelValue="val => updateRange('start', val)"
          />
          <BaseTimeSelector
              v-if="showTime"
              :modelValue="timeStart"
              :maxTime="rangeStart && rangeEnd && rangeStart === rangeEnd ? timeEnd : undefined"
              @update:modelValue="val => timeStart = val"
          />
        </div>
        <div class="flex-1">
          <div class="text-sm mb-2 font-semibold text-gray-800">{{ endLabel }}</div>
          <BaseCalendar
              v-if="showDate"
              :modelValue="rangeEnd"
              :minDate="rangeStart"
              @update:modelValue="val => updateRange('end', val)"
          />
          <BaseTimeSelector
              v-if="showTime"
              :modelValue="timeEnd"
              :minTime="rangeStart && rangeEnd && rangeStart === rangeEnd ? timeStart : undefined"
              @update:modelValue="val => timeEnd = val"
          />
        </div>
      </div>

      <div v-else>
        <div class="text-sm mb-2 font-semibold text-gray-800">{{ singleLabel }}</div>
        <BaseCalendar
            v-if="showDate"
            :modelValue="singleDate"
            @update:modelValue="val => singleDate = val"
            @confirm="handleConfirm"
        />
        <BaseTimeSelector
            v-if="showTime"
            :modelValue="singleTime"
            @update:modelValue="val => singleTime = val"
        />
      </div>

      <div class="flex justify-end mt-5 space-x-3">
        <BaseButton type="button" title="取消" color="outline" @click="cancel" />
        <BaseButton type="button" title="确认" color="primary" @click="confirm" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import dayjs from 'dayjs'
import { LucideX, LucideCalendar } from 'lucide-vue-next'
import BaseButton from './BaseButton.vue'
import BaseCalendar from './BaseCalendar.vue'
import BaseTimeSelector from './BaseTimeSelector.vue'

const props = withDefaults(
    defineProps<{
      modelValue?: string
      title?: string
      placeholder?: string
      clearable?: boolean
      required?: boolean
      requiredMessage?: string
      type?: 'date' | 'time' | 'datetime'
      range?: boolean
      disabled?: boolean
    }>(),
    {
      clearable: true,
      required: false,
      requiredMessage: '',
      type: 'date',
      range: false,
      disabled: false
    }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'blur'): void
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const container = ref<HTMLElement | null>(null)
const open = ref(false)
const showError = ref(false)

// 区间数据
const rangeStart = ref<string | null>(null)
const rangeEnd = ref<string | null>(null)
const timeStart = ref({ h: 0, m: 0, s: 0 })
const timeEnd = ref({ h: 23, m: 59, s: 59 })

// 单值数据
const singleDate = ref<string | null>(null)
const singleTime = ref({ h: 0, m: 0, s: 0 })

// 计算显示日期、时间的条件
const showDate = computed(() => props.type === 'date' || props.type === 'datetime')
const showTime = computed(() => props.type === 'time' || props.type === 'datetime')

const hasValue = computed(() => {
  if (props.range) {
    return !!(rangeStart.value && rangeEnd.value)
  } else {
    return !!singleDate.value
  }
})

const placeholderText = computed(() => {
  if (showError.value && props.required) {
    return props.requiredMessage || `请选择${props.title || '日期'}`
  }
  return props.placeholder || `请选择${props.title || '日期'}`
})

const displayValue = computed(() => {
  // 如果显示错误，不显示任何值
  if (showError.value && props.required) {
    return ''
  }

  const fmt = (d: string | null, t: any) => {
    if (!d || !dayjs(d).isValid()) return ''
    let str = ''
    if (showDate.value) str += dayjs(d).format('YYYY-MM-DD')
    if (showTime.value) {
      const timeStr = `${String(t.h).padStart(2, '0')}:${String(t.m).padStart(2, '0')}:${String(t.s).padStart(2, '0')}`
      str += showDate.value ? ` ${timeStr}` : timeStr
    }
    return str
  }

  if (props.range) {
    if (rangeStart.value && rangeEnd.value) {
      return `${fmt(rangeStart.value, timeStart.value)} ~ ${fmt(rangeEnd.value, timeEnd.value)}`
    }
    if (rangeStart.value) {
      return fmt(rangeStart.value, timeStart.value)
    }
    return ''
  } else {
    if (singleDate.value) {
      return fmt(singleDate.value, singleTime.value)
    }
    return ''
  }
})

// 标签文字
const startLabel = computed(() => `开始${labelForType()}`)
const endLabel = computed(() => `结束${labelForType()}`)
const singleLabel = computed(() => labelForType())

function labelForType() {
  if (props.type === 'date') return '日期'
  if (props.type === 'time') return '时间'
  return '日期时间'
}

function togglePopover() {
  if (props.disabled) return
  open.value = !open.value
}

// 失焦事件处理
function onBlur() {
  // 延迟执行，避免与弹窗点击冲突
  setTimeout(() => {
    if (!open.value) {
      emit('blur')
      validateRequired()
    }
  }, 150)
}

// 必填验证
function validateRequired() {
  if (props.required) {
    const valid = props.range
        ? !!(rangeStart.value && rangeEnd.value)
        : !!singleDate.value
    showError.value = !valid
    return valid
  }
  showError.value = false
  return true
}

// 弹窗关闭时同步数据到外层，保证点击空白关闭弹窗时同步
watch(open, (val, oldVal) => {
  if (oldVal === true && val === false) {
    // 弹窗从开变关，主动同步当前值给外层
    const currentValue = displayValue.value
    emit('update:modelValue', currentValue)

    // 弹窗关闭后进行验证
    setTimeout(() => {
      if (props.required) {
        validateRequired()
      }
    }, 100)
  }
})

// 修复清空函数
function clearAll() {
  if (props.range) {
    rangeStart.value = rangeEnd.value = null
    timeStart.value = { h: 0, m: 0, s: 0 }
    timeEnd.value = { h: 23, m: 59, s: 59 }
  } else {
    singleDate.value = null
    singleTime.value = { h: 0, m: 0, s: 0 }
  }

  emit('update:modelValue', '')

  // 清空后如果是必填字段，需要显示错误提示
  if (props.required) {
    // 使用 nextTick 确保 DOM 更新后再验证
    nextTick(() => {
      showError.value = true
    })
  } else {
    showError.value = false
  }
}

function applyQuick(opt: any) {
  opt.apply()
  quickOptions.value.forEach(o => (o.isActive = o === opt))
  // 选择后清除错误状态
  if (hasValue.value) {
    showError.value = false
  }
}

function updateRange(which: 'start' | 'end', val: string) {
  if (which === 'start') rangeStart.value = val
  else rangeEnd.value = val
  quickOptions.value.forEach(o => (o.isActive = false))
  // 选择后清除错误状态
  if (hasValue.value) {
    showError.value = false
  }
}

function cancel() {
  open.value = false
  syncFromModel()
  quickOptions.value.forEach(o => (o.isActive = false))
}

function validate() {
  return validateRequired()
}

function confirm() {
  if (!validate()) return

  if (props.range) {
    if (rangeEnd.value && dayjs(rangeEnd.value).isBefore(dayjs(rangeStart.value))) {
      const tmpDate = rangeStart.value
      const tmpTime = timeStart.value
      rangeStart.value = rangeEnd.value
      rangeEnd.value = tmpDate
      timeStart.value = timeEnd.value
      timeEnd.value = tmpTime
    }
  }

  emit('update:modelValue', displayValue.value)
  open.value = false
  showError.value = false
}

// 双击子组件触发，父组件关闭弹窗
function handleConfirm() {
  if (!props.range && (props.type === 'date' || props.type === 'datetime')) {
    confirm()
  }
}

// 解析传入 modelValue 字符串，同步到内部状态
function syncFromModel() {
  const val = props.modelValue || ''
  if (!val) {
    if (props.range) {
      rangeStart.value = rangeEnd.value = null
      timeStart.value = { h: 0, m: 0, s: 0 }
      timeEnd.value = { h: 23, m: 59, s: 59 }
    } else {
      singleDate.value = null
      singleTime.value = { h: 0, m: 0, s: 0 }
    }
    // 不要在这里清除错误状态，让验证函数来处理
    return
  }

  if (props.range) {
    const parts = val.split('~').map(p => p.trim())
    if (parts.length === 2) {
      const ds = dayjs(parts[0], getFormat())
      const de = dayjs(parts[1], getFormat())
      if (ds.isValid()) {
        rangeStart.value = ds.format('YYYY-MM-DD')
        timeStart.value = { h: ds.hour(), m: ds.minute(), s: ds.second() }
      } else {
        rangeStart.value = null
      }
      if (de.isValid()) {
        rangeEnd.value = de.format('YYYY-MM-DD')
        timeEnd.value = { h: de.hour(), m: de.minute(), s: de.second() }
      } else {
        rangeEnd.value = null
      }
    } else {
      if (props.range) {
        rangeStart.value = rangeEnd.value = null
        timeStart.value = { h: 0, m: 0, s: 0 }
        timeEnd.value = { h: 23, m: 59, s: 59 }
      } else {
        singleDate.value = null
        singleTime.value = { h: 0, m: 0, s: 0 }
      }
    }
  } else {
    const d = dayjs(val, getFormat())
    if (d.isValid()) {
      singleDate.value = d.format('YYYY-MM-DD')
      singleTime.value = { h: d.hour(), m: d.minute(), s: d.second() }
    } else {
      singleDate.value = null
      singleTime.value = { h: 0, m: 0, s: 0 }
    }
  }

  // 同步后如果有值则清除错误状态
  if (hasValue.value && showError.value) {
    showError.value = false
  }
}

function getFormat() {
  if (props.type === 'date') return 'YYYY-MM-DD'
  if (props.type === 'time') return 'HH:mm:ss'
  return 'YYYY-MM-DD HH:mm:ss'
}

// 监听单个日期选择，清除错误状态
watch(singleDate, (newVal) => {
  if (newVal && showError.value) {
    showError.value = false
  }
})

// 监听范围日期选择，清除错误状态
watch([rangeStart, rangeEnd], ([newStart, newEnd]) => {
  if (props.range && newStart && newEnd && showError.value) {
    showError.value = false
  }
})

// 快捷选项（只在区间模式下显示）
const quickOptions = ref([
  {
    label: '今天',
    apply() {
      const now = dayjs()
      if (props.range) {
        rangeStart.value = now.format('YYYY-MM-DD')
        rangeEnd.value = now.format('YYYY-MM-DD')
        timeStart.value = { h: 0, m: 0, s: 0 }
        timeEnd.value = { h: 23, m: 59, s: 59 }
      } else {
        singleDate.value = now.format('YYYY-MM-DD')
        singleTime.value = { h: 0, m: 0, s: 0 }
      }
    },
    isActive: false,
  },
  {
    label: '昨天',
    apply() {
      const yesterday = dayjs().subtract(1, 'day')
      if (props.range) {
        rangeStart.value = yesterday.format('YYYY-MM-DD')
        rangeEnd.value = yesterday.format('YYYY-MM-DD')
        timeStart.value = { h: 0, m: 0, s: 0 }
        timeEnd.value = { h: 23, m: 59, s: 59 }
      } else {
        singleDate.value = yesterday.format('YYYY-MM-DD')
        singleTime.value = { h: 0, m: 0, s: 0 }
      }
    },
    isActive: false,
  },
  {
    label: '本周',
    apply() {
      const start = dayjs().startOf('week')
      const end = dayjs().endOf('week')
      if (props.range) {
        rangeStart.value = start.format('YYYY-MM-DD')
        rangeEnd.value = end.format('YYYY-MM-DD')
        timeStart.value = { h: 0, m: 0, s: 0 }
        timeEnd.value = { h: 23, m: 59, s: 59 }
      } else {
        singleDate.value = start.format('YYYY-MM-DD')
        singleTime.value = { h: 0, m: 0, s: 0 }
      }
    },
    isActive: false,
  },
  {
    label: '本月',
    apply() {
      const start = dayjs().startOf('month')
      const end = dayjs().endOf('month')
      if (props.range) {
        rangeStart.value = start.format('YYYY-MM-DD')
        rangeEnd.value = end.format('YYYY-MM-DD')
        timeStart.value = { h: 0, m: 0, s: 0 }
        timeEnd.value = { h: 23, m: 59, s: 59 }
      } else {
        singleDate.value = start.format('YYYY-MM-DD')
        singleTime.value = { h: 0, m: 0, s: 0 }
      }
    },
    isActive: false,
  },
])

const showQuickOptions = computed(() => props.range)

function onClickOutside(e: MouseEvent) {
  if (!container.value?.contains(e.target as Node)) {
    open.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  syncFromModel()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
})

// 监听 modelValue 变化
watch(() => props.modelValue, () => {
  syncFromModel()
})

defineExpose({
  validate,
})
</script>

<style scoped>
.placeholder-red::placeholder {
  color: var(--color-error);
}
</style>
