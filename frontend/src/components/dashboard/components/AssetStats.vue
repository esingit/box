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
              :disabled="!isDateRangeValid || isLoading"
              @click="handleSearch"
              color="outline"
              :icon="LucideSearch"
              variant="search"
          />
          <BaseButton
              :disabled="isLoading"
              @click="handleReset"
              color="outline"
              :icon="LucideRotateCcw"
              variant="search"
          />
          <BaseButton
              @click="showMore = !showMore"
              color="outline"
              :icon="showMore ? LucideChevronUp : LucideChevronDown"
              variant="search"
          />
        </div>
      </div>

      <div v-if="showMore" class="flex flex-wrap items-center gap-3 mt-3">
        <div class="w-[300px] flex-shrink-0">
          <BaseDateInput
              v-model="dateRange"
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

      <!-- 图表显示选项 -->
      <div v-if="hasData" class="flex flex-wrap items-center gap-4 pt-3 border-t">
        <span class="text-sm font-medium text-gray-600">显示维度:</span>
        <label v-for="option in chartOptionsConfig" :key="option.key" class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions[option.key]"
              class="rounded checkbox-input"
          />
          <span class="text-sm">{{ option.label }}</span>
        </label>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="hasData" class="grid grid-cols-1 md:grid-cols-5 gap-4 text-sm">
      <div v-for="stat in statisticsCards" :key="stat.title" :class="stat.cardClass">
        <div :class="stat.titleClass">{{ stat.title }}</div>
        <div :class="stat.valueClass">{{ stat.value }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="relative min-h-[500px] h-[calc(100vh-400px)]">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="flex items-center justify-center h-full text-gray-400">
        <div class="flex items-center gap-2">
          <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
          <span>加载资产数据中...</span>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="errorMessage" class="h-full">
        <BaseEmptyState
            icon="Wallet"
            :message="errorMessage"
            description="请检查网络连接或稍后重试"
        />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="!hasData" class="h-full">
        <BaseEmptyState
            icon="Wallet"
            message="暂无资产数据"
            :description="emptyStateDescription"
        />
      </div>

      <!-- 图表容器 -->
      <div v-else ref="chartRef" class="w-full h-full chart-container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, reactive, nextTick } from 'vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch,
} from 'lucide-vue-next'
import type { EChartsOption } from 'echarts'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import { useAssetStore } from '@/store/assetStore'
import { useDateRange, useChart } from '@/utils/common'
import emitter from '@/utils/eventBus'

// 类型定义
interface Option {
  label: string
  value: string | number
  id?: string
  name?: string
  value1?: string
  key1?: string
  key2?: string
  key3?: string
}

interface UnitOption {
  id: string | number
  label: string
  value: string | number
  value1?: string
  key1?: string
}

interface AssetRecord {
  id: string
  assetNameId: string
  assetTypeId: string
  amount: string
  unitId: string
  assetLocationId: string
  acquireTime: string
  assetName?: string | null
  assetTypeName?: string | null
  assetTypeValue?: string
  unitName?: string | null
  unitValue?: string
  assetLocationName?: string | null
  assetLocationValue?: string
  remark?: string
}

// Props
const props = defineProps<{
  assetNameOptions: Option[]
  assetTypeOptions: Option[]
  assetLocationOptions: Option[]
  unitOptions: UnitOption[]
}>()

// Composables
const assetStore = useAssetStore()
const { dateRange, isDateRangeValid, getDefaultRange, parseDateRange, dateRangeDisplay } = useDateRange()
const { chartRef, initChart, destroyChart, resizeChart } = useChart()

// 响应式状态
const selectedTypeIds = ref<(string | number)[]>([])
const selectedNameIds = ref<(string | number)[]>([])
const selectedLocationIds = ref<(string | number)[]>([])
const remark = ref('')
const showMore = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const isChartReady = ref(false)

// 图表选项配置
const chartOptionsConfig = [
  { key: 'showTotalTrend', label: '总金额趋势' },
  { key: 'showNameDimension', label: '按资产名称' },
  { key: 'showTypeDimension', label: '按资产类型' },
  { key: 'showLocationDimension', label: '按资产位置' }
] as const

const chartOptions = reactive({
  showTotalTrend: true,
  showNameDimension: true,
  showTypeDimension: true,
  showLocationDimension: true
})

// 低饱和度颜色方案
const CHART_COLORS = [
  '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E',
  '#7B9E9E', '#B8936B', '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8',
  '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B', '#9E7B8C', '#7B8C9E',
  '#A8937B', '#8C8C7B'
]

