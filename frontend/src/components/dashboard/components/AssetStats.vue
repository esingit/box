<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-4">
    <h2 class="text-lg font-semibold mb-4">资产统计</h2>

    <!-- 使用 AssetSearch 组件 -->
    <AssetSearch
        :query="query"
        :asset-name-options="assetNameOptions"
        :asset-type-options="assetTypeOptions"
        :asset-location-options="assetLocationOptions"
        :result-count="filteredRecords.length"
        @search="handleSearch"
        @reset="handleReset"
        @update:query="handleQueryUpdate"
    />

    <!-- 图表显示选项 -->
    <div v-if="shouldShowOptions" class="border rounded-xl p-4">
      <div class="flex flex-wrap items-center gap-4">
        <span class="text-sm font-medium text-gray-600">显示维度:</span>
        <label v-for="option in CHART_OPTIONS_CONFIG" :key="option.key" class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" v-model="chartOptions[option.key]" class="rounded" />
          <span class="text-sm">{{ option.label }}</span>
        </label>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="shouldShowStats" class="grid grid-cols-1 md:grid-cols-5 gap-4 text-sm">
      <div v-for="stat in statisticsCards" :key="stat.title" :class="stat.cardClass">
        <div :class="stat.titleClass">{{ stat.title }}</div>
        <div :class="stat.valueClass">{{ stat.value }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="relative min-h-[500px] h-[calc(100vh-400px)]">
      <!-- 加载状态覆盖层 -->
      <transition name="fade">
        <div v-if="showLoading" class="absolute inset-0 bg-white/80 flex items-center justify-center z-10">
          <div class="flex items-center gap-2 text-gray-600">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
            <span>查询资产数据中...</span>
          </div>
        </div>
      </transition>

      <!-- 实时更新提示 -->
      <transition name="fade">
        <div v-if="isFilterUpdating && !showLoading" class="absolute top-2 right-2 bg-blue-100 text-blue-700 px-3 py-1 rounded-lg text-sm z-10">
          更新中...
        </div>
      </transition>

      <!-- 错误状态 -->
      <div v-if="errorMessage && !showLoading" class="h-full">
        <BaseEmptyState icon="Wallet" :message="errorMessage" description="请检查网络连接或稍后重试" />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="showEmptyState && !showLoading" class="h-full">
        <BaseEmptyState icon="Wallet" message="暂无资产数据" :description="emptyStateDescription" />
      </div>

      <!-- 图表容器 -->
      <div
          v-show="shouldShowChart || hasInitialData"
          ref="chartRef"
          class="w-full h-full chart-container"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, reactive, nextTick, shallowRef } from 'vue'
import { storeToRefs } from 'pinia'
import type {EChartsCoreOption, EChartsType} from 'echarts/core'
import AssetSearch from '@/components/asset/AssetSearch.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import { useAssetStore } from '@/store/assetStore'
import { useDateRange, useChart, isIdInList } from '@/utils/common'
import emitter from '@/utils/eventBus'
import type {AssetRecord, ChartOptionsType, QueryConditions} from '@/types/asset'
import type { Option } from '@/types/common'
import {clearCommonMetaCache} from "@/utils/commonMeta";

// ==================== 常量定义 ====================
const CHART_OPTIONS_STORAGE_KEY = 'asset_chart_options'
const CHART_OPTIONS_CONFIG = [
  { key: 'showTotalTrend', label: '总金额趋势' },
  { key: 'showNameDimension', label: '按资产名称' },
  { key: 'showTypeDimension', label: '按资产类型' },
  { key: 'showLocationDimension', label: '按资产位置' }
] as const

const CHART_COLORS = [
  '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E', '#7B9E9E', '#B8936B',
  '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8', '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B',
  '#9E7B8C', '#7B8C9E', '#A8937B', '#8C8C7B'
]

const ASSET_TYPE_KEYS = {
  SAVINGS: 'SAVINGS',
  FINANCE: 'FINANCE',
  FUND: 'FUND',
  DEBT: 'DEBT'
} as const

