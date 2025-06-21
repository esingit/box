<template>
  <div class="bg-white border rounded-md p-4 shadow animate-fade max-w-3xl mx-auto space-y-6">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <!-- 查询区域 -->
    <div class="border rounded-xl p-4 space-y-4">
      <div class="flex flex-wrap items-center gap-3">
        <!-- 多选健身类型 -->
        <div class="flex-1 min-w-[200px]">
          <BaseSelect
              v-model="query.typeIdList"
              :options="fitnessTypeOptions"
              multiple
              clearable
              placeholder="全部健身类型"
              class="w-full"
          />
        </div>

        <!-- 日期范围 -->
        <div class="flex gap-2 items-center flex-shrink-0">
          <BaseDateInput
              v-model="rangeValue"
              type="date"
              range
              clearable
              placeholder="请选择日期范围"
              class="w-[300px]"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="flex gap-2 ml-auto flex-shrink-0">
          <BaseButton @click="onSearch" color="outline" :icon="LucideSearch" variant="search" />
          <BaseButton @click="onReset" color="outline" :icon="LucideRotateCcw" variant="search" />
          <BaseButton
              @click="toggleMore"
              color="outline"
              :icon="showMore ? LucideChevronUp : LucideChevronDown"
              variant="search"
          />
        </div>
      </div>

      <!-- 更多条件 -->
      <div v-if="showMore" class="flex flex-wrap gap-3">
        <BaseInput v-model="query.remark" placeholder="备注关键词" type="text" clearable />
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="relative h-72">
      <template v-if="fitnessError">
        <div class="flex flex-col items-center justify-center h-full text-red-600">
          <p class="mb-2 font-semibold">{{ fitnessError }}</p>
          <p class="text-sm text-gray-500">{{ getEmptyDescription('健身') }}</p>
        </div>
      </template>

      <template v-else-if="echartOptions && echartOptions.series.length">
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
import { useFitnessStore } from '@/store/fitnessStore'
import emitter from '@/utils/eventBus'
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

// Props 类型定义
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
const query = ref({
  typeIdList: [] as number[],
  startDate: '',
  endDate: '',
  remark: ''
})

const rangeValue = ref('')
const showMore = ref(false)
const fitnessError = ref('')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const toggleMore = () => (showMore.value = !showMore.value)

watch(rangeValue, val => {
  const { start, end } = splitRangeDates(val)
  query.value.startDate = start
  query.value.endDate = end
})

function getEmptyDescription(type: string) {
  return `暂无相关${type}统计数据`
}

function formatAmount(value: any) {
  return value == null ? '-' : Number(value).toFixed(2)
}

const echartOptions = computed(() => {
  const list = fitnessStore.allList
  const selected = query.value.typeIdList
  if (!list?.length || !selected.length) return null

  const dates = [...new Set(list.map(i => i.finishTime.split('T')[0]))].sort()
  const formattedDates = dates.map(d => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  const series = selected.map((typeId, index) => {
    const meta = props.fitnessTypeOptions.find(i => i.value === typeId)
    if (!meta) return null

    const data = dates.map(date =>
        list
            .filter(r => r.typeId === typeId && r.finishTime.startsWith(date))
            .reduce((sum, r) => sum + Number(r.count || 0), 0)
    )

    return {
      name: meta.value1 || `类型${typeId}`,
      type: 'line',
      data,
      smooth: true,
      lineStyle: { color: `hsl(${index * 60}, 70%, 50%)` },
      itemStyle: { color: `hsl(${index * 60}, 70%, 40%)` },
      areaStyle: { color: `hsla(${index * 60}, 70%, 50%, 0.3)` }
    }
  }).filter(Boolean)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) =>
          params.map((p: any) => `${p.marker} ${p.seriesName}: ${formatAmount(p.data)}`).join('<br/>')
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 0
    },
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
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.1)' } },
      minInterval: 1
    },
    series
  }
})

function getDefaultDateRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10)
  }
}

async function fetchData() {
  fitnessError.value = ''
  if (!query.value.startDate || !query.value.endDate) {
    emitter.emit('notify', { message: '请选择日期范围', type: 'error' })
    return
  }

  fitnessStore.updateQuery(query.value)

  try {
    await fitnessStore.loadAllRecords()

    if (!query.value.typeIdList.length) {
      query.value.typeIdList = props.fitnessTypeOptions.map(i => i.value)
    }

    await nextTick()
    renderChart()
  } catch {
    fitnessError.value = '获取健身数据失败，请稍后重试'
  }
}

function renderChart() {
  if (!chartRef.value || !echartOptions.value) return

  chartInstance?.dispose()
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(echartOptions.value)
}

function onSearch() {
  fetchData()
}

function onReset() {
  const { startDate, endDate } = getDefaultDateRange()
  query.value = {
    typeIdList: props.fitnessTypeOptions.map(i => i.value),
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

  fetchData()
})
</script>

<style scoped>
.animate-fade {
  animation: fadeIn 0.3s ease forwards;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
