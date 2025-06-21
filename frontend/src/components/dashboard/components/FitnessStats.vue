<template>
  <div class="bg-white border rounded-md p-4 shadow animate-fade max-w-3xl mx-auto space-y-6">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <!-- 查询看板 -->
    <div class="relative w-full border rounded-xl p-4 space-y-4 transition">
      <div class="flex flex-wrap items-center gap-3">
        <!-- 健身类型多选 -->
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
        <div class="flex gap-2 flex-shrink-0 ml-auto">
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

      <!-- 更多查询项 -->
      <div v-if="showMore" class="flex flex-wrap gap-3">
        <BaseInput v-model="query.remark" placeholder="备注关键词" type="text" clearable />
      </div>
    </div>

    <!-- ECharts 图表区域 -->
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
import { ref, computed, watch, onMounted, nextTick, defineProps } from 'vue'
import * as echarts from 'echarts'
import { useFitnessStore } from '@/store/fitnessStore'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch
} from 'lucide-vue-next'
import emitter from '@/utils/eventBus'
import { joinRangeDates, splitRangeDates } from '@/utils/formatters'

const props = defineProps({
  fitnessTypeOptions: {
    type: Array as () => { label: string; value: number }[],
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

const showMore = ref(false)
const toggleMore = () => {
  showMore.value = !showMore.value
}

const rangeValue = ref('')

watch(
    () => [query.value.startDate, query.value.endDate],
    ([start, end]) => {
      rangeValue.value = joinRangeDates(start, end)
    },
    { immediate: true }
)

watch(rangeValue, val => {
  const { start, end } = splitRangeDates(val)
  query.value.startDate = start
  query.value.endDate = end
})

const fitnessError = ref('')
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

function getEmptyDescription(type: string) {
  return `暂无相关${type}统计数据`
}

function formatAmount(value: any) {
  if (value == null) return '-'
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

const echartOptions = computed(() => {
  if (!fitnessStore.list.length || !query.value.typeIdList.length) return null

  const dates = [...new Set(fitnessStore.list.map(i => i.finishTime.split('T')[0]))].sort()

  const formattedDates = dates.map(d => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  const series = query.value.typeIdList
      .map((typeId, index) => {
        const typeMeta = fitnessStore.metaTypes.find(t => t.id === typeId)
        if (!typeMeta) return null

        const data = dates.map(date => {
          const record = fitnessStore.list.find(
              r => r.typeId === typeId && r.finishTime.startsWith(date)
          )
          return record ? Number(record.count) : 0
        })

        return {
          name: typeMeta.value1 || '未知类型',
          type: 'line',
          data,
          smooth: true,
          lineStyle: { color: `hsl(${index * 60}, 70%, 50%)` },
          itemStyle: { color: `hsl(${index * 60}, 70%, 40%)` },
          areaStyle: { color: `hsla(${index * 60}, 70%, 50%, 0.3)` }
        }
      })
      .filter(Boolean)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: params =>
          params.map(p => `${p.marker} ${p.seriesName}: ${formatAmount(p.data)}`).join('<br/>')
    },
    legend: {
      data: series.map(s => s.name),
      bottom: 0,
      textStyle: { color: '#555' }
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
  start.setDate(end.getDate() - 29) // 最近30天包含今天
  const format = (d: Date) => d.toISOString().slice(0, 10)
  return {
    startDate: format(start),
    endDate: format(end)
  }
}

async function fetchData() {
  fitnessError.value = ''

  if (!query.value.startDate || !query.value.endDate) {
    emitter.emit('notify', { message: '请选择开始日期和结束日期', type: 'error' })
    return
  }

  fitnessStore.updateQuery({
    typeIdList: query.value.typeIdList,
    startDate: query.value.startDate,
    endDate: query.value.endDate,
    remark: query.value.remark
  })

  try {
    await fitnessStore.loadList()
  } catch {
    fitnessError.value = '获取健身数据失败，请稍后重试'
  }
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

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (!echartOptions.value) return

  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(echartOptions.value)
}

watch(echartOptions, () => {
  nextTick(renderChart)
})

onMounted(() => {
  query.value.typeIdList = props.fitnessTypeOptions.map(i => i.value)

  const { startDate, endDate } = getDefaultDateRange()
  query.value.startDate = startDate
  query.value.endDate = endDate

  fetchData().then(() => nextTick(renderChart))
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