// 工具函数
function createMapping(options: any[], valueKey = 'value1', fallbackKey = 'label'): Record<string, string> {
  const map: Record<string, string> = {}
  if (!options || !Array.isArray(options)) return map

  options.forEach(option => {
    if (option) {
      const id = option.id || option.value
      const name = option[valueKey] || option.name || option[fallbackKey]
      if (id && name) {
        map[String(id)] = String(name)
      }
    }
  })
  return map
}

function normalizeUnitSymbol(unitSymbol: string): string {
  if (['￥', 'CNY', '人民币', 'RMB'].includes(unitSymbol)) {
    return '¥'
  }
  return unitSymbol
}

function formatAmountWithUnit(amount: number, unitSymbol = '¥'): string {
  if (amount === 0) return `${normalizeUnitSymbol(unitSymbol)}0.00`

  const normalizedSymbol = normalizeUnitSymbol(unitSymbol)
  let formattedAmount: string

  if (amount >= 10000) {
    formattedAmount = `${(amount / 10000).toFixed(1)}万`
  } else if (amount >= 1000) {
    formattedAmount = amount.toFixed(0)
  } else {
    formattedAmount = amount.toFixed(2)
  }

  return `${normalizedSymbol}${formattedAmount}`
}

function getDisplayName(id: string, mapping: Record<string, string>, fallback?: string | null, prefix = '未知'): string {
  return mapping[id] || fallback || `${prefix}${id}`
}

function getDefaultUnitForAssetType(typeId: string | number): string {
  const assetType = props.assetTypeOptions?.find(type =>
      String(type.value) === String(typeId) || String(type.id) === String(typeId)
  )

  if (!assetType?.key3) return '¥'

  const defaultUnit = props.unitOptions?.find(unit => unit.key1 === assetType.key3)
  return defaultUnit ? normalizeUnitSymbol(defaultUnit.value1 || '¥') : '¥'
}

function getUnitSymbol(record: AssetRecord): string {
  if (record.unitId && unitMapping.value[String(record.unitId)]) {
    return normalizeUnitSymbol(unitMapping.value[String(record.unitId)])
  }
  if (record.unitValue) {
    return normalizeUnitSymbol(record.unitValue)
  }
  return getDefaultUnitForAssetType(record.assetTypeId)
}

function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', { message, type })
}

// 创建映射
const nameMapping = computed(() => createMapping(props.assetNameOptions))
const typeMapping = computed(() => createMapping(props.assetTypeOptions))
const locationMapping = computed(() => createMapping(props.assetLocationOptions))
const unitMapping = computed(() => createMapping(props.unitOptions, 'value1'))

// 基础数据
const assetRecords = computed<AssetRecord[]>(() => {
  const list = assetStore.allList
  return (list && Array.isArray(list) ? list : []) as AssetRecord[]
})

const allDates = computed(() => {
  const dateSet = new Set<string>()
  assetRecords.value.forEach(record => {
    if (record?.acquireTime) {
      const date = record.acquireTime.split('T')[0]
      if (date) dateSet.add(date)
    }
  })
  return Array.from(dateSet).sort()
})

const lastDateWithRecords = computed(() => {
  return allDates.value.length ? allDates.value[allDates.value.length - 1] : ''
})

const lastDateRecords = computed(() => {
  if (!lastDateWithRecords.value) return []
  return assetRecords.value.filter(record =>
      record?.acquireTime?.startsWith(lastDateWithRecords.value)
  )
})

const hasData = computed(() => {
  return assetRecords.value.length > 0 && allDates.value.length > 0 && !errorMessage.value
})

const hasSearchConditions = computed(() => {
  return selectedTypeIds.value.length > 0 ||
      selectedNameIds.value.length > 0 ||
      selectedLocationIds.value.length > 0 ||
      remark.value.trim() !== ''
})

