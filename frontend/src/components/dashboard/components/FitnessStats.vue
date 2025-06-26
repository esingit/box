<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-4">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <FitnessSearch
        v-model:query="query"
        :fitness-type-options="fitnessTypeOptions"
        :result-count="filteredRecords.length"
        @search="handleSearchFromComponent"
        @reset="handleResetFromComponent"
        @update:query="handleQueryUpdate"
    />

    <!-- 图表显示选项 -->
    <div v-if="shouldShowOptions" class="border rounded-xl p-4">
      <div class="flex flex-wrap items-center gap-4">
        <span class="text-sm font-medium text-gray-600">显示选项:</span>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showDataLabels"
              class="rounded"
          />
          <span class="text-sm">显示数据标签</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showAreaFill"
              class="rounded"
          />
          <span class="text-sm">显示面积填充</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.smoothCurve"
              class="rounded"
          />
          <span class="text-sm">平滑曲线</span>
        </label>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="shouldShowStats" class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
      <div class="bg-green-50 p-3 rounded-lg">
        <div class="text-green-600 font-medium">运动天数</div>
        <div class="text-lg font-bold text-green-800">{{ exerciseDaysCount }}</div>
      </div>
      <div class="bg-purple-50 p-3 rounded-lg">
        <div class="text-purple-600 font-medium">蛋白质汇总</div>
        <div class="text-lg font-bold text-purple-800">{{ proteinCount }}g</div>
      </div>
      <div class="bg-blue-50 p-3 rounded-lg">
        <div class="text-blue-600 font-medium">俯卧撑总数</div>
        <div class="text-lg font-bold text-blue-800">{{ pushUpCount }}</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="relative min-h-[400px] h-[calc(100vh-300px)]">
      <!-- 加载状态覆盖层 -->
      <transition name="fade">
        <div v-if="showLoading" class="absolute inset-0 bg-white/80 flex items-center justify-center z-10">
          <div class="flex items-center gap-2 text-gray-600">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
            <span>{{ loadingText }}</span>
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
        <BaseEmptyState
            icon="Dumbbell"
            :message="errorMessage"
            description="请检查网络连接或稍后重试"
        />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="showEmptyState && !showLoading" class="h-full">
        <BaseEmptyState
            icon="Dumbbell"
            message="暂无健身数据"
            :description="emptyStateDescription"
        />
      </div>

      <!-- 图表容器 - 始终渲染以便快速更新 -->
      <div
          v-show="shouldShowChart || hasInitialData"
          ref="chartRef"
          class="w-full h-full chart-container"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, ComputedRef, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch, onUnmounted} from 'vue'
import {storeToRefs} from 'pinia'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'
import {useFitnessStore} from '@/store/fitnessStore'
import {useChart, useDateRange} from '@/utils/common'
import emitter from '@/utils/eventBus'
import type {EChartsOption} from 'echarts'
import type {EChartsType} from 'echarts/core'
import {FormattedFitnessRecord} from '@/types/fitness'
import type {Option} from '@/types/common'
import {clearCommonMetaCache} from "@/utils/commonMeta";

// 为了兼容，创建别名
type FitnessRecord = FormattedFitnessRecord

// 定义查询条件类型
interface QueryType {
  typeIdList: number[]
  startDate: string
  endDate: string
  remark: string
}

// 常量
const EXERCISE_TYPE_KEY = 'EXERCISE'
const CHART_COLORS = [
  '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E',
  '#7B9E9E', '#B8936B', '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8',
  '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B', '#9E7B8C', '#7B8C9E'
]

// Props
const props = defineProps<{
  fitnessTypeOptions: Option[]
  unitOptions: Option[]
}>()

// Store
const fitnessStore = useFitnessStore()
// 只从 storeToRefs 解构响应式状态
const {query, allList, loadingState} = storeToRefs(fitnessStore)
// 直接从 store 获取方法
const {
  loadAllRecordsDebounced,
  updateQuery,
  resetQuery,
  cleanup
} = fitnessStore

// Composables
const {getDefaultRange, parseDateRange} = useDateRange()
const {chartRef, initChart, destroyChart, resizeChart} = useChart()

// 添加组件是否已卸载的标志
const isComponentUnmounted = ref(false)

// 状态管理
const errorMessage = ref('')
const isChartReady = ref(false)
const isUpdatingChart = ref(false)
const isSearching = ref(false)
const hasInitialData = ref(false)
const chartInstance = shallowRef<EChartsType | null>(null)
const isFilterUpdating = ref(false)
const allLoadedRecords = ref<FitnessRecord[]>([])

