<template>
  <div class="border rounded-xl shadow p-6 animate-fade w-full space-y-6 bg-white">
    <h2 class="text-lg font-semibold mb-4">资产统计</h2>

    <!-- 查询条件 -->
    <div class="border rounded-xl p-4 space-y-4 hover:shadow-md">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex-1 w-full">
          <BaseSelect
              title="资产名称"
              v-model="selectedNameIds"
              :options="props.assetNameOptions"
              multiple
              clearable
              placeholder="请选择资产名称"
              class="w-full"
          />
        </div>
        <div class="w-[220px] flex-shrink-0">
          <BaseSelect
              title="资产类型"
              v-model="selectedTypeIds"
              :options="props.assetTypeOptions"
              multiple
              clearable
              placeholder="请选择资产类型"
              class="w-full"
              @change="handleTypeChange"
          />
        </div>
        <div class="w-[220px] flex-shrink-0">
          <BaseSelect
              title="资产位置"
              v-model="selectedLocationIds"
              :options="props.assetLocationOptions"
              multiple
              clearable
              placeholder="请选择资产位置"
              class="w-full"
          />
        </div>

        <div class="flex gap-2 ml-auto flex-shrink-0">
          <BaseButton
              :disabled="!isRangeValid"
              @click="onSearch"
              color="outline"
              :icon="LucideSearch"
              variant="search"
          />
          <BaseButton
              @click="onReset"
              color="outline"
              :icon="LucideRotateCcw"
              variant="search"
          />
          <BaseButton
              @click="toggleMore"
              color="outline"
              :icon="showMore ? LucideChevronUp : LucideChevronDown"
              variant="search"
          />
        </div>
      </div>

      <div v-if="showMore" class="flex flex-wrap items-center gap-3 mt-3">
        <div class="w-[300px] flex-shrink-0">
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
        <div class="flex-1 min-w-[240px]">
          <BaseInput
              v-model="remark"
              placeholder="备注关键词"
              type="text"
              clearable
              class="w-full"
          />
        </div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="relative min-h-[400px] h-[calc(100vh-300px)]">
      <div v-if="dates.length === 0" class="flex items-center justify-center h-full text-gray-400">
        暂无数据
      </div>
      <div v-else ref="chartRef" class="w-full h-full"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch,
} from 'lucide-vue-next'
import { useAssetStore } from '@/store/assetStore'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  assetNameOptions: Option[]
  assetTypeOptions: Option[]
  assetLocationOptions: Option[]
}>()

const assetStore = useAssetStore()

const selectedTypeIds = ref<(string | number)[]>([])
const selectedNameIds = ref<(string | number)[]>([])
const selectedLocationIds = ref<(string | number)[]>([])
const remark = ref('')
const rangeValue = ref('')
const showMore = ref(false)
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

function formatDate(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

function getLastMonthRange(): string {
  const end = new Date()
  const start = new Date()
  start.setMonth(end.getMonth() - 1)
  return `${formatDate(start)} ~ ${formatDate(end)}`
}

rangeValue.value = getLastMonthRange()

const isRangeValid = computed(() => {
  if (!rangeValue.value) return false
  const parts = rangeValue.value.split('~').map(s => s.trim())
  return parts.length === 2 && parts[0] !== '' && parts[1] !== ''
})

// 统一取资产记录，避免多次调用
const records = computed(() => assetStore.allList || [])

/**
 * 按维度（资产名称/类型/位置）和日期汇总金额
 * @param dimensionKey 维度字段名，例如 'assetName' / 'assetType' / 'assetLocation'
 * @returns Record<维度值, Record<日期, 金额>>
 */
function summarizeByDimension(dimensionKey: keyof typeof records.value[0]) {
  const map: Record<string, Record<string, number>> = {}
  records.value.forEach((item) => {
    const date = item.recordDate?.split('T')[0]
    if (!date) return
    const dimVal = item[dimensionKey] || `未知${dimensionKey}`
    if (!map[dimVal]) map[dimVal] = {}
    map[dimVal][date] = (map[dimVal][date] || 0) + (item.amount || 0)
  })
  return map
}

// 总金额按日期汇总
const totalAmountByDate = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {}
  records.value.forEach(item => {
    const date = item.recordDate?.split('T')[0]
    if (!date) return
    map[date] = (map[date] || 0) + (item.amount || 0)
  })
  return map
})

