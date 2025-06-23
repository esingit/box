<!--src/components/dashboard/components/FitnessStats.vue-->
<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-4">
    <h2 class="text-lg font-semibold">健身统计</h2>

    <!-- 使用健身查询组件替换原有查询条件 -->
    <FitnessSearch
        :query="query"
        :fitness-type-options="fitnessTypeOptions"
        :result-count="fitnessRecords.length"
        @search="handleSearchFromComponent"
        @reset="handleResetFromComponent"
    />

    <!-- 图表显示选项 -->
    <div v-if="hasData" class="border rounded-xl p-4">
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
    <div v-if="hasData" class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
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
      <!-- 加载状态 -->
      <div v-if="isLoading" class="flex items-center justify-center h-full text-gray-400">
        <div class="flex items-center gap-2">
          <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
          <span>加载健身数据中...</span>
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
      <div v-else-if="!hasData" class="h-full">
        <BaseEmptyState
            icon="Dumbbell"
            message="暂无健身数据"
            :description="emptyStateDescription"
        />
      </div>

      <!-- 图表容器 -->
      <div v-else ref="chartRef" class="w-full h-full chart-container"></div>
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

interface FitnessRecord {
  id: string
  typeId: number | string
  count: string | number
  unitId: number | string
  finishTime: string
  remark?: string
}

// 常量
const EXERCISE_TYPE_KEY = 'EXERCISE'

// 定义 Option 接口
interface Option {
  label: string
  value: string | number
  id?: string | number
  value1?: string
  key1?: string
  key2?: string
  key3?: string
}

// Props 定义
const props = defineProps<{
  fitnessTypeOptions: Option[]
  unitOptions: Option[]
}>()

// Composables
const fitnessStore = useFitnessStore()
const {query, allList, loadingList} = storeToRefs(fitnessStore)

const {
  getDefaultRange,
  parseDateRange
} = useDateRange()

const {
  chartRef,
  initChart,
  destroyChart,
  resizeChart
} = useChart()

// 🔥 使用 store 中的 loading 状态
const isLoading = computed(() => loadingList.value)
const errorMessage = ref('')
const isChartReady = ref(false)

// 图表选项
const chartOptions = reactive({
  showDataLabels: false,
  showAreaFill: true,
  smoothCurve: true
})

// 低饱和度颜色方案
const CHART_COLORS = [
  '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E',
  '#7B9E9E', '#B8936B', '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8',
  '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B', '#9E7B8C', '#7B8C9E'
]

// 防抖函数
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

// 转换 fitnessTypeOptions 格式以适配 FitnessSearch 组件
const fitnessTypeOptions = computed(() => {
  if (!props.fitnessTypeOptions || !Array.isArray(props.fitnessTypeOptions)) {
    return []
  }

  return props.fitnessTypeOptions.map(option => ({
    label: option.value1 || option.label || `类型${option.value}`,
    value: option.value || option.id
  }))
})

// 格式化日期范围显示
function formatDateRange(startDate: string, endDate: string): string {
  if (!startDate || !endDate) {
    return ''
  }

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const start = formatDate(startDate)
  const end = formatDate(endDate)

  if (start === end) {
    return start
  }

  return `${start} ~ ${end}`
}

