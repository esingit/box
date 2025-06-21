<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-6">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <!-- 查询条件 -->
    <div class="border rounded-xl p-4 space-y-4 hover:shadow-md">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex-1 min-w-[200px]">
          <BaseSelect
              title="健身类型"
              v-model="query.typeIdList"
              :options="fitnessTypeOptions"
              multiple
              clearable
              placeholder="全部健身类型"
              class="w-full"
          />
        </div>

        <div class="w-[300px] flex-shrink-0 flex gap-2 items-center">
          <BaseDateInput
              v-model="rangeValue"
              type="date"
              range
              clearable
              required
              placeholder="请选择日期范围"
              class="w-full"
          />
        </div>

        <div class="flex gap-2 ml-auto flex-shrink-0">
          <BaseButton @click="fetchData" color="outline" :icon="LucideSearch" variant="search" />
          <BaseButton @click="resetForm" color="outline" :icon="LucideRotateCcw" variant="search" />
          <BaseButton
              @click="showMore = !showMore"
              color="outline"
              :icon="showMore ? LucideChevronUp : LucideChevronDown"
              variant="search"
          />
        </div>
      </div>

      <div v-if="showMore" class="flex flex-wrap gap-3">
        <BaseInput v-model="query.remark" placeholder="备注关键词" type="text" clearable />
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="relative min-h-[400px] h-[calc(100vh-300px)]">
      <template v-if="fitnessError">
        <div class="flex flex-col items-center justify-center h-full msg-error">
          <p class="mb-2 font-semibold">{{ fitnessError }}</p>
          <p class="text-sm text-gray-500">{{ getEmptyDescription('健身') }}</p>
        </div>
      </template>

      <template v-else-if="echartOptions">
        <div ref="chartRef" class="w-full h-full"></div>
      </template>

      <template v-else>
        <div class="flex flex-col items-center justify-center h-full text-gray-400">
          <p class="mb-2">暂无数据</p>
          <p class="text-sm">{{ getEmptyDescription('健身') }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick, type PropType } from 'vue'
import * as echarts from 'echarts'
import emitter from '@/utils/eventBus'
import { useFitnessStore } from '@/store/fitnessStore'
import { joinRangeDates, splitRangeDates } from '@/utils/formatters'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch
} from 'lucide-vue-next'

import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'

interface FitnessOption {
  label: string
  value: number
  value1?: string
}

const props = defineProps({
  fitnessTypeOptions: {
    type: Array as PropType<FitnessOption[]>,
    required: true
  }
})

const fitnessStore = useFitnessStore()
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const showMore = ref(false)
const fitnessError = ref('')
const rangeValue = ref('')

const query = ref({
  typeIdList: [] as number[],
  startDate: '',
  endDate: '',
  remark: ''
})

// 监听日期范围变化，同步拆分给 startDate 和 endDate
watch(rangeValue, (val) => {
  const { start, end } = splitRangeDates(val)
  query.value.startDate = start
  query.value.endDate = end
})

function getEmptyDescription(type: string) {
  return `暂无相关${type}统计数据`
}

// 格式化数字，保留两位小数，空值显示 '-'
const formatAmount = (val: any) => (val == null ? '-' : Number(val).toFixed(2))

// 计算当前图表配置
const echartOptions = computed(() => {
  const list = fitnessStore.allList
  if (!list?.length) return null

  // 如果没选类型，默认全选
  const selected = query.value.typeIdList.length > 0
      ? query.value.typeIdList
      : props.fitnessTypeOptions.map(i => i.value)

  if (!selected.length) return null

  // 获取日期，排序
  const dates = [...new Set(list.map(i => i.finishTime.split('T')[0]))].sort()
  if (!dates.length) return null

  // 格式化日期显示 MM/DD
  const formattedDates = dates.map(d => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  // 组装series
  const series = selected.map((typeId, index) => {
    const meta = props.fitnessTypeOptions.find(i => i.value === typeId)
    if (!meta) return null

    const data = dates.map(date =>
        list
            .filter(r => r.typeId === typeId && r.finishTime.startsWith(date))
            .reduce((sum, r) => sum + Number(r.count || 0), 0)
    )

    const hue = (index * 60) % 360
    const color = `hsl(${hue}, 30%, 50%)`

    return {
      name: meta.value1 || `类型${typeId}`,
      type: 'line',
      data,
      smooth: true,
      lineStyle: { color },
      itemStyle: { color },
      areaStyle: { color: `hsla(${hue}, 30%, 50%, 0.2)` }
    }
  }).filter(Boolean)

  if (!series.length) return null

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) =>
          params.map(p => `${p.marker} ${p.seriesName}: ${formatAmount(p.data)}`).join('<br/>')
    },
    legend: { data: series.map(s => s.name), bottom: 0 },
    grid: { left: 30, right: 30, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: formattedDates,
      axisLine: { lineStyle: { color: '#ccc' } },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '次数',
      axisLine: { lineStyle: { color: '#ccc' } },
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } },
      minInterval: 1
    },
    series
  }
})

// 默认日期范围：最近30天
function getDefaultDateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10)
  }
}

// 加载数据并渲染图表
async function fetchData() {
  fitnessError.value = ''

  if (!query.value.startDate || !query.value.endDate) {
    emitter.emit('notify', { message: '请选择日期范围', type: 'error' })
    return
  }

  // 传入查询参数
  const queryTypeIds = query.value.typeIdList.length > 0
      ? query.value.typeIdList
      : props.fitnessTypeOptions.map(i => i.value)

  fitnessStore.updateQuery({ ...query.value, typeIdList: queryTypeIds })

  try {
    await fitnessStore.loadAllRecords()
    await nextTick()
    renderChart()
  } catch {
    fitnessError.value = '获取健身数据失败，请稍后重试'
  }
}

// 渲染 ECharts
function renderChart() {
  if (!chartRef.value || !echartOptions.value) return

  chartInstance?.dispose()
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(echartOptions.value)
}

function resetForm() {
  const { startDate, endDate } = getDefaultDateRange()
  query.value = {
    typeIdList: [],
    startDate,
    endDate,
    remark: ''
  }
  rangeValue.value = joinRangeDates(startDate, endDate)
  fitnessStore.resetQuery()
  fetchData()
}

onMounted(() => {
  const { startDate, endDate } = getDefaultDateRange()
  query.value.startDate = startDate
  query.value.endDate = endDate
  rangeValue.value = joinRangeDates(startDate, endDate)
  query.value.typeIdList = []
  fetchData()
})
</script>

<style scoped>
.animate-fade {
  animation: fadeIn 0.3s ease forwards;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