// 使用正确的 loading 状态
const isLoading = computed(() => {
  if (isComponentUnmounted.value) return false
  return loadingState?.value?.list ?? false
})

// 图表选项
const chartOptions = reactive({
  showDataLabels: false,
  showAreaFill: true,
  smoothCurve: true
})

// 添加 watchers 的清理函数数组
const watchStoppers: Array<() => void> = []

// 工具函数
function debounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout>
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

function quickDebounce<T extends (...args: any[]) => any>(
    func: T,
    wait: number = 50
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout>
  let lastCallTime = 0

  return (...args: Parameters<T>) => {
    const now = Date.now()
    const timeSinceLastCall = now - lastCallTime

    if (timeSinceLastCall >= wait) {
      lastCallTime = now
      func(...args)
    } else {
      clearTimeout(timeout)
      timeout = setTimeout(() => {
        lastCallTime = Date.now()
        func(...args)
      }, wait - timeSinceLastCall)
    }
  }
}

function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', {message, type})
}

function createSafeQuery(inputQuery: any): QueryType {
  const defaultQuery: QueryType = {
    typeIdList: [],
    startDate: '',
    endDate: '',
    remark: ''
  }

  if (!inputQuery || typeof inputQuery !== 'object') {
    return defaultQuery
  }

  return {
    typeIdList: Array.isArray(inputQuery.typeIdList) ? inputQuery.typeIdList : [],
    startDate: typeof inputQuery.startDate === 'string' ? inputQuery.startDate : '',
    endDate: typeof inputQuery.endDate === 'string' ? inputQuery.endDate : '',
    remark: typeof inputQuery.remark === 'string' ? inputQuery.remark : ''
  }
}

// 安全的计算属性包装器
function safeComputed<T>(getter: () => T, defaultValue: T): ComputedRef<T> {
  return computed(() => {
    if (isComponentUnmounted.value) return defaultValue
    try {
      return getter()
    } catch (error) {
      console.warn('Computed property error:', error)
      return defaultValue
    }
  })
}

// 计算属性 - 数据相关
const fitnessTypeOptions = safeComputed(() => {
  if (!props.fitnessTypeOptions?.length) return []

  return props.fitnessTypeOptions.map(option => ({
    label: option.value1 || option.label || `类型${option.value}`,
    value: option.value || option.id || ''
  }))
}, [])

// 从 store 获取的原始数据
const fitnessRecords = safeComputed<FitnessRecord[]>(() => {
  const records = Array.isArray(allList?.value) ? allList.value : []
  // 确保每条记录都有必要的字段
  return records.map(record => ({
    ...record,
    id: record.id || '',
    typeId: record.typeId || record.assetTypeId || '',
    count: record.count || 0,
    unitId: record.unitId || '',
    finishTime: record.finishTime || record.date || '',
    remark: record.remark || ''
  }))
}, [])

// 添加过滤后的记录计算属性 - 修复类型问题
const filteredRecords = safeComputed<FitnessRecord[]>(() => {
  let records = [...allLoadedRecords.value]

  // 根据查询条件过滤 - 修复类型转换问题
  if (query?.value?.typeIdList?.length > 0) {
    const typeIdStrings = query.value.typeIdList.map(id => String(id))
    records = records.filter(record =>
        typeIdStrings.includes(String(record.typeId))
    )
  }

  if (query?.value?.remark?.trim()) {
    const searchTerm = query.value.remark.trim().toLowerCase()
    records = records.filter(record => {
      // 获取类型名称
      const typeOption = props.fitnessTypeOptions?.find(type =>
          String(type.value) === String(record.typeId) ||
          String(type.id) === String(record.typeId)
      )
      const typeName = typeOption?.value1 || typeOption?.label || ''

      return record.remark?.toLowerCase().includes(searchTerm) ||
          typeName.toLowerCase().includes(searchTerm)
    })
  }

  return records
}, [])

const hasData = safeComputed(() => {
  return filteredRecords.value.length > 0
}, false)

const hasSearchConditions = safeComputed(() => {
  return query?.value?.typeIdList?.length > 0 || (query?.value?.remark || '').trim() !== ''
}, false)

const dateRangeDisplay = safeComputed(() => {
  return formatDateRange(query?.value?.startDate || '', query?.value?.endDate || '')
}, '')