// 创建单位映射
const unitMapping = computed(() => {
  const map: Record<string, string> = {}
  if (!props.unitOptions || !Array.isArray(props.unitOptions)) {
    return map
  }

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

// 获取健身类型的默认单位
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

// 判断健身类型是否为运动类型
function isExerciseType(typeId: string | number): boolean {
  const fitnessType = props.fitnessTypeOptions?.find(type =>
      String(type.value) === String(typeId) || String(type.id) === String(typeId)
  )
  return fitnessType?.key2 === EXERCISE_TYPE_KEY
}

// 计算属性
const fitnessRecords = computed<FitnessRecord[]>(() => {
  const list = fitnessStore.allList
  if (!list || !Array.isArray(list)) {
    return []
  }
  return list as FitnessRecord[]
})

const hasData = computed(() => {
  return fitnessRecords.value.length > 0 && !errorMessage.value
})

const hasSearchConditions = computed(() => {
  return query.value.typeIdList.length > 0 || query.value.remark.trim() !== ''
})

const dateRangeDisplay = computed(() => {
  return formatDateRange(query.value.startDate, query.value.endDate)
})

const emptyStateDescription = computed(() => {
  if (!query.value.startDate || !query.value.endDate) {
    return '请选择日期范围查看健身数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到健身记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无健身记录，开始您的健身之旅吧！`
})

const effectiveTypeIds = computed(() => {
  if (!props.fitnessTypeOptions || !Array.isArray(props.fitnessTypeOptions)) {
    return []
  }

  return query.value.typeIdList.length > 0
      ? query.value.typeIdList
      : props.fitnessTypeOptions.map(item => item.value || item.id)
})

// 获取运动类型的健身记录
const exerciseRecords = computed(() => {
  return fitnessRecords.value.filter(record => {
    return isExerciseType(record.typeId)
  })
})

// 获取运动类型的健身天数
const exerciseDaysCount = computed(() => {
  if (!exerciseRecords.value.length) return 0

  const exerciseDays = new Set<string>()
  exerciseRecords.value.forEach(record => {
    if (record.finishTime) {
      const date = record.finishTime.split('T')[0]
      exerciseDays.add(date)
    }
  })

  return exerciseDays.size
})

// 获取所有日期并排序
const allDates = computed(() => {
  const dateSet = new Set<string>()

  if (!fitnessRecords.value || !Array.isArray(fitnessRecords.value)) {
    return []
  }

  fitnessRecords.value.forEach(record => {
    if (record && record.finishTime) {
      const date = record.finishTime.split('T')[0]
      if (date) {
        dateSet.add(date)
      }
    }
  })
  return Array.from(dateSet).sort()
})

// 格式化日期显示
const formattedDates = computed(() => {
  if (!allDates.value || !Array.isArray(allDates.value)) {
    return []
  }

  return allDates.value.map(date => {
    const [year, month, day] = date.split('-')
    return `${month}/${day}`
  })
})

// 获取指定类型和日期的记录单位
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

// 格式化数值显示（带单位）
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

// 单独的数值格式化函数，不带单位
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

// 生成图表系列数据
const chartSeries = computed(() => {
  if (!hasData.value || !allDates.value.length) return []

  try {
    return effectiveTypeIds.value
        .map((typeId, index) => {
          const typeOption = props.fitnessTypeOptions?.find(item =>
              String(item.value) === String(typeId) || String(item.id) === String(typeId)
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
            typeId: typeId,
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
                if (typeId !== undefined) {
                  return formatValueWithUnit(value, typeId, date)
                }
                return formatValue(value)
              }
            } : undefined,
            emphasis: {
              focus: 'series',
              scale: true
            }
          }
        })
        .filter(Boolean) as any[]
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
              const series = chartSeries.value.find(s => s.name === param.seriesName)
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
        data: chartSeries.value.map(s => s.name),
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
        axiosLabel: {
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

// 通知函数
function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', {message, type})
}

// 🔥 优化后的图表初始化
async function initializeChart(): Promise<void> {
  if (!isChartReady.value || !hasData.value || !echartConfig.value) {
    return
  }

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

    await initChart(echartConfig.value as EChartsOption)
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// 创建防抖版本的图表更新函数
const debouncedUpdateChart = debounce(async () => {
  if (hasData.value && echartConfig.value && !isLoading.value && isChartReady.value) {
    await initializeChart()
  }
}, 200)

// 🔥 优化数据加载，使用 store 的防抖方法
async function loadData(): Promise<void> {
  if (!query.value.startDate || !query.value.endDate) {
    showNotification('请选择有效的日期范围', 'error')
    return
  }

  errorMessage.value = ''

  try {
    console.log('🟢 开始加载健身数据')

    // 🔥 使用 store 中的防抖方法
    fitnessStore.loadAllRecordsDebounced(300)

    // 🔥 监听数据加载完成
    await nextTick()

    // 等待数据加载完成
    const checkDataLoaded = () => {
      return new Promise<void>((resolve) => {
        const checkInterval = setInterval(() => {
          if (!isLoading.value) {
            clearInterval(checkInterval)
            resolve()
          }
        }, 100)

        // 5秒超时
        setTimeout(() => {
          clearInterval(checkInterval)
          resolve()
        }, 5000)
      })
    }

    await checkDataLoaded()

    if (hasData.value) {
      showNotification('健身数据加载成功', 'success')
      setTimeout(async () => {
        await initializeChart()
      }, 150)
    }
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      console.error('🔴 Failed to load fitness data:', error)
      errorMessage.value = '获取健身数据失败'
      showNotification('获取健身数据失败，请稍后重试', 'error')
    }
  }
}

// 处理搜索事件
async function handleSearchFromComponent(newQuery: typeof query.value) {
  console.log('🟢 处理搜索请求', newQuery)
  fitnessStore.updateQuery(newQuery)

  // 🔥 使用防抖版本避免频繁请求
  fitnessStore.loadAllRecordsDebounced(300)
}

async function handleResetFromComponent() {
  fitnessStore.resetQuery()

  const defaultRange = getDefaultRange()
  const {startDate, endDate} = parseDateRange(defaultRange)
  fitnessStore.updateQuery({
    ...fitnessStore.query,
    typeIdList: [],
    remark: '',
    startDate,
    endDate,
  })

  chartOptions.showDataLabels = false
  chartOptions.showAreaFill = true
  chartOptions.smoothCurve = true
  errorMessage.value = ''

  // 🔥 使用防抖版本
  fitnessStore.loadAllRecordsDebounced(300)
}

// 计算俯卧撑总数
const pushUpCount = computed(() => {
  if (!fitnessRecords.value?.length || !props.fitnessTypeOptions?.length) {
    return 0
  }

  return fitnessRecords.value.filter(record => {
    if (!record || !record.typeId) return false

    const fitnessType = props.fitnessTypeOptions.find(type =>
        String(type.value) === String(record.typeId) || String(type.id) === String(record.typeId)
    )

    return fitnessType?.key1 === 'PUSH_UP'
  }).reduce((sum, record) => {
    const count = Number(record.count || 0)
    return sum + (isNaN(count) ? 0 : count)
  }, 0)
})

// 计算蛋白质总量
const proteinCount = computed(() => {
  if (!fitnessRecords.value?.length || !props.fitnessTypeOptions?.length) {
    return 0
  }

  return fitnessRecords.value.filter(record => {
    if (!record || !record.typeId) return false

    const fitnessType = props.fitnessTypeOptions.find(type =>
        String(type.value) === String(record.typeId) || String(type.id) === String(record.typeId)
    )

    return fitnessType?.key1 === 'PROTEIN' ||
        fitnessType?.value1?.includes('蛋白')
  }).reduce((sum, record) => {
    const count = Number(record.count || 0)
    return sum + (isNaN(count) ? 0 : count)
  }, 0)
})

// 🔥 生命周期优化
onMounted(async () => {
  await nextTick()
  isChartReady.value = true

  if (!query.value.startDate || !query.value.endDate) {
    const defaultRange = getDefaultRange()
    const {startDate, endDate} = parseDateRange(defaultRange)
    fitnessStore.updateQuery({
      startDate,
      endDate
    })
  }

  // 🔥 延迟加载数据，确保组件完全初始化
  setTimeout(() => {
    loadData()
  }, 100)

  // 🔥 修复: 添加 passive 选项和错误处理
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', resizeChart, {
      passive: true,
      capture: false
    })
  }
})

// 监听器优化
watch(
    () => [chartOptions.showDataLabels, chartOptions.showAreaFill, chartOptions.smoothCurve],
    () => {
      debouncedUpdateChart()
    },
    {deep: true}
)

watch(
    () => [fitnessStore.allList, hasData.value],
    async ([newList, newHasData], [oldList, oldHasData]) => {
      if (isChartReady.value && !isLoading.value) {
        if (newHasData && (!oldHasData || newList !== oldList)) {
          await nextTick()
          debouncedUpdateChart()
        }
      }
    },
    {deep: true}
)

watch(chartRef, async (newRef, oldRef) => {
  if (newRef && newRef !== oldRef && isChartReady.value && hasData.value && !isLoading.value) {
    await nextTick()
    setTimeout(() => {
      debouncedUpdateChart()
    }, 100)
  }
})

// 🔥 优化清理函数
onBeforeUnmount(() => {
  try {
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