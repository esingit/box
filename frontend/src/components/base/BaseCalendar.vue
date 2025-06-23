<template>
  <div class="select-none text-gray-900 bg-white rounded-xl p-2 border border-gray-300">
    <!-- 年月选择 -->
    <div class="flex items-center justify-between mb-2 px-2">
      <button @click="prevMonth" type="button" class="p-1 hover:bg-gray-200 rounded">‹</button>

      <div class="flex space-x-2 items-center cursor-pointer select-none">
        <div @click="showYearList = !showYearList" class="font-semibold text-gray-800">
          {{ currentYear }}年
        </div>
        <div @click="showMonthList = !showMonthList" class="font-semibold text-gray-800">
          {{ currentMonth + 1 }}月
        </div>
      </div>

      <button @click="nextMonth" type="button" class="p-1 hover:bg-gray-200 rounded">›</button>
    </div>

    <!-- 年份列表弹窗 -->
    <div v-if="showYearList" class="absolute bg-white border rounded shadow max-h-60 overflow-auto z-50 p-2 w-24">
      <button
          v-for="year in yearRange"
          :key="year"
          type="button"
          @click="selectYear(year)"
          :class="['w-full text-left py-1 px-2 rounded hover:bg-gray-100', year === currentYear ? 'bg-gray-200 font-bold' : '']"
      >
        {{ year }}年
      </button>
    </div>

    <!-- 月份列表弹窗 -->
    <div v-if="showMonthList" class="absolute bg-white border rounded shadow max-h-48 overflow-auto z-50 p-2 w-24">
      <button
          v-for="m in 12"
          :key="m"
          type="button"
          @click="selectMonth(m - 1)"
          :class="['w-full text-left py-1 px-2 rounded hover:bg-gray-100', m - 1 === currentMonth ? 'bg-gray-200 font-bold' : '']"
      >
        {{ m }}月
      </button>
    </div>

    <!-- 星期标题 -->
    <div class="grid grid-cols-7 text-center text-xs text-gray-500 mb-2">
      <div v-for="d in weekDays" :key="d">{{ d }}</div>
    </div>

    <!-- 日期格子 -->
    <div class="grid grid-cols-7 gap-1">
      <button
          v-for="day in days"
          :key="day.date"
          type="button"
          @click="selectDate(day.date)"
          @dblclick="dblClickDate(day.date)"
          :disabled="day.disabled"
          :class="[
          'w-8 h-8 flex items-center justify-center rounded-xl',
          day.isToday ? 'bg-gray-400 text-white' : '',
          day.isSelected ? 'bg-gray-200 text-gray-900 font-semibold' : 'hover:bg-gray-100 text-gray-900',
          day.disabled ? 'text-gray-400 cursor-not-allowed' : ''
        ]"
      >
        {{ day.day }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import dayjs from 'dayjs'

const props = defineProps<{
  modelValue: string | null
  minDate?: string | null
  maxDate?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'confirm'): void
}>()

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

// 当前显示年月
const currentYear = ref(dayjs(props.modelValue || undefined).year())
const currentMonth = ref(dayjs(props.modelValue || undefined).month())

const days = ref<any[]>([])

const showYearList = ref(false)
const showMonthList = ref(false)

// 生成年份范围，例如当前年 +-10年
const yearRange = computed(() => {
  const current = dayjs().year()
  const start = current - 10
  const end = current + 10
  const arr = []
  for (let y = start; y <= end; y++) arr.push(y)
  return arr
})

function buildCalendar() {
  let base = dayjs().year(currentYear.value).month(currentMonth.value).startOf('month')
  if (!base.isValid()) base = dayjs().startOf('month')
  const start = base.startOf('week')
  const end = base.endOf('month').endOf('week')
  const tmp = []
  let day = start

  while (day.isBefore(end) || day.isSame(end)) {
    const isOutOfMin = props.minDate && day.isBefore(dayjs(props.minDate), 'day')
    const isOutOfMax = props.maxDate && day.isAfter(dayjs(props.maxDate), 'day')

    tmp.push({
      date: day.format('YYYY-MM-DD'),
      day: day.date(),
      isToday: day.isSame(dayjs(), 'day'),
      isSelected: day.isSame(dayjs(props.modelValue), 'day'),
      disabled: day.month() !== base.month() || isOutOfMin || isOutOfMax
    })
    day = day.add(1, 'day')
  }
  days.value = tmp
}

function selectDate(date: string) {
  if (days.value.find(d => d.date === date)?.disabled) return
  emit('update:modelValue', date)
}

function dblClickDate(date: string) {
  if (days.value.find(d => d.date === date)?.disabled) return
  emit('update:modelValue', date)
  emit('confirm') // 双击时确认
}

function selectYear(year: number) {
  currentYear.value = year
  showYearList.value = false
  buildCalendar()
}

function selectMonth(month: number) {
  currentMonth.value = month
  showMonthList.value = false
  buildCalendar()
}

function prevMonth() {
  const d = dayjs().year(currentYear.value).month(currentMonth.value).subtract(1, 'month')
  currentYear.value = d.year()
  currentMonth.value = d.month()
  buildCalendar()
}

function nextMonth() {
  const d = dayjs().year(currentYear.value).month(currentMonth.value).add(1, 'month')
  currentYear.value = d.year()
  currentMonth.value = d.month()
  buildCalendar()
}

watch(() => [props.modelValue, props.minDate, props.maxDate], () => {
  if (props.modelValue) {
    const d = dayjs(props.modelValue)
    if (d.isValid()) {
      currentYear.value = d.year()
      currentMonth.value = d.month()
    }
  }
  buildCalendar()
}, { immediate: true })

</script>