const emptyStateDescription = safeComputed(() => {
  if (!query?.value?.startDate || !query?.value?.endDate) {
    return '请选择日期范围查看健身数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到健身记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无健身记录，开始您的健身之旅吧！`
}, '请选择日期范围查看健身数据')

// 计算属性 - 显示控制
const showLoading = safeComputed(() => {
  return isLoading.value && isSearching.value
}, false)

const loadingText = safeComputed(() => {
  return '查询健身数据中...'
}, '查询健身数据中...')

const showEmptyState = safeComputed(() => {
  return !hasData.value &&
      !errorMessage.value &&
      query?.value?.startDate &&
      query?.value?.endDate &&
      !isLoading.value
}, false)

const shouldShowChart = safeComputed(() => {
  return hasData.value &&
      !errorMessage.value &&
      isChartReady.value
}, false)

const shouldShowOptions = safeComputed(() => {
  return hasData.value || hasInitialData.value
}, false)

const shouldShowStats = safeComputed(() => {
  return hasData.value || hasInitialData.value
}, false)

const effectiveTypeIds = safeComputed(() => {
  if (!props.fitnessTypeOptions?.length) return []

  // 获取实际有数据的类型ID
  const dataTypeIds = new Set<string>()
  filteredRecords.value.forEach(record => {
    if (record?.typeId) {
      dataTypeIds.add(String(record.typeId))
    }
  })

  // 如果有查询条件，使用查询条件与实际数据的交集
  if (query?.value?.typeIdList?.length > 0) {
    return query.value.typeIdList.filter(id =>
        dataTypeIds.has(String(id))
    )
  }

  // 否则返回所有有数据的类型
  return Array.from(dataTypeIds)
}, [])

const allDates = safeComputed(() => {
  const dateSet = new Set<string>()

  filteredRecords.value.forEach(record => {
    if (record?.finishTime) {
      const date = record.finishTime.split('T')[0]
      if (date) dateSet.add(date)
    }
  })

  return Array.from(dateSet).sort()
}, [])

const formattedDates = safeComputed(() => {
  return allDates.value.map(date => {
    const [year, month, day] = date.split('-')
    return `${month}/${day}`
  })
}, [])

// 统计相关计算属性 - 使用过滤后的数据
const exerciseDaysCount = safeComputed(() => {
  if (!filteredRecords.value.length) return 0

  const exerciseDays = new Set<string>()
  for (const record of filteredRecords.value) {
    if (isExerciseType(record.typeId) && record.finishTime) {
      exerciseDays.add(record.finishTime.split('T')[0])
    }
  }

  return exerciseDays.size
}, 0)

const pushUpCount = safeComputed(() => {
  if (!filteredRecords.value.length || !props.fitnessTypeOptions?.length) return 0

  let sum = 0
  for (const record of filteredRecords.value) {
    const fitnessType = props.fitnessTypeOptions?.find(type =>
        String(type.value) === String(record.typeId) ||
        String(type.id) === String(record.typeId)
    )
    if (fitnessType?.key1 === 'PUSH_UP') {
      sum += Number(record.count || 0)
    }
  }
  return sum
}, 0)

const proteinCount = safeComputed(() => {
  if (!filteredRecords.value.length || !props.fitnessTypeOptions?.length) return 0

  let sum = 0
  for (const record of filteredRecords.value) {
    const fitnessType = props.fitnessTypeOptions?.find(type =>
        String(type.value) === String(record.typeId) ||
        String(type.id) === String(record.typeId)
    )
    if (fitnessType?.key1 === 'PROTEIN' || fitnessType?.value1?.includes('蛋白')) {
      sum += Number(record.count || 0)
    }
  }
  return sum
}, 0)

// 单位映射
const unitMapping = safeComputed(() => {
  const map: Record<string, string> = {}
  if (!props.unitOptions?.length) return map

  for (const option of props.unitOptions) {
    if (option) {
      if (option.id && option.value1) {
        map[String(option.id)] = option.value1
      }
      if (option.value && option.value1) {
        map[String(option.value)] = option.value1
      }
    }
  }
  return map
}, {})

// 图表相关函数
function formatDateRange(startDate: string, endDate: string): string {
  if (!startDate || !endDate) return ''

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  }

  const start = formatDate(startDate)
  const end = formatDate(endDate)

  return start === end ? start : `${start} ~ ${end}`
}

