<template>
  <div class="fitness-stats p-4 bg-white rounded-md shadow space-y-4 max-w-4xl mx-auto">
    <div class="flex items-center gap-4">
      <BaseSelect
          v-model="query.typeIdList"
          :options="fitnessTypeOptions"
          multiple
          clearable
          placeholder="全部健身类型"
          class="flex-1 min-w-[180px]"
      />
      <BaseDateInput
          v-model="rangeValue"
          type="date"
          range
          clearable
          placeholder="选择日期范围"
          class="w-[320px]"
      />
      <BaseButton @click="onSearch" color="primary" icon="LucideSearch">查询</BaseButton>
      <BaseButton @click="onReset" color="outline" icon="LucideRotateCcw">重置</BaseButton>
    </div>

    <div v-if="loadingList" class="text-center text-gray-500">加载中...</div>
    <div v-else-if="list.length === 0" class="text-center text-gray-400">暂无数据</div>
    <div v-else ref="chartRef" class="w-full h-64"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useFitnessStore } from '@/store/fitnessStore'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import { LucideSearch, LucideRotateCcw } from 'lucide-vue-next'
import { joinRangeDates, splitRangeDates } from '@/utils/formatters'

defineProps({
  fitnessTypeOptions: {
    type: Array,
    required: true
  }
})

const fitnessStore = useFitnessStore()
const query = fitnessStore.query

const rangeValue = ref('')

// 绑定组件多选和日期范围
watch(
    () => [query.startDate, query.endDate],
    ([start, end]) => {
      rangeValue.value = joinRangeDates(start, end)
    },
    { immediate: true }
)
watch(rangeValue, val => {
  const { start, end } = splitRangeDates(val)
  query.startDate = start
  query.endDate = end
})

const list = computed(() => fitnessStore.list)
const loadingList = computed(() => fitnessStore.loadingList)

const chartRef = ref(null)
let chartInstance: echarts.ECharts | null = null

function renderChart() {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  if (list.value.length === 0) return

  chartInstance = echarts.init(chartRef.value)

  const dates = Array.from(new Set(list.value.map(i => i.finishTime.slice(0, 10)))).sort()
  const formattedDates = dates.map(d => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  const series = query.typeIdList.map((typeId, idx) => {
    const data = dates.map(date => {
      const rec = list.value.find(r => r.typeId === typeId && r.finishTime.startsWith(date))
      return rec ? rec.count : 0
    })
    return {
      name: `类型 ${typeId}`,
      type: 'line',
      data,
      smooth: true,
      lineStyle: { color: `hsl(${idx * 60}, 70%, 50%)` },
      itemStyle: { color: `hsl(${idx * 60}, 70%, 40%)` },
      areaStyle: { color: `hsla(${idx * 60}, 70%, 50%, 0.3)` }
    }
  })

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name) },
    xAxis: {
      type: 'category',
      data: formattedDates
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series
  })
}

async function onSearch() {
  if (!query.startDate || !query.endDate) {
    alert('请选择日期范围')
    return
  }
  await fitnessStore.loadList()
  nextTick(renderChart)
}

function onReset() {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 29)
  query.startDate = start.toISOString().slice(0, 10)
  query.endDate = end.toISOString().slice(0, 10)
  query.typeIdList = props.fitnessTypeOptions.map(i => i.value)
  fitnessStore.resetQuery()
  onSearch()
}

onMounted(() => {
  // 默认初始化
  if (!query.startDate || !query.endDate) {
    onReset()
  } else {
    onSearch()
  }
})
</script>
