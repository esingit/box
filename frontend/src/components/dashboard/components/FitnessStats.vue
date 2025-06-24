<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-4">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <FitnessSearch
        :query="query"
        :fitness-type-options="fitnessTypeOptions"
        :result-count="fitnessRecords.length"
        @search="handleSearchFromComponent"
        @reset="handleResetFromComponent"
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
      <!-- 加载状态 - 优先级最高 -->
      <div v-if="showLoading" class="flex items-center justify-center h-full text-gray-400">
        <div class="flex items-center gap-2">
          <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
          <span>{{ loadingText }}</span>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="errorMessage" class="h-full">
        <BaseEmptyState
            icon="Dumbbell"
            :message="errorMessage"
            description="请检查网络连接或稍后重试"
        />
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="showEmptyState" class="h-full">
        <BaseEmptyState
            icon="Dumbbell"
            message="暂无健身数据"
            :description="emptyStateDescription"
        />
      </div>

      <!-- 图表容器 -->
      <div v-else-if="shouldShowChart" :key="chartKey" ref="chartRef" class="w-full h-full chart-container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'
import {storeToRefs} from 'pinia'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'
import {useFitnessStore} from '@/store/fitnessStore'
import {useChart, useDateRange} from '@/utils/common'
import emitter from '@/utils/eventBus'
import type {EChartsOption} from 'echarts'

// 类型定义
interface FitnessRecord {
  id: string
  typeId: number | string
  count: string | number
  unitId: number | string
  finishTime: string
  remark?: string
}

interface Option {
  label: string
  value: string | number
  id?: string | number
  value1?: string
  key1?: string
  key2?: string
  key3?: string
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
const {query, allList, loadingList} = storeToRefs(fitnessStore)

// Composables
const {getDefaultRange, parseDateRange} = useDateRange()
const {chartRef, initChart, destroyChart, resizeChart} = useChart()

// 状态管理
const isLoading = computed(() => loadingList.value)
const errorMessage = ref('')
const isChartReady = ref(false)
const chartKey = ref(0)
const isUpdatingChart = ref(false)
const isSearching = ref(false) // 新增：搜索状态
const hasInitialData = ref(false) // 新增：是否有初始数据

// 图表选项
const chartOptions = reactive({
  showDataLabels: false,
  showAreaFill: true,
  smoothCurve: true
})

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

function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', {message, type})
}

// 验证和创建安全的查询对象
function createSafeQuery(inputQuery: any) {
  const defaultQuery = {
    typeIdList: [],
    startDate: '',
    endDate: '',
    remark: ''
  }

  if (!inputQuery || typeof inputQuery !== 'object') {
    console.warn('⚠️ 无效的查询参数，使用默认值', inputQuery)
    return defaultQuery
  }

  return {
    typeIdList: Array.isArray(inputQuery.typeIdList) ? inputQuery.typeIdList : [],
    startDate: typeof inputQuery.startDate === 'string' ? inputQuery.startDate : '',
    endDate: typeof inputQuery.endDate === 'string' ? inputQuery.endDate : '',
    remark: typeof inputQuery.remark === 'string' ? inputQuery.remark : '',
    ...Object.fromEntries(
        Object.entries(inputQuery).filter(([key, value]) =>
            !['typeIdList', 'startDate', 'endDate', 'remark'].includes(key) &&
            value !== undefined &&
            value !== null
        )
    )
  }
}

// 计算属性 - 数据相关
const fitnessTypeOptions = computed(() => {
  if (!props.fitnessTypeOptions?.length) return []

  return props.fitnessTypeOptions.map(option => ({
    label: option.value1 || option.label || `类型${option.value}`,
    value: option.value || option.id || ''
  }))
})

const fitnessRecords = computed<FitnessRecord[]>(() => {
  return Array.isArray(fitnessStore.allList) ? fitnessStore.allList : []
})

const hasData = computed(() => {
  return fitnessRecords.value.length > 0
})