const CURRENCY_SYMBOLS = ['￥', 'CNY', '人民币', 'RMB']
const DEFAULT_CURRENCY = '¥'

// ==================== Props ====================
const props = defineProps<{
  assetNameOptions: Option[]
  assetTypeOptions: Option[]
  assetLocationOptions: Option[]
  unitOptions: Option[]
}>()

// ==================== Store & Composables ====================
const assetStore = useAssetStore()
const { query } = storeToRefs(assetStore)
const { getDefaultRange, parseDateRange } = useDateRange()
const { chartRef, initChart, destroyChart, resizeChart } = useChart()

// ==================== 状态管理 ====================
const isLoading = ref(false)
const errorMessage = ref('')
const isChartReady = ref(false)
const isUpdatingChart = ref(false)
const isSearching = ref(false)
const hasInitialData = ref(false)
const chartInstance = shallowRef<EChartsType | null>(null)
const isFilterUpdating = ref(false)
const allLoadedRecords = ref<AssetRecord[]>([])
const dateDataCache = new Map<string, Map<string, number>>()

// ==================== 图表选项管理 ====================
const getSavedChartOptions = (): Partial<ChartOptionsType> => {
  try {
    const saved = localStorage.getItem(CHART_OPTIONS_STORAGE_KEY)
    return saved ? JSON.parse(saved) : {}
  } catch {
    return {}
  }
}

const chartOptions = reactive<ChartOptionsType>({
  showTotalTrend: true,
  showNameDimension: true,
  showTypeDimension: true,
  showLocationDimension: true,
  ...getSavedChartOptions()
})

const saveChartOptions = () => {
  try {
    localStorage.setItem(CHART_OPTIONS_STORAGE_KEY, JSON.stringify(chartOptions))
  } catch (error) {
    console.warn('Failed to save chart options:', error)
  }
}

// ==================== 工具函数 ====================
function debounce<T extends (...args: any[]) => any>(func: T, wait: number): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout>
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', { message, type })
}

function createMapping(options: any[], valueKey = 'value1', fallbackKey = 'label'): Record<string, string> {
  const map: Record<string, string> = {}
  if (!options?.length) return map

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
  return CURRENCY_SYMBOLS.includes(unitSymbol) ? DEFAULT_CURRENCY : unitSymbol
}

function formatRawAmountWithUnit(amount: number, unitSymbol = DEFAULT_CURRENCY): string {
  if (amount === 0) return `${normalizeUnitSymbol(unitSymbol)}0.00`

  const normalizedSymbol = normalizeUnitSymbol(unitSymbol)
  const formattedAmount = amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })

  return `${normalizedSymbol}${formattedAmount}`
}

function formatYAxisAmount(value: number): string {
  if (value === 0) return '¥0'
  if (value >= 100000000) {
    return `¥${(value / 100000000).toFixed(1)}亿`
  } else if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  } else {
    return `¥${value.toLocaleString('zh-CN')}`
  }
}

function getDisplayName(id: string, mapping: Record<string, string>, fallback?: string | null, prefix = '未知'): string {
  return mapping[id] || fallback || `${prefix}${id}`
}