function isExerciseType(typeId: string | number | undefined): boolean {
  if (!typeId) return false

  const fitnessType = props.fitnessTypeOptions?.find(type =>
      String(type.value) === String(typeId) ||
      String(type.id) === String(typeId)
  )
  return fitnessType?.key2 === EXERCISE_TYPE_KEY
}

function getRecordUnit(typeId: string | number, date: string): string {
  const records = filteredRecords.value.filter(record =>
      record &&
      String(record.typeId) === String(typeId) &&
      record.finishTime?.startsWith(date)
  )

  if (records.length > 0 && records[0].unitId) {
    const unitId = String(records[0].unitId)
    const unitName = unitMapping.value[unitId]
    if (unitName) {
      return unitName
    }
  }

  return getDefaultUnitForType(typeId)
}

function getDefaultUnitForType(typeId: string | number): string {
  const fitnessType = props.fitnessTypeOptions?.find(type =>
      String(type.value) === String(typeId) || String(type.id) === String(typeId)
  )

  if (!fitnessType?.key3) {
    return ''
  }

  const defaultUnit = props.unitOptions?.find(unit => unit.key1 === fitnessType.key3)
  if (!defaultUnit) {
    return ''
  }

  return defaultUnit.value1 || ''
}

function formatValueWithUnit(value: number, typeId: string | number, date: string): string {
  if (value === 0) return '0'

  const unit = getRecordUnit(typeId, date)

  let formattedValue: string
  if (value >= 1000) {
    formattedValue = `${(value / 1000).toFixed(1)}k`
  } else if (value >= 100) {
    formattedValue = value.toFixed(0)
  } else if (value >= 10) {
    formattedValue = value.toFixed(1)
  } else {
    formattedValue = value.toFixed(2)
  }

  return unit ? `${formattedValue}${unit}` : formattedValue
}