const hasSearchConditions = computed(() => {
  return query.value?.typeIdList?.length > 0 || (query.value?.remark || '').trim() !== ''
})

const dateRangeDisplay = computed(() => {
  return formatDateRange(query.value?.startDate || '', query.value?.endDate || '')
})

const emptyStateDescription = computed(() => {
  if (!query.value?.startDate || !query.value?.endDate) {
    return '请选择日期范围查看健身数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到健身记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无健身记录，开始您的健身之旅吧！`
})

// 计算属性 - 显示控制
const showLoading = computed(() => {
  // 正在加载中，或者正在搜索且没有初始数据
  return isLoading.value || (isSearching.value && !hasInitialData.value)
})

const loadingText = computed(() => {
  if (isSearching.value) {
    return '查询健身数据中...'
  }
  return '加载健身数据中...'
})

const showEmptyState = computed(() => {
  // 不在加载中，没有错误，没有数据，且查询条件有效
  return !showLoading.value &&
      !errorMessage.value &&
      !hasData.value &&
      query.value?.startDate &&
      query.value?.endDate
})

const shouldShowChart = computed(() => {
  // 不在加载中，没有错误，有数据，图表已准备好
  return !showLoading.value &&
      !errorMessage.value &&
      hasData.value &&
      isChartReady.value
})

const shouldShowOptions = computed(() => {
  // 有数据或者有初始数据时显示选项
  return hasData.value || hasInitialData.value
})

const shouldShowStats = computed(() => {
  // 有数据或者有初始数据时显示统计
  return hasData.value || hasInitialData.value
})

const effectiveTypeIds = computed(() => {
  if (!props.fitnessTypeOptions?.length) return []

  return query.value?.typeIdList?.length > 0
      ? query.value.typeIdList
      : props.fitnessTypeOptions.map(item => item.value || item.id).filter(Boolean)
})

const allDates = computed(() => {
  const dateSet = new Set<string>()

  fitnessRecords.value.forEach(record => {
    if (record?.finishTime) {
      const date = record.finishTime.split('T')[0]
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

// 统计相关计算属性
const exerciseDaysCount = computed(() => {
  if (!fitnessRecords.value.length) return 0

  const exerciseDays = new Set<string>()
  fitnessRecords.value
      .filter(record => isExerciseType(record.typeId))
      .forEach(record => {
        if (record.finishTime) {
          exerciseDays.add(record.finishTime.split('T')[0])
        }
      })

  return exerciseDays.size
})

const pushUpCount = computed(() => {
  if (!fitnessRecords.value.length || !props.fitnessTypeOptions?.length) return 0

  return fitnessRecords.value
      .filter(record => {
        const fitnessType = props.fitnessTypeOptions?.find(type =>
            String(type.value) === String(record.typeId) ||
            String(type.id) === String(record.typeId)
        )
        return fitnessType?.key1 === 'PUSH_UP'
      })
      .reduce((sum, record) => {
        const count = Number(record.count || 0)
        return sum + (isNaN(count) ? 0 : count)
      }, 0)
})

const proteinCount = computed(() => {
  if (!fitnessRecords.value.length || !props.fitnessTypeOptions?.length) return 0

  return fitnessRecords.value
      .filter(record => {
        const fitnessType = props.fitnessTypeOptions?.find(type =>
            String(type.value) === String(record.typeId) ||
            String(type.id) === String(record.typeId)
        )
        return fitnessType?.key1 === 'PROTEIN' ||
            fitnessType?.value1?.includes('蛋白')
      })
      .reduce((sum, record) => {
        const count = Number(record.count || 0)
        return sum + (isNaN(count) ? 0 : count)
      }, 0)
})

// 单位映射
const unitMapping = computed(() => {
  const map: Record<string, string> = {}
  if (!props.unitOptions?.length) return map

  props.unitOptions.forEach(option => {
    if (option) {
      if (option.id && option.value1) {
        map[String(option.id)] = option.value1
      }
      if (option.value && option.value1) {
        map[String(option.value)] = option.value1
      }
    }
  })
  return map
})

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

function isExerciseType(typeId: string | number): boolean {
  const fitnessType = props.fitnessTypeOptions?.find(type =>
      String(type.value) === String(typeId) ||
      String(type.id) === String(typeId)
  )
  return fitnessType?.key2 === EXERCISE_TYPE_KEY
}

function getRecordUnit(typeId: string | number, date: string): string {
  const records = fitnessRecords.value.filter(record =>
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

// 图表配置生成
const chartSeries = computed(() => {
  if (!hasData.value || !allDates.value.length) return []

  try {
    return effectiveTypeIds.value
        .map((typeId, index) => {
          if (!typeId) return null

          const typeOption = props.fitnessTypeOptions?.find(item =>
              String(item.value) === String(typeId) ||
              String(item.id) === String(typeId)
          )
          const typeName = typeOption?.value1 || typeOption?.label || `类型${typeId}`

          const data = allDates.value.map(date => {
            return fitnessRecords.value
                .filter(record =>
                    record &&
                    String(record.typeId) === String(typeId) &&
                    record.finishTime?.startsWith(date)
                )
                .reduce((sum, record) => sum + Number(record.count || 0), 0)
          })

          if (!data.some(value => value > 0)) return null

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
})

// ECharts 配置
const echartConfig = computed(() => {
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
      animationDuration: 1000,
      animationEasing: 'cubicOut'
    }
  } catch (error) {
    console.error('Error generating chart config:', error)
    return null
  }
})

// 图表初始化函数
async function initializeChart(): Promise<void> {
  if (!shouldShowChart.value || !echartConfig.value || isUpdatingChart.value) {
    return
  }

  isUpdatingChart.value = true

  try {
    await nextTick()

    let retryCount = 0
    const maxRetries = 10
    const retryDelay = 50

    while (retryCount < maxRetries) {
      if (chartRef.value) {
        break
      }
      await new Promise(resolve => setTimeout(resolve, retryDelay))
      retryCount++
    }

    if (!chartRef.value) {
      console.warn('Chart container not found after waiting')
      return
    }

    const rect = chartRef.value.getBoundingClientRect()
    if (rect.width === 0 || rect.height === 0) {
      console.warn('Chart container has no size')
      return
    }

    // 销毁旧图表
    destroyChart()

    // 创建新图表
    await initChart(echartConfig.value as EChartsOption)

    console.log('✅ 图表初始化成功')
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  } finally {
    isUpdatingChart.value = false
  }
}

// 创建防抖版本的图表更新函数
const debouncedUpdateChart = debounce(async () => {
  if (shouldShowChart.value && echartConfig.value && !isUpdatingChart.value) {
    // 只有在确实需要更新时才改变chartKey
    chartKey.value++
    await nextTick()
    setTimeout(async () => {
      await initializeChart()
    })
  }
})

// 数据加载函数
async function loadData(): Promise<void> {
  try {
    if (!query.value?.startDate || !query.value?.endDate) {
      showNotification('请选择有效的日期范围', 'error')
      return
    }

    errorMessage.value = ''

    console.log('🟢 开始加载健身数据', {
      startDate: query.value.startDate,
      endDate: query.value.endDate,
      typeIdList: query.value.typeIdList
    })

    // 使用 store 中的防抖方法
    fitnessStore.loadAllRecordsDebounced(300)
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      console.error('🔴 Failed to load fitness data:', error)
      errorMessage.value = '获取健身数据失败'
      showNotification('获取健身数据失败，请稍后重试', 'error')
    }
  }
}

// 处理搜索事件
async function handleSearchFromComponent(newQuery?: any) {
  try {
    console.log('🟢 处理搜索请求', {
      newQuery,
      type: typeof newQuery,
      currentQuery: query.value
    })

    // 设置搜索状态
    isSearching.value = true

    // 如果没有传递参数或参数无效，直接使用当前的查询条件进行搜索
    let targetQuery = query.value

    if (newQuery && typeof newQuery === 'object') {
      targetQuery = newQuery
    } else {
      console.log('🔍 使用当前查询条件进行搜索')
    }

    // 创建安全的查询对象
    const safeQuery = createSafeQuery(targetQuery)
    console.log('🟢 验证后的查询参数', safeQuery)

    // 检查查询条件是否真的发生了变化（仅当传递了新查询时才检查）
    if (newQuery && typeof newQuery === 'object') {
      const currentQueryStr = JSON.stringify(query.value)
      const newQueryStr = JSON.stringify(safeQuery)

      if (currentQueryStr === newQueryStr) {
        console.log('ℹ️ 查询条件未变化，跳过更新')
        isSearching.value = false
        return
      }

      // 更新查询条件
      fitnessStore.updateQuery(safeQuery)
    }

    // 清除错误信息
    errorMessage.value = ''

    // 加载数据
    fitnessStore.loadAllRecordsDebounced(300)

  } catch (error) {
    console.error('❌ 处理搜索请求失败', error)
    showNotification('搜索失败，请重试', 'error')
    isSearching.value = false
  }
}

// 处理重置事件
async function handleResetFromComponent() {
  try {
    console.log('🟢 处理重置请求')

    // 设置搜索状态
    isSearching.value = true

    // 重置store状态
    fitnessStore.resetQuery()

    const defaultRange = getDefaultRange()
    const {startDate, endDate} = parseDateRange(defaultRange)

    // 创建安全的重置查询对象
    const resetQuery = createSafeQuery({
      typeIdList: [],
      remark: '',
      startDate: startDate || '',
      endDate: endDate || ''
    })

    console.log('🟢 重置查询参数', resetQuery)

    fitnessStore.updateQuery(resetQuery)

    // 重置图表选项
    chartOptions.showDataLabels = false
    chartOptions.showAreaFill = true
    chartOptions.smoothCurve = true

    // 清除错误信息
    errorMessage.value = ''

    // 加载数据
    fitnessStore.loadAllRecordsDebounced(300)

  } catch (error) {
    console.error('❌ 处理重置请求失败', error)
    showNotification('重置失败，请重试', 'error')
    isSearching.value = false
  }
}

// 监听器优化
watch(
    () => chartOptions,
    () => {
      try {
        console.log('🟢 图表选项改变，更新图表')
        if (shouldShowChart.value) {
          debouncedUpdateChart()
        }
      } catch (error) {
        console.error('❌ 图表选项监听错误', error)
      }
    },
    { deep: true }
)

// 监听数据加载完成
watch(
    () => isLoading.value,
    (newLoading, oldLoading) => {
      try {
        console.log('🟢 loading状态改变', { newLoading, oldLoading })

        // 当loading从true变为false时，说明数据加载完成
        if (oldLoading && !newLoading) {
          // 重置搜索状态
          isSearching.value = false

          // 设置有初始数据标志
          if (hasData.value) {
            hasInitialData.value = true
            console.log('📊 数据加载完成，准备更新图表')
            // 延迟更新图表，确保DOM完全渲染
            setTimeout(() => {
              if (shouldShowChart.value) {
                debouncedUpdateChart()
              }
            })
          }
        }
      } catch (error) {
        console.error('❌ loading状态监听错误', error)
      }
    }
)

// 生命周期
onMounted(async () => {
  try {
    console.log('🟢 组件挂载')

    await nextTick()
    isChartReady.value = true

    if (!query.value?.startDate || !query.value?.endDate) {
      const defaultRange = getDefaultRange()
      const {startDate, endDate} = parseDateRange(defaultRange)

      const defaultQuery = createSafeQuery({
        typeIdList: [],
        remark: '',
        startDate: startDate || '',
        endDate: endDate || ''
      })

      fitnessStore.updateQuery(defaultQuery)
    }

    // 延迟加载数据，确保组件完全初始化
    setTimeout(() => {
      loadData()
    })

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

    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', resizeChart)
    }
    destroyChart()
    fitnessStore.cleanup()
  } catch (error) {
    console.warn('Cleanup error:', error)
  }
})
</script>