function getDefaultUnitForAssetType(typeId: string | number): string {
  const assetType = props.assetTypeOptions?.find(type =>
      String(type.value) === String(typeId) || String(type.id) === String(typeId)
  )

  if (!assetType?.key3) return DEFAULT_CURRENCY

  const defaultUnit = props.unitOptions?.find(unit => unit.key1 === assetType.key3)
  return defaultUnit ? normalizeUnitSymbol(defaultUnit.value1 || DEFAULT_CURRENCY) : DEFAULT_CURRENCY
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

function safeParseAmount(amount: string | number | null | undefined): number {
  const parsed = parseFloat(String(amount || '0'))
  return isNaN(parsed) ? 0 : parsed
}

// ==================== 计算属性 - 数据映射 ====================
const assetNameOptions = computed(() =>
    props.assetNameOptions?.map(option => ({
      label: option.value1 || option.label || `资产${option.value}`,
      value: option.value || option.id || ''
    })) || []
)

const assetTypeOptions = computed(() =>
    props.assetTypeOptions?.map(option => ({
      label: option.value1 || option.label || `类型${option.value}`,
      value: option.value || option.id || ''
    })) || []
)

const assetLocationOptions = computed(() =>
    props.assetLocationOptions?.map(option => ({
      label: option.value1 || option.label || `位置${option.value}`,
      value: option.value || option.id || ''
    })) || []
)

const nameMapping = computed(() => createMapping(props.assetNameOptions))
const typeMapping = computed(() => createMapping(props.assetTypeOptions))
const locationMapping = computed(() => createMapping(props.assetLocationOptions))
const unitMapping = computed(() => createMapping(props.unitOptions, 'value1'))

// ==================== 计算属性 - 数据处理 ====================
const assetRecords = computed<AssetRecord[]>(() =>
    Array.isArray(assetStore.allList) ? assetStore.allList : []
)

const filteredRecords = computed<AssetRecord[]>(() => {
  let records = [...allLoadedRecords.value]

  if (query.value.assetTypeIdList?.length > 0) {
    records = records.filter(record =>
        isIdInList(record.assetTypeId, query.value.assetTypeIdList)
    )
  }

  if (query.value.assetNameIdList?.length > 0) {
    records = records.filter(record =>
        isIdInList(record.assetNameId, query.value.assetNameIdList)
    )
  }

  if (query.value.assetLocationIdList?.length > 0) {
    records = records.filter(record =>
        isIdInList(record.assetLocationId, query.value.assetLocationIdList)
    )
  }

  if (query.value.remark?.trim()) {
    const searchTerm = query.value.remark.trim().toLowerCase()
    records = records.filter(record =>
        record.remark?.toLowerCase().includes(searchTerm) ||
        record.assetName?.toLowerCase().includes(searchTerm) ||
        record.assetTypeName?.toLowerCase().includes(searchTerm) ||
        record.assetLocationName?.toLowerCase().includes(searchTerm)
    )
  }

  return records
})

// ==================== 计算属性 - 日期数据 ====================
const allDates = computed(() => {
  const dateSet = new Set<string>()
  filteredRecords.value.forEach(record => {
    if (record?.acquireTime) {
      const date = record.acquireTime.split('T')[0]
      if (date) dateSet.add(date)
    }
  })
  return Array.from(dateSet).sort()
})

const formattedDates = computed(() => {
  return allDates.value.map(date => {
    const [year, month, day] = date.split('-')
    return `${month}/${day}`
  })
})

const lastDateWithRecords = computed(() =>
    allDates.value.length ? allDates.value[allDates.value.length - 1] : ''
)

const lastDateRecords = computed(() => {
  if (!lastDateWithRecords.value) return []
  return filteredRecords.value.filter(record =>
      record?.acquireTime?.startsWith(lastDateWithRecords.value)
  )
})

// ==================== 计算属性 - 统计数据 ====================
const getTypeTotal = (typeKey: string) => {
  const typeIds = props.assetTypeOptions
      ?.filter(type => type.key1 === typeKey)
      ?.map(type => String(type.id || type.value)) || []

  return lastDateRecords.value
      .filter(record => typeIds.includes(String(record.assetTypeId)))
      .reduce((sum, record) => sum + safeParseAmount(record.amount), 0)
}

const statisticsData = computed(() => {
  const totalAmount = lastDateRecords.value.reduce((sum, record) =>
      sum + safeParseAmount(record.amount), 0
  )

  const unitSymbol = lastDateRecords.value.length > 0
      ? getUnitSymbol(lastDateRecords.value[0])
      : DEFAULT_CURRENCY

  return {
    totalAmount,
    savingsTotal: getTypeTotal(ASSET_TYPE_KEYS.SAVINGS),
    financeTotal: getTypeTotal(ASSET_TYPE_KEYS.FINANCE),
    fundTotal: getTypeTotal(ASSET_TYPE_KEYS.FUND),
    debtTotal: getTypeTotal(ASSET_TYPE_KEYS.DEBT),
    unitSymbol
  }
})

const statisticsCards = computed(() => [
  {
    title: '总金额',
    value: formatRawAmountWithUnit(statisticsData.value.totalAmount, statisticsData.value.unitSymbol),
    cardClass: 'bg-red-50 p-3 rounded-lg',
    titleClass: 'text-red-600 font-medium',
    valueClass: 'text-lg font-bold text-red-800'
  },
  {
    title: '储蓄类型总额',
    value: formatRawAmountWithUnit(statisticsData.value.savingsTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-green-50 p-3 rounded-lg',
    titleClass: 'text-green-600 font-medium',
    valueClass: 'text-lg font-bold text-green-800'
  },
  {
    title: '理财类型总额',
    value: formatRawAmountWithUnit(statisticsData.value.financeTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-yellow-50 p-3 rounded-lg',
    titleClass: 'text-yellow-600 font-medium',
    valueClass: 'text-lg font-bold text-yellow-800'
  },
  {
    title: '基金类型总额',
    value: formatRawAmountWithUnit(statisticsData.value.fundTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-purple-50 p-3 rounded-lg',
    titleClass: 'text-purple-600 font-medium',
    valueClass: 'text-lg font-bold text-purple-800'
  },
  {
    title: '负债总额',
    value: formatRawAmountWithUnit(statisticsData.value.debtTotal, statisticsData.value.unitSymbol),
    cardClass: 'bg-blue-50 p-3 rounded-lg',
    titleClass: 'text-blue-600 font-medium',
    valueClass: 'text-lg font-bold text-blue-800'
  }
])

// ==================== 计算属性 - 显示控制 ====================
const hasData = computed(() => filteredRecords.value.length > 0)
const hasSearchConditions = computed(() =>
    query.value.assetTypeIdList.length > 0 ||
    query.value.assetNameIdList.length > 0 ||
    query.value.assetLocationIdList.length > 0 ||
    query.value.remark.trim() !== ''
)

const dateRangeDisplay = computed(() => {
  if (!query.value.startDate || !query.value.endDate) return ''
  return `${query.value.startDate} ~ ${query.value.endDate}`
})

const emptyStateDescription = computed(() => {
  if (!query.value.startDate || !query.value.endDate) {
    return '请选择日期范围查看资产数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到资产记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无资产记录`
})

const showLoading = computed(() => isLoading.value && isSearching.value)
const showEmptyState = computed(() =>
    !hasData.value &&
    !errorMessage.value &&
    query.value?.startDate &&
    query.value?.endDate &&
    !isLoading.value
)

const shouldShowChart = computed(() =>
    hasData.value &&
    !errorMessage.value &&
    isChartReady.value
)

const shouldShowOptions = computed(() => hasData.value || hasInitialData.value)
const shouldShowStats = computed(() => hasData.value || hasInitialData.value)

// ==================== 计算属性 - 图表数据 ====================
const amountByDimension = computed(() => {
  const byName: Record<string, Record<string, number>> = {}
  const byType: Record<string, Record<string, number>> = {}
  const byLocation: Record<string, Record<string, number>> = {}

  dateDataCache.clear()

  for (const record of filteredRecords.value) {
    if (!record?.acquireTime) continue

    const date = record.acquireTime.split('T')[0]
    const amount = safeParseAmount(record.amount)
    if (amount === 0) continue

    // 缓存日期数据
    if (!dateDataCache.has(date)) {
      dateDataCache.set(date, new Map())
    }
    const dateMap = dateDataCache.get(date)!
    const assetNameIdKey = String(record.assetNameId)
    dateMap.set(assetNameIdKey, (dateMap.get(assetNameIdKey) || 0) + amount)

    // 按维度聚合
    const nameKey = getDisplayName(String(record.assetNameId), nameMapping.value, record.assetName, '资产')
    if (!byName[nameKey]) byName[nameKey] = {}
    byName[nameKey][date] = (byName[nameKey][date] || 0) + amount

    const typeKey = getDisplayName(
        String(record.assetTypeId),
        typeMapping.value,
        record.assetTypeName || record.assetTypeValue,
        '类型'
    )
    if (!byType[typeKey]) byType[typeKey] = {}
    byType[typeKey][date] = (byType[typeKey][date] || 0) + amount

    const locationKey = getDisplayName(
        String(record.assetLocationId),
        locationMapping.value,
        record.assetLocationName || record.assetLocationValue,
        '位置'
    )
    if (!byLocation[locationKey]) byLocation[locationKey] = {}
    byLocation[locationKey][date] = (byLocation[locationKey][date] || 0) + amount
  }

  return { byName, byType, byLocation }
})

const totalAmountByDate = computed(() => {
  const map: Record<string, number> = {}
  for (const record of filteredRecords.value) {
    if (!record?.acquireTime) continue
    const date = record.acquireTime.split('T')[0]
    const amount = safeParseAmount(record.amount)
    if (amount > 0) {
      map[date] = (map[date] || 0) + amount
    }
  }
  return map
})

// ==================== 图表系列生成 ====================
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
          lineStyle: {
            width: 4,
            color: '#4A5568',
            shadowColor: 'rgba(74, 85, 104, 0.3)',
            shadowBlur: 4
          },
          itemStyle: {
            color: '#4A5568',
            borderWidth: 2,
            borderColor: '#fff'
          },
          emphasis: {
            focus: 'series',
            scale: true
          },
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
              lineStyle: {
                width: 2,
                type: config.lineType,
                color,
                shadowColor: `${color}33`,
                shadowBlur: 2
              },
              itemStyle: {
                color,
                borderWidth: 1,
                borderColor: '#fff'
              },
              emphasis: {
                focus: 'series'
              }
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

// ==================== 图表配置 ====================
const echartConfig = computed(() => {
  if (!hasData.value || !chartSeries.value.length || !allDates.value.length) return null

  try {
    const hasMultipleDates = allDates.value.length > 7
    const allValues = chartSeries.value.flatMap(s => s.data || []).filter(v => !isNaN(v))
    const maxValue = allValues.length > 0 ? Math.max(...allValues) : 100
    const yAxisMax = maxValue > 0 ? Math.ceil(maxValue * 1.1) : 100

    return {
      title: {
        text: '资产金额趋势分析',
        subtext: `统计期间: ${dateRangeDisplay.value} | 汇总基准: ${lastDateWithRecords.value}`,
        left: 'center',
        top: 15,
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold',
          color: '#2D3748'
        },
        subtextStyle: {
          fontSize: 12,
          color: '#718096'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#718096'
          }
        },
        backgroundColor: 'rgba(255, 255, 255, 0.96)',
        borderColor: '#E2E8F0',
        borderWidth: 1,
        borderRadius: 8,
        textStyle: {
          color: '#2D3748'
        },
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
              const titles = {
                total: '💰 总计',
                name: '📊 按资产名称',
                type: '🏷️ 按资产类型',
                location: '📍 按资产位置'
              }
              result += `<div style="margin-top: 8px; font-weight: 600; color: #4A5568; font-size: 13px">${titles[key as keyof typeof titles]}</div>`
              series.forEach(item => {
                if (item.value > 0) {
                  const formattedAmount = formatRawAmountWithUnit(item.value, unitSymbol)
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
        textStyle: {
          fontSize: 11,
          color: '#4A5568'
        }
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
        axisLine: {
          lineStyle: {
            color: '#CBD5E0'
          }
        },
        axisTick: {
          alignWithLabel: true,
          lineStyle: {
            color: '#CBD5E0'
          }
        }
      },
      yAxis: {
        type: 'value',
        name: '金额',
        nameTextStyle: {
          fontSize: 12,
          color: '#718096'
        },
        axisLabel: {
          fontSize: 11,
          color: '#718096',
          formatter: formatYAxisAmount
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#E2E8F0'
          }
        },
        axisLine: {
          show: false
        },
        axisTick: {
          show: false
        },
        max: yAxisMax,
        minInterval: 1
      },
      series: chartSeries.value,
      dataZoom: hasMultipleDates ? [
        {
          type: 'inside',
          start: 0,
          end: 100
        },
        {
          type: 'slider',
          show: true,
          start: 0,
          end: 100,
          height: 20,
          bottom: 25
        }
      ] : undefined,
      animation: true,
      animationDuration: 600,
      animationEasing: 'cubicOut'
    } as EChartsCoreOption
  } catch (error) {
    console.error('Error generating chart config:', error)
    return null
  }
})

// ==================== 图表操作函数 ====================
async function updateChartData(): Promise<void> {
  if (!shouldShowChart.value || !echartConfig.value || isUpdatingChart.value) {
    return
  }

  isUpdatingChart.value = true

  try {
    if (chartInstance.value) {
      chartInstance.value.clear()
      chartInstance.value.setOption(echartConfig.value, {
        notMerge: true,
        lazyUpdate: false
      })
      console.log('✅ 图表数据完全替换成功')
    } else {
      await initializeChart()
    }
  } catch (error) {
    console.error('Failed to update chart:', error)
    errorMessage.value = '图表更新失败'
  } finally {
    isUpdatingChart.value = false
  }
}

async function initializeChart(): Promise<void> {
  if (!shouldShowChart.value || !echartConfig.value) return

  try {
    if (!chartRef.value) {
      await nextTick()
    }

    if (!chartRef.value) {
      console.warn('Chart container not found')
      return
    }

    const rect = chartRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
      setTimeout(() => initializeChart(), 50)
      return
    }

    if (chartInstance.value) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }

    chartInstance.value = await initChart(echartConfig.value)
    console.log('✅ 图表初始化成功')
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// ==================== 防抖函数 ====================
const debouncedUpdateChart = debounce(async () => {
  await updateChartData()
}, 200)

const debouncedLoadData = debounce(async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    await assetStore.loadAllRecords()
    allLoadedRecords.value = [...assetRecords.value]

    await nextTick()
    if (hasData.value) {
      showNotification('资产数据加载成功', 'success')
    }
  } catch (error: any) {
    console.error('Failed to load asset data:', error)
    errorMessage.value = '获取资产数据失败'
    showNotification('获取资产数据失败，请稍后重试', 'error')
  } finally {
    isLoading.value = false
  }
}, 100)

const debouncedFilterUpdate = debounce(async () => {
  isFilterUpdating.value = true

  await nextTick()

  if (shouldShowChart.value) {
    await updateChartData()
  }

  setTimeout(() => {
    isFilterUpdating.value = false
  }, 300)
}, 150)

// ==================== 数据加载与事件处理 ====================
async function loadData(): Promise<void> {
  if (!query.value?.startDate || !query.value?.endDate) {
    showNotification('请选择有效的日期范围', 'error')
    return
  }

  console.log('🟢 开始加载资产数据', {
    startDate: query.value.startDate,
    endDate: query.value.endDate
  })

  await debouncedLoadData()
}

async function handleQueryUpdate(newQuery: Partial<QueryConditions>) {
  console.log('🔄 查询条件实时更新', newQuery)

  assetStore.updateQuery(newQuery)

  if (newQuery.startDate !== undefined || newQuery.endDate !== undefined) {
    await loadData()
  } else {
    isFilterUpdating.value = true
    await nextTick()

    if (shouldShowChart.value) {
      await debouncedUpdateChart()
    }

    setTimeout(() => {
      isFilterUpdating.value = false
    }, 300)
  }
}

async function handleSearch(searchQuery?: QueryConditions): Promise<void> {
  try {
    console.log('🟢 处理搜索请求', searchQuery)
    isSearching.value = true

    if (searchQuery) {
      const needReload = searchQuery.startDate !== query.value.startDate ||
          searchQuery.endDate !== query.value.endDate

      assetStore.updateQuery(searchQuery)

      if (needReload) {
        await loadData()
      } else {
        await debouncedFilterUpdate()
      }
    } else {
      await loadData()
    }
  } catch (error) {
    console.error('❌ 处理搜索请求失败', error)
    showNotification('搜索失败，请重试', 'error')
  } finally {
    isSearching.value = false
  }
}

async function handleReset(): Promise<void> {
  try {
    console.log('🟢 处理重置请求')
    isSearching.value = true

    assetStore.resetQuery()

    const defaultRange = getDefaultRange()
    const { startDate, endDate } = parseDateRange(defaultRange)
    assetStore.updateQuery({ startDate, endDate })

    Object.assign(chartOptions, {
      showTotalTrend: true,
      showNameDimension: true,
      showTypeDimension: true,
      showLocationDimension: true
    })
    saveChartOptions()

    errorMessage.value = ''
    await loadData()
  } catch (error) {
    console.error('❌ 处理重置请求失败', error)
    showNotification('重置失败，请重试', 'error')
    isSearching.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  console.log('🟢 组件挂载')

  await nextTick()
  isChartReady.value = true

  if (!query.value.startDate || !query.value.endDate) {
    const defaultRange = getDefaultRange()
    const { startDate, endDate } = parseDateRange(defaultRange)
    assetStore.updateQuery({ startDate, endDate })
  }

  await loadData()

  if (typeof window !== 'undefined') {
    window.addEventListener('resize', resizeChart, {
      passive: true,
      capture: false
    })
  }
})

onBeforeUnmount(() => {
  console.log('🟢 组件卸载')

  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', resizeChart)
  }

  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }

  destroyChart()
  dateDataCache.clear()
  clearCommonMetaCache()
})

// ==================== 监听器 ====================
watch(
    () => chartOptions,
    () => {
      console.log('🟢 图表选项改变，更新图表')
      if (shouldShowChart.value) {
        debouncedUpdateChart()
      }
      saveChartOptions()
    },
    { deep: true }
)

watch(
    () => isLoading.value,
    (newLoading, oldLoading) => {
      console.log('🟢 loading状态改变', { newLoading, oldLoading })

      if (oldLoading && !newLoading) {
        isSearching.value = false

        if (hasData.value) {
          hasInitialData.value = true
          console.log('📊 数据加载完成，准备更新图表')
          nextTick(() => {
            if (shouldShowChart.value) {
              debouncedUpdateChart()
            }
          })
        }
      }
    }
)

watch(
    echartConfig,
    (newConfig) => {
      if (newConfig && chartInstance.value && !isLoading.value) {
        debouncedUpdateChart()
      }
    },
    { deep: true }
)

watch(
    () => query.value.assetTypeIdList,
    () => {
      assetStore.updateQuery({ assetNameIdList: [] })
    }
)

watch(
    () => ({
      assetTypeIdList: [...query.value.assetTypeIdList],
      assetNameIdList: [...query.value.assetNameIdList],
      assetLocationIdList: [...query.value.assetLocationIdList],
      remark: query.value.remark
    }),
    (newVal, oldVal) => {
      if (isLoading.value || !hasInitialData.value) return

      const hasChange =
          JSON.stringify(newVal.assetTypeIdList) !== JSON.stringify(oldVal.assetTypeIdList) ||
          JSON.stringify(newVal.assetNameIdList) !== JSON.stringify(oldVal.assetNameIdList) ||
          JSON.stringify(newVal.assetLocationIdList) !== JSON.stringify(oldVal.assetLocationIdList) ||
          newVal.remark !== oldVal.remark

      if (hasChange) {
        console.log('🔄 过滤条件变化，实时更新图表')
        debouncedFilterUpdate()
      }
    },
    { deep: true }
)
</script>