function formatValue(value: number): string {
  if (value === 0) return '0'

  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}k`
  } else if (value >= 100) {
    return value.toFixed(0)
  } else if (value >= 10) {
    return value.toFixed(1)
  } else {
    return value.toFixed(2)
  }
}

// 图表配置生成 - 使用过滤后的数据
const chartSeries = safeComputed(() => {
  if (!hasData.value || !allDates.value.length) return []

  try {
    // 每次计算都创建新的缓存，避免污染
    const dateDataCache = new Map<string, Map<string, number>>()

    // 收集实际有数据的类型
    const actualTypeIds = new Set<string>()

    // 预处理数据 - 使用过滤后的数据
    for (const record of filteredRecords.value) {
      if (record?.finishTime && record?.typeId) {
        const date = record.finishTime.split('T')[0]
        const typeId = String(record.typeId)

        // 记录实际存在的类型
        actualTypeIds.add(typeId)

        if (!dateDataCache.has(date)) {
          dateDataCache.set(date, new Map())
        }

        const typeMap = dateDataCache.get(date)!
        const currentValue = typeMap.get(typeId) || 0
        typeMap.set(typeId, currentValue + Number(record.count || 0))
      }
    }

    console.log('📊 实际数据中的类型：', Array.from(actualTypeIds))
    console.log('📊 计划显示的类型：', effectiveTypeIds.value)

    // 根据查询条件决定显示哪些类型
    let typeIdsToShow: (string | number)[]

    if (query?.value?.typeIdList?.length > 0) {
      // 如果有查询条件，只显示查询的类型（与实际数据的交集）
      typeIdsToShow = query.value.typeIdList.filter(id =>
          actualTypeIds.has(String(id))
      )
    } else {
      // 如果没有查询条件，只显示有数据的类型
      typeIdsToShow = Array.from(actualTypeIds)
    }

    console.log('📊 最终显示的类型：', typeIdsToShow)

    // 只为有数据的类型生成系列
    return typeIdsToShow
        .map((typeId, index) => {
          const typeOption = props.fitnessTypeOptions?.find(item =>
              String(item.value) === String(typeId) ||
              String(item.id) === String(typeId)
          )
          const typeName = typeOption?.value1 || typeOption?.label || `类型${typeId}`

          // 获取该类型的数据
          const data = allDates.value.map(date => {
            const typeMap = dateDataCache.get(date)
            return typeMap?.get(String(typeId)) || 0
          })

          // 确认这个类型确实有数据
          if (!data.some(value => value > 0)) {
            console.log(`📊 类型 ${typeName} 没有数据，跳过`)
            return null
          }

          const color = CHART_COLORS[index % CHART_COLORS.length]

          return {
            name: typeName,
            type: 'line',
            data,
            typeId,
            smooth: chartOptions.smoothCurve,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              width: 2,
              color,
              shadowColor: `${color}33`,
              shadowBlur: 2
            },
            itemStyle: {
              color,
              borderWidth: 1,
              borderColor: '#fff'
            },
            areaStyle: chartOptions.showAreaFill ? {
              color: `${color}26`
            } : undefined,
            label: chartOptions.showDataLabels ? {
              show: true,
              fontSize: 10,
              color: '#666',
              position: 'top',
              formatter: (params: any) => {
                const {value, dataIndex} = params
                if (value <= 0) return ''
                const date = allDates.value[dataIndex]
                return formatValueWithUnit(value, typeId, date)
              }
            } : undefined,
            emphasis: {
              focus: 'series',
              scale: true
            }
          }
        })
        .filter(Boolean)
  } catch (error) {
    console.error('Error generating chart series:', error)
    return []
  }
}, [])

// ECharts 配置
const echartConfig = safeComputed(() => {
  if (!hasData.value || !chartSeries.value.length || !allDates.value.length) {
    return null
  }

  try {
    const hasMultipleDates = allDates.value.length > 7

    return {
      title: {
        text: '健身统计趋势',
        subtext: `统计期间: ${dateRangeDisplay.value}`,
        left: 'center',
        top: 15,
        textStyle: {
          fontSize: 16,
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
          if (!Array.isArray(params) || params.length === 0) return ''

          const dataIndex = params[0]?.dataIndex
          const date = allDates.value[dataIndex] || ''
          let result = `<div style="font-weight: bold; margin-bottom: 8px; color: #1A202C">${date}</div>`

          params.forEach((param) => {
            if (param.value > 0) {
              const series = chartSeries.value.find(s => s && s.name === param.seriesName)
              if (series && series.typeId !== undefined) {
                const typeId = series.typeId as string | number
                const unit = getRecordUnit(typeId, date)
                const formattedValue = formatValue(param.value)
                const displayValue = unit ? `${formattedValue}${unit}` : formattedValue

                result += `<div style="display: flex; align-items: center; gap: 6px; margin-top: 3px">
                  <span style="display: inline-block; width: 8px; height: 8px; background: ${param.color}; border-radius: 50%"></span>
                  <span>${param.seriesName}: <strong>${displayValue}</strong></span>
                </div>`
              } else {
                const formattedValue = formatValue(param.value)
                result += `<div style="display: flex; align-items: center; gap: 6px; margin-top: 3px">
                  <span style="display: inline-block; width: 8px; height: 8px; background: ${param.color}; border-radius: 50%"></span>
                  <span>${param.seriesName}: <strong>${formattedValue}</strong></span>
                </div>`
              }
            }
          })

          return result
        }
      },
      legend: {
        type: 'scroll',
        orient: 'horizontal',
        bottom: hasMultipleDates ? 60 : 15,
        data: chartSeries.value.map(s => s?.name || '').filter(Boolean),
        textStyle: {
          fontSize: 12,
          color: '#4A5568'
        }
      },
      grid: {
        left: 80,
        right: 40,
        top: 80,
        bottom: hasMultipleDates ? 120 : 80,
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
        name: '数值',
        nameTextStyle: {
          fontSize: 12,
          color: '#718096'
        },
        axisLabel: {
          fontSize: 11,
          color: '#718096',
          formatter: (value: number) => {
            if (value >= 1000) {
              return `${(value / 1000).toFixed(1)}k`
            } else if (value >= 100) {
              return value.toFixed(0)
            } else {
              return value.toFixed(1)
            }
          }
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
        minInterval: 1,
        min: 0
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
    }
  } catch (error) {
    console.error('Error generating chart config:', error)
    return null
  }
}, null)

// 图表更新函数
async function updateChartData(): Promise<void> {
  if (isComponentUnmounted.value) return

  if (!shouldShowChart.value || !echartConfig.value || isUpdatingChart.value) {
    return
  }

  isUpdatingChart.value = true

  try {
    if (chartInstance.value) {
      // 🔥 关键：先清空图表，再设置新配置
      chartInstance.value.clear()
      chartInstance.value.setOption(echartConfig.value as EChartsOption, {
        notMerge: true,  // 👈 完全替换
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

// 图表初始化函数 - 修复类型问题
async function initializeChart(): Promise<void> {
  if (isComponentUnmounted.value) return

  if (!shouldShowChart.value || !echartConfig.value) {
    return
  }

  try {
    // 立即尝试初始化，不等待
    if (!chartRef.value) {
      await nextTick()
    }

    if (!chartRef.value) {
      console.warn('Chart container not found')
      return
    }

    const rect = chartRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
      // 延迟重试一次
      setTimeout(() => {
        if (!isComponentUnmounted.value) {
          initializeChart()
        }
      }, 50)
      return
    }

    // 销毁旧图表
    if (chartInstance.value) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }

    // 创建新图表 - 类型断言处理
    const instance = await initChart(echartConfig.value as EChartsOption)
    if (instance) {
      chartInstance.value = instance as EChartsType
      console.log('✅ 图表初始化成功')
    }
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// 创建快速防抖版本的图表更新函数
const quickUpdateChart = quickDebounce(async () => {
  if (!isComponentUnmounted.value) {
    await updateChartData()
  }
}, 100)

// 添加一个专门的防抖函数用于过滤更新
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

// 数据加载函数
async function loadData(): Promise<void> {
  if (isComponentUnmounted.value) return

  try {
    if (!query?.value?.startDate || !query?.value?.endDate) {
      showNotification('请选择有效的日期范围', 'error')
      return
    }

    errorMessage.value = ''

    console.log('🟢 开始加载健身数据', {
      startDate: query.value.startDate,
      endDate: query.value.endDate,
      typeIdList: query.value.typeIdList
    })

    // 使用更短的防抖时间
    loadAllRecordsDebounced(100)

    // 等待数据加载完成
    const unwatch = watch(
        () => isLoading.value,
        (newLoading) => {
          if (!newLoading) {
            // 保存所有加载的数据
            allLoadedRecords.value = [...fitnessRecords.value]
            unwatch()
          }
        }
    )
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      console.error('🔴 Failed to load fitness data:', error)
      errorMessage.value = '获取健身数据失败'
      showNotification('获取健身数据失败，请稍后重试', 'error')
    }
  }
}

// 添加处理查询条件更新的函数 - 防止重复刷新
let lastQueryUpdate = 0
async function handleQueryUpdate(newQuery: Partial<QueryType>) {
  // 防止短时间内重复更新
  const now = Date.now()
  if (now - lastQueryUpdate < 100) return
  lastQueryUpdate = now

  console.log('🔄 查询条件实时更新', newQuery)

  // 更新 store 中的查询条件
  updateQuery(newQuery)

  // 如果是日期范围变化，需要重新加载数据
  if (newQuery.startDate !== undefined || newQuery.endDate !== undefined) {
    isSearching.value = true
    await loadData()
  } else {
    // 其他条件变化只需要更新图表
    isFilterUpdating.value = true

    // 使用 nextTick 确保计算属性更新完成
    await nextTick()

    // 更新图表
    if (shouldShowChart.value) {
      await quickUpdateChart()
    }

    setTimeout(() => {
      isFilterUpdating.value = false
    }, 300)
  }
}

// 处理搜索事件
async function handleSearchFromComponent(newQuery?: any) {
  if (isComponentUnmounted.value) return

  try {
    console.log('🟢 处理搜索请求', newQuery)

    // 设置搜索状态
    isSearching.value = true

    let targetQuery = query?.value

    if (newQuery && typeof newQuery === 'object') {
      targetQuery = newQuery

      const safeQuery = createSafeQuery(targetQuery)

      // 判断是否需要重新加载数据
      const needReload = safeQuery.startDate !== query?.value?.startDate ||
          safeQuery.endDate !== query?.value?.endDate

      // 更新查询条件
      updateQuery(safeQuery)

      if (needReload) {
        // 日期变化需要重新加载数据
        await loadData()
      } else {
        // 其他条件变化只需要更新图表
        await debouncedFilterUpdate()
      }
    } else {
      // 没有传入参数，使用当前条件加载数据
      await loadData()
    }

  } catch (error) {
    console.error('❌ 处理搜索请求失败', error)
    showNotification('搜索失败，请重试', 'error')
  } finally {
    isSearching.value = false
  }
}

// 处理重置事件
async function handleResetFromComponent() {
  if (isComponentUnmounted.value) return

  try {
    console.log('🟢 处理重置请求')

    // 设置搜索状态
    isSearching.value = true

    // 重置store状态
    resetQuery()

    const defaultRange = getDefaultRange()
    const {startDate, endDate} = parseDateRange(defaultRange)

    const resetQueryData = createSafeQuery({
      typeIdList: [],
      remark: '',
      startDate: startDate || '',
      endDate: endDate || ''
    })

    updateQuery(resetQueryData)

    // 重置图表选项
    chartOptions.showDataLabels = false
    chartOptions.showAreaFill = true
    chartOptions.smoothCurve = true

    // 清除错误信息
    errorMessage.value = ''

    // 立即加载数据
    await loadData()

  } catch (error) {
    console.error('❌ 处理重置请求失败', error)
    showNotification('重置失败，请重试', 'error')
  } finally {
    isSearching.value = false
  }
}

// 安全的监听器包装
function safeWatch<T>(
    source: any,
    callback: (newVal: T, oldVal: T) => void,
    options?: any
): () => void {
  const stop = watch(source, (newVal: any, oldVal: any) => {
    if (!isComponentUnmounted.value) {
      try {
        callback(newVal as T, oldVal as T)
      } catch (error) {
        console.error('Watch callback error:', error)
      }
    }
  }, options)

  watchStoppers.push(stop)
  return stop
}

// 监听器
safeWatch(
    () => chartOptions,
    () => {
      console.log('🟢 图表选项改变，更新图表')
      if (shouldShowChart.value) {
        quickUpdateChart()
      }
    },
    { deep: true }
)

// 监听数据加载完成
safeWatch(
    () => isLoading.value,
    (newLoading, oldLoading) => {
      console.log('🟢 loading状态改变', { newLoading, oldLoading })

      if (oldLoading && !newLoading) {
        // 重置搜索状态
        isSearching.value = false

        // 保存所有加载的数据
        allLoadedRecords.value = [...fitnessRecords.value]

        // 设置有初始数据标志
        if (hasData.value) {
          hasInitialData.value = true
          console.log('📊 数据加载完成，准备更新图表')
          // 立即更新图表
          nextTick(() => {
            if (shouldShowChart.value && !isComponentUnmounted.value) {
              quickUpdateChart()
            }
          })
        }
      }
    }
)

// 添加监听器，监听非日期查询条件的变化 - 修复类型问题
safeWatch<{ typeIdList: number[], remark: string }>(
    () => ({
      typeIdList: [...(query?.value?.typeIdList || [])],
      remark: query?.value?.remark || ''
    }),
    (newVal, oldVal) => {
      // 跳过初始化和数据加载中的变化
      if (isLoading.value || !hasInitialData.value) return

      // 检查是否有实际变化
      const hasChange =
          JSON.stringify(newVal.typeIdList) !== JSON.stringify(oldVal.typeIdList) ||
          newVal.remark !== oldVal.remark

      if (hasChange) {
        console.log('🔄 过滤条件变化，实时更新图表')
        debouncedFilterUpdate()
      }
    },
    { deep: true }
)

// 生命周期
onMounted(async () => {
  try {
    console.log('🟢 组件挂载')
    isComponentUnmounted.value = false

    await nextTick()
    isChartReady.value = true

    if (!query?.value?.startDate || !query?.value?.endDate) {
      const defaultRange = getDefaultRange()
      const {startDate, endDate} = parseDateRange(defaultRange)

      const defaultQuery = createSafeQuery({
        typeIdList: [],
        remark: '',
        startDate: startDate || '',
        endDate: endDate || ''
      })

      updateQuery(defaultQuery)
    }

    // 立即加载数据
    loadData()

    // 添加窗口大小变化监听
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', resizeChart, {
        passive: true,
        capture: false
      })
    }
  } catch (error) {
    console.error('❌ 组件挂载错误', error)
    showNotification('组件初始化失败', 'error')
  }
})

onBeforeUnmount(() => {
  try {
    console.log('🟢 组件卸载')
    isComponentUnmounted.value = true

    // 停止所有监听器
    watchStoppers.forEach(stop => stop())
    watchStoppers.length = 0

    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', resizeChart)
    }

    if (chartInstance.value) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }

    destroyChart()
    cleanup() // 使用正确的方法调用

    // 清理缓存
    clearCommonMetaCache()
  } catch (error) {
    console.warn('Cleanup error:', error)
  }
})

// 额外的清理保险
onUnmounted(() => {
  isComponentUnmounted.value = true
})
</script>