const amountByNameDate = computed(() => summarizeByDimension('assetName'))
const amountByTypeDate = computed(() => summarizeByDimension('assetType'))
const amountByLocationDate = computed(() => summarizeByDimension('assetLocation'))

const dates = computed(() => {
  const dateSet = new Set<string>()
  // 收集所有日期
  Object.keys(totalAmountByDate.value).forEach(d => dateSet.add(d))
  ;[amountByNameDate.value, amountByTypeDate.value, amountByLocationDate.value].forEach(dimMap => {
    Object.values(dimMap).forEach(dateMap => {
      Object.keys(dateMap).forEach(d => dateSet.add(d))
    })
  })
  return Array.from(dateSet).sort()
})

// 填充数据，缺失日期补0
function fillSeriesData(
    dates: string[],
    dataMap: Record<string, Record<string, number>>,
    keys: string[]
) {
  return keys.map(key => ({
    name: key,
    data: dates.map(d => dataMap[key]?.[d] ?? 0)
  }))
}

function fillSimpleData(dates: string[], dataMap: Record<string, number>) {
  return dates.map(d => dataMap[d] ?? 0)
}

function initChart() {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const totalData = fillSimpleData(dates.value, totalAmountByDate.value)
  const nameKeys = Object.keys(amountByNameDate.value)
  const typeKeys = Object.keys(amountByTypeDate.value)
  const locKeys = Object.keys(amountByLocationDate.value)

  const nameSeries = fillSeriesData(dates.value, amountByNameDate.value, nameKeys)
  const typeSeries = fillSeriesData(dates.value, amountByTypeDate.value, typeKeys)
  const locSeries = fillSeriesData(dates.value, amountByLocationDate.value, locKeys)

  const series = [
    {
      name: '总金额',
      type: 'line',
      smooth: true,
      data: totalData,
      lineStyle: { color: '#3b82f6' },
      itemStyle: { color: '#3b82f6' }
    },
    ...nameSeries.map(item => ({
      name: `资产名称: ${item.name}`,
      type: 'line',
      smooth: true,
      data: item.data,
      lineStyle: { width: 1 }
    })),
    ...typeSeries.map(item => ({
      name: `资产类型: ${item.name}`,
      type: 'line',
      smooth: true,
      data: item.data,
      lineStyle: { width: 1 }
    })),
    ...locSeries.map(item => ({
      name: `资产位置: ${item.name}`,
      type: 'line',
      smooth: true,
      data: item.data,
      lineStyle: { width: 1 }
    }))
  ]

  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { type: 'scroll', orient: 'horizontal', bottom: 0 },
    xAxis: {
      type: 'category',
      data: dates.value,
      boundaryGap: false,
      axisLabel: { rotate: 45 }
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: val => `￥${val.toFixed(2)}` },
      minInterval: 1,
      splitLine: { lineStyle: { type: 'dashed', color: '#eee' } }
    },
    grid: { left: 50, right: 30, bottom: 80, top: 40 },
    series
  })
}

function loadData() {
  if (!isRangeValid.value) return
  const [start, end] = rangeValue.value.split('~').map(s => s.trim())
  assetStore.updateQuery({
    assetTypeIdList: selectedTypeIds.value,
    assetNameIdList: selectedNameIds.value,
    assetLocationIdList: selectedLocationIds.value,
    startDate: start,
    endDate: end,
    remark: remark.value.trim(),
  })
  assetStore.loadAllRecords()
}

function onSearch() {
  loadData()
}

function onReset() {
  selectedTypeIds.value = []
  selectedNameIds.value = []
  selectedLocationIds.value = []
  remark.value = ''
  rangeValue.value = getLastMonthRange()
  assetStore.allList = []
  loadData()
}

function toggleMore() {
  showMore.value = !showMore.value
}

function handleTypeChange() {
  selectedNameIds.value = []
}

watch(
    () => assetStore.allList,
    () => {
      nextTick(() => {
        initChart()
      })
    },
    { deep: true }
)

onMounted(() => {
  loadData()
  initChart()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})

function onResize() {
  chartInstance?.resize()
}
</script>

<style scoped>
.animate-fade {
  animation: fadeIn 0.3s ease forwards;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