const emptyStateDescription = computed(() => {
  if (!isDateRangeValid.value) {
    return '请选择日期范围查看资产数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到资产记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无资产记录`
})

// 统计数据
const getTypeTotal = (typeKey: string) => {
  const typeIds = props.assetTypeOptions
      ?.filter(type => type.key1 === typeKey)
      ?.map(type => String(type.id || type.value)) || []

  return lastDateRecords.value
      .filter(record => typeIds.includes(String(record.assetTypeId)))
      .reduce((sum, record) => sum + (parseFloat(record.amount || '0') || 0), 0)
}

const totalAmount = computed(() => {
  return lastDateRecords.value.reduce((sum, record) =>
      sum + (parseFloat(record.amount || '0') || 0), 0)
})

const statisticsData = computed(() => {
  const unitSymbol = lastDateRecords.value.length > 0
      ? getUnitSymbol(lastDateRecords.value[0])
      : '¥'

  return {
    totalAmount: totalAmount.value,
    savingsTotal: getTypeTotal('SAVINGS'),
    financeTotal: getTypeTotal('FINANCE'),
    fundTotal: getTypeTotal('FUND'),
    debtTotal: getTypeTotal('DEBT'),
    unitSymbol
  }
})

const statisticsCards = computed(() => [
  {
    title: '总金额',
    value: formatAmountWithUnit(statisticsData.value.totalAmount, statisticsData.value.unitSymbol),
    cardClass: 'bg-red-50 p-3 rounded-lg',
    titleClass: 'text-red-600 font-medium',
    valueClass: 'text-lg font-bold text-red-800'
  },
  {
    title: '储蓄类型总额',
    value: formatAmountWithUnit(statisticsData.value.savingsTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-green-50 p-3 rounded-lg',
    titleClass: 'text-green-600 font-medium',
    valueClass: 'text-lg font-bold text-green-800'
  },
  {
    title: '理财类型总额',
    value: formatAmountWithUnit(statisticsData.value.financeTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-yellow-50 p-3 rounded-lg',
    titleClass: 'text-yellow-600 font-medium',
    valueClass: 'text-lg font-bold text-yellow-800'
  },
  {
    title: '基金类型总额',
    value: formatAmountWithUnit(statisticsData.value.fundTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-purple-50 p-3 rounded-lg',
    titleClass: 'text-purple-600 font-medium',
    valueClass: 'text-lg font-bold text-purple-800'
  },
  {
    title: '负债总额',
    value: formatAmountWithUnit(statisticsData.value.debtTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-blue-50 p-3 rounded-lg',
    titleClass: 'text-blue-600 font-medium',
    valueClass: 'text-lg font-bold text-blue-800'
  }
])

// 图表数据处理
const formattedDates = computed(() => {
  return allDates.value.map(date => {
    const [year, month, day] = date.split('-')
    return `${month}/${day}`
  })
})

const totalAmountByDate = computed(() => {
  const map: Record<string, number> = {}
  assetRecords.value.forEach(record => {
    if (!record?.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const amount = parseFloat(record.amount) || 0
    map[date] = (map[date] || 0) + amount
  })
  return map
})

const amountByDimension = computed(() => {
  const byName: Record<string, Record<string, number>> = {}
  const byType: Record<string, Record<string, number>> = {}
  const byLocation: Record<string, Record<string, number>> = {}

  assetRecords.value.forEach(record => {
    if (!record?.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const amount = parseFloat(record.amount) || 0

    // 按资产名称
    const nameKey = getDisplayName(record.assetNameId, nameMapping.value, record.assetName, '资产')
    if (!byName[nameKey]) byName[nameKey] = {}
    byName[nameKey][date] = (byName[nameKey][date] || 0) + amount

    // 按资产类型
    const typeKey = getDisplayName(
        record.assetTypeId,
        typeMapping.value,
        record.assetTypeName || record.assetTypeValue,
        '类型'
    )
    if (!byType[typeKey]) byType[typeKey] = {}
    byType[typeKey][date] = (byType[typeKey][date] || 0) + amount

    // 按资产位置
    const locationKey = getDisplayName(
        record.assetLocationId,
        locationMapping.value,
        record.assetLocationName || record.assetLocationValue,
        '位置'
    )
    if (!byLocation[locationKey]) byLocation[locationKey] = {}
    byLocation[locationKey][date] = (byLocation[locationKey][date] || 0) + amount
  })

  return { byName, byType, byLocation }
})

// 图表系列生成
function createSeriesData(dataMap: Record<string, Record<string, number>>, keys: string[]): Array<{ name: string; data: number[] }> {
  return keys.map(key => ({
    name: key,
    data: allDates.value.map(date => dataMap[key]?.[date] ?? 0)
  }))
}

const chartSeries = computed(() => {
  if (!hasData.value || !allDates.value.length) return []

  const series: any[] = []
  let colorIndex = 0
  const { byName, byType, byLocation } = amountByDimension.value

  try {
    // 总金额趋势线
    if (chartOptions.showTotalTrend) {
      const totalData = allDates.value.map(date => totalAmountByDate.value[date] ?? 0)
      if (totalData.some(v => v > 0)) {
        series.push({
          name: '📈 总金额趋势',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          data: totalData,
          lineStyle: { width: 4, color: '#4A5568', shadowColor: 'rgba(74, 85, 104, 0.3)', shadowBlur: 4 },
          itemStyle: { color: '#4A5568', borderWidth: 2, borderColor: '#fff' },
          emphasis: { focus: 'series', scale: true },
          z: 10
        })
      }
      colorIndex++
    }

    // 按维度添加系列
    const dimensionConfigs = [
      { condition: chartOptions.showNameDimension, data: byName, prefix: '💰', symbol: 'circle', lineType: 'solid' },
      { condition: chartOptions.showTypeDimension, data: byType, prefix: '🏷️', symbol: 'triangle', lineType: 'dashed' },
      { condition: chartOptions.showLocationDimension, data: byLocation, prefix: '📍', symbol: 'diamond', lineType: 'dotted' }
    ]

    dimensionConfigs.forEach(config => {
      if (config.condition && config.data) {
        const keys = Object.keys(config.data)
        const seriesData = createSeriesData(config.data, keys)
        seriesData.forEach((item, index) => {
          if (item.data.some(v => v > 0)) {
            const color = CHART_COLORS[(colorIndex + index) % CHART_COLORS.length]
            series.push({
              name: `${config.prefix} ${item.name}`,
              type: 'line',
              smooth: true,
              symbol: config.symbol,
              symbolSize: 5,
              data: item.data,
              lineStyle: { width: 2, type: config.lineType, color, shadowColor: `${color}33`, shadowBlur: 2 },
              itemStyle: { color, borderWidth: 1, borderColor: '#fff' },
              emphasis: { focus: 'series' }
            })
          }
        })
        colorIndex += seriesData.length
      }
    })

    return series
  } catch (error) {
    console.error('Error generating chart series:', error)
    return []
  }
})

// 图表配置
const echartConfig = computed(() => {
  if (!hasData.value || !chartSeries.value.length || !allDates.value.length) return null

  try {
    const hasMultipleDates = allDates.value.length > 7
    const allValues = chartSeries.value.flatMap(s => s.data || [])
    const maxValue = Math.max(...allValues)
    const yAxisMax = maxValue > 0 ? Math.ceil(maxValue * 1.1) : 100

    return {
      title: {
        text: '资产金额趋势分析',
        subtext: `统计期间: ${dateRangeDisplay.value} | 汇总基准: ${lastDateWithRecords.value}`,
        left: 'center',
        top: 15,
        textStyle: { fontSize: 18, fontWeight: 'bold', color: '#2D3748' },
        subtextStyle: { fontSize: 12, color: '#718096' }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross', label: { backgroundColor: '#718096' } },
        backgroundColor: 'rgba(255, 255, 255, 0.96)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        borderRadius: 8,
        textStyle: { color: '#2D3748' },
        extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);',
        formatter: (params: any[]) => {
          if (!Array.isArray(params)) return ''

          const dataIndex = params[0]?.dataIndex
          const date = allDates.value[dataIndex] || ''
          const unitSymbol = statisticsData.value.unitSymbol

          let result = `<div style="font-weight: bold; margin-bottom: 8px; color: #1A202C">${date}</div>`

          const groupedParams = {
            total: params.filter(p => p.seriesName.includes('总金额')),
            name: params.filter(p => p.seriesName.includes('💰')),
            type: params.filter(p => p.seriesName.includes('🏷️')),
            location: params.filter(p => p.seriesName.includes('📍'))
          }

          Object.entries(groupedParams).forEach(([key, series]) => {
            if (series.length > 0) {
              const titles = { total: '💰 总计', name: '📊 按资产名称', type: '🏷️ 按资产类型', location: '📍 按资产位置' }
              result += `<div style="margin-top: 8px; font-weight: 600; color: #4A5568; font-size: 13px">${titles[key as keyof typeof titles]}</div>`
              series.forEach(item => {
                if (item.value > 0) {
                  const formattedAmount = formatAmountWithUnit(item.value, unitSymbol)
                  result += `<div style="display: flex; align-items: center; gap: 8px; margin-top: 4px">
                    <span style="display: inline-block; width: 8px; height: 8px; background: ${item.color}; border-radius: 50%"></span>
                    <span>${item.seriesName.replace(/[💰🏷️📍📈]/g, '').trim()}: <strong>${formattedAmount}</strong></span>
                  </div>`
                }
              })
            }
          })

          return result
        }
      },
      legend: {
        type: 'scroll',
        orient: 'horizontal',
        bottom: hasMultipleDates ? 60 : 15,
        data: chartSeries.value.map(s => s.name),
        textStyle: { fontSize: 11, color: '#4A5568' }
      },
      grid: {
        left: 100,
        right: 50,
        bottom: hasMultipleDates ? 120 : 80,
        top: 80,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: formattedDates.value,
        boundaryGap: false,
        axisLabel: {
          fontSize: 11,
          color: '#718096',
          interval: 0,
          rotate: hasMultipleDates ? 45 : 0
        },
        axisLine: { lineStyle: { color: '#CBD5E0' } },
        axisTick: { alignWithLabel: true, lineStyle: { color: '#CBD5E0' } }
      },
      yAxis: {
        type: 'value',
        name: '金额',
        nameTextStyle: { fontSize: 12, color: '#718096' },
        axisLabel: {
          fontSize: 11,
          color: '#718096',
          formatter: (value: number) => formatAmountWithUnit(value, statisticsData.value.unitSymbol)
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#E2E8F0' } },
        axisLine: { show: false },
        axisTick: { show: false },
        max: yAxisMax,
        minInterval: 1
      },
      series: chartSeries.value,
      dataZoom: hasMultipleDates ? [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', show: true, start: 0, end: 100, height: 20, bottom: 25 }
      ] : undefined,
      animation: true,
      animationDuration: 1200,
      animationEasing: 'cubicOut'
    } as EChartsOption  // 添加类型断言
  } catch (error) {
    console.error('Error generating chart config:', error)
    return null
  }
})

// 图表初始化
async function initializeChart(): Promise<void> {
  if (!isChartReady.value || !hasData.value || !echartConfig.value) return

  try {
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 100))

    if (!chartRef.value) return

    await initChart(echartConfig.value)
    console.log('Chart initialized successfully')
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// 数据加载
async function loadData(): Promise<void> {
  if (!isDateRangeValid.value) {
    showNotification('请选择有效的日期范围', 'error')
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const { startDate, endDate } = parseDateRange(dateRange.value)

    assetStore.updateQuery({
      assetTypeIdList: selectedTypeIds.value.map(id => Number(id)),
      assetNameIdList: selectedNameIds.value.map(id => Number(id)),
      assetLocationIdList: selectedLocationIds.value.map(id => Number(id)),
      startDate,
      endDate,
      remark: remark.value.trim()
    })

    await assetStore.loadAllRecords()

    await nextTick()
    if (hasData.value) {
      showNotification('资产数据加载成功', 'success')
      await initializeChart()
    }
  } catch (error) {
    console.error('Failed to load asset data:', error)
    errorMessage.value = '获取资产数据失败'
    showNotification('获取资产数据失败，请稍后重试', 'error')
  } finally {
    isLoading.value = false
  }
}

// 事件处理
async function handleSearch(): Promise<void> {
  await loadData()
}

async function handleReset(): Promise<void> {
  selectedTypeIds.value = []
  selectedNameIds.value = []
  selectedLocationIds.value = []
  remark.value = ''
  dateRange.value = getDefaultRange()
  Object.assign(chartOptions, {
    showTotalTrend: true,
    showNameDimension: true,
    showTypeDimension: true,
    showLocationDimension: true
  })
  errorMessage.value = ''
  assetStore.allList = []
  await loadData()
}

async function handleRetry(): Promise<void> {
  await loadData()
}

function handleTypeChange(): void {
  selectedNameIds.value = []
}

// 生命周期
onMounted(async () => {
  await nextTick()
  isChartReady.value = true
  dateRange.value = getDefaultRange()
  await loadData()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  destroyChart()
})

// 监听器
watch(
    () => [chartOptions.showTotalTrend, chartOptions.showNameDimension, chartOptions.showTypeDimension, chartOptions.showLocationDimension],
    async () => {
      if (isChartReady.value && !isLoading.value) {
        await initializeChart()
      }
    },
    { deep: true }
)

watch(
    () => assetStore.allList,
    async () => {
      if (isChartReady.value && !isLoading.value) {
        await nextTick()
        await initializeChart()
      }
    },
    { deep: true }
)

watch(chartRef, async (newRef) => {
  if (newRef && isChartReady.value && hasData.value && !isLoading.value) {
    await nextTick()
    await initializeChart()
  }
})
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

.chart-container {
  min-height: 400px;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: #4A5568;
}

.action-button {
  @apply px-6 py-2 text-white rounded-lg transition-colors shadow-sm;
}

button {
  transition: all 0.2s ease;
  font-weight: 500;
}

button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

button:active {
  transform: translateY(0);
}
</style>