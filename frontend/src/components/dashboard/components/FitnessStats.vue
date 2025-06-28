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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'
import { useFitnessStore } from '@/store/fitnessStore'
import { useChart, useDateRange } from '@/utils/common'
import type { EChartsOption } from 'echarts'
import type { EChartsType } from 'echarts/core'
import { FormattedFitnessRecord } from '@/types/fitness'
import type { Option } from '@/types/common'
import { clearCommonMetaCache } from "@/utils/commonMeta"

// Composables
import { useDebounce, useQuickDebounce } from '@/composables/fitness/dashboard/useDebounce'
import { useFitnessState } from '@/composables/fitness/dashboard/useFitnessState'
import { useFitnessQuery, QueryType } from '@/composables/fitness/dashboard/useFitnessQuery'
import { useFitnessData } from '@/composables/fitness/dashboard/useFitnessData'
import { useFitnessStats } from '@/composables/fitness/dashboard/useFitnessStats'
import { useFitnessChart } from '@/composables/fitness/dashboard/useFitnessChart'

// 为了兼容，创建别名
type FitnessRecord = FormattedFitnessRecord

// Props
const props = defineProps<{
  fitnessTypeOptions: Option[]
  unitOptions: Option[]
}>()

// Store
const fitnessStore = useFitnessStore()
const { query, allList, loadingState } = storeToRefs(fitnessStore)
const {
  loadAllRecordsDebounced,
  updateQuery,
  resetQuery,
  cleanup
} = fitnessStore

// Composables
const { getDefaultRange, parseDateRange } = useDateRange()
const { chartRef, initChart, destroyChart, resizeChart } = useChart()

// State from composables
const {
  errorMessage,
  isChartReady,
  isUpdatingChart,
  isSearching,
  hasInitialData,
  isFilterUpdating,
  isComponentUnmounted,
  showNotification
} = useFitnessState()

const { createSafeQuery, formatDateRange } = useFitnessQuery()

// 状态管理
const chartInstance = shallowRef<EChartsType | null>(null)
const allLoadedRecords = ref<FitnessRecord[]>([])

// 添加 watchers 的清理函数数组
const watchStoppers: Array<() => void> = []

// 使用正确的 loading 状态
const isLoading = computed(() => {
  if (isComponentUnmounted.value) return false
  return loadingState?.value?.list ?? false
})

// Data composables - 这里会初始化 safeComputed
const {
  safeComputed,
  filteredRecords,
  hasData,
  hasSearchConditions,
  allDates,
  formattedDates,
  effectiveTypeIds
} = useFitnessData({
  query,
  allLoadedRecords,
  fitnessTypeOptions: props.fitnessTypeOptions, // 先使用原始的 props
  isComponentUnmounted
})

// 修改 fitnessTypeOptions 定义，确保包含所有字段 - 移到 safeComputed 初始化之后
const fitnessTypeOptions = safeComputed(() => {
  if (!props.fitnessTypeOptions?.length) return []

  // 确保包含所有字段
  return props.fitnessTypeOptions.map(option => ({
    label: option.value1 || option.label || `类型${option.value}`,
    value: option.value || option.id || '',
    id: option.id || option.value,
    key1: option.key1,
    key2: option.key2,
    key3: option.key3,
    value1: option.value1
  }))
}, [])

// Stats composables
const {
  exerciseDaysCount,
  pushUpCount,
  proteinCount
} = useFitnessStats({
  filteredRecords,
  fitnessTypeOptions: fitnessTypeOptions.value, // 使用完整的类型选项
  safeComputed
})

const fitnessRecords = safeComputed<FitnessRecord[]>(() => {
  const records = Array.isArray(allList?.value) ? allList.value : []
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

// Chart composables
const { chartOptions, echartConfig } = useFitnessChart({
  filteredRecords,
  fitnessTypeOptions: fitnessTypeOptions.value, // 使用完整的类型选项
  unitOptions: props.unitOptions,
  query,
  allDates,
  formattedDates,
  effectiveTypeIds,
  dateRangeDisplay,
  hasData,
  safeComputed
})

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

// 图表更新函数
async function updateChartData(): Promise<void> {
  if (isComponentUnmounted.value) return

  if (!shouldShowChart.value || !echartConfig.value || isUpdatingChart.value) {
    return
  }

  isUpdatingChart.value = true

  try {
    if (chartInstance.value) {
      chartInstance.value.clear()
      chartInstance.value.setOption(echartConfig.value as EChartsOption, {
        notMerge: true,
        lazyUpdate: false
      })
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

// 图表初始化函数
async function initializeChart(): Promise<void> {
  if (isComponentUnmounted.value) return

  if (!shouldShowChart.value || !echartConfig.value) {
    return
  }

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
      setTimeout(() => {
        if (!isComponentUnmounted.value) {
          initializeChart()
        }
      }, 50)
      return
    }

    if (chartInstance.value) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }

    const instance = await initChart(echartConfig.value as EChartsOption)
    if (instance) {
      chartInstance.value = instance as EChartsType
    }
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// 创建快速防抖版本的图表更新函数
const quickUpdateChart = useQuickDebounce(async () => {
  if (!isComponentUnmounted.value) {
    await updateChartData()
  }
}, 100)

// 添加一个专门的防抖函数用于过滤更新
const debouncedFilterUpdate = useDebounce(async () => {
  isFilterUpdating.value = true

  await nextTick()

  if (shouldShowChart.value) {
    quickUpdateChart()
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
    loadAllRecordsDebounced(100)

    const unwatch = watch(
        () => isLoading.value,
        (newLoading) => {
          if (!newLoading) {
            allLoadedRecords.value = [...fitnessRecords.value]
            unwatch()
          }
        }
    )
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      console.error('Failed to load fitness data:', error)
      errorMessage.value = '获取健身数据失败'
      showNotification('获取健身数据失败，请稍后重试', 'error')
    }
  }
}

// 处理查询条件更新的函数
let lastQueryUpdate = 0
async function handleQueryUpdate(newQuery: Partial<QueryType>) {
  const now = Date.now()
  if (now - lastQueryUpdate < 100) return
  lastQueryUpdate = now

  updateQuery(newQuery)

  if (newQuery.startDate !== undefined || newQuery.endDate !== undefined) {
    isSearching.value = true
    await loadData()
  } else {
    isFilterUpdating.value = true

    await nextTick()

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
    isSearching.value = true

    let targetQuery = query?.value

    if (newQuery && typeof newQuery === 'object') {
      targetQuery = newQuery

      const safeQuery = createSafeQuery(targetQuery)

      const needReload = safeQuery.startDate !== query?.value?.startDate ||
          safeQuery.endDate !== query?.value?.endDate

      updateQuery(safeQuery)

      if (needReload) {
        await loadData()
      } else {
        await debouncedFilterUpdate()
      }
    } else {
      await loadData()
    }

  } catch (error) {
    console.error('处理搜索请求失败', error)
    showNotification('搜索失败，请重试', 'error')
  } finally {
    isSearching.value = false
  }
}

// 处理重置事件
async function handleResetFromComponent() {
  if (isComponentUnmounted.value) return

  try {
    isSearching.value = true

    resetQuery()

    const defaultRange = getDefaultRange()
    const { startDate, endDate } = parseDateRange(defaultRange)

    const resetQueryData = createSafeQuery({
      typeIdList: [],
      remark: '',
      startDate: startDate || '',
      endDate: endDate || ''
    })

    updateQuery(resetQueryData)

    chartOptions.showDataLabels = false
    chartOptions.showAreaFill = true
    chartOptions.smoothCurve = true

    errorMessage.value = ''

    await loadData()

  } catch (error) {
    console.error('处理重置请求失败', error)
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
      if (shouldShowChart.value) {
        quickUpdateChart()
      }
    },
    { deep: true }
)

safeWatch(
    () => isLoading.value,
    (newLoading, oldLoading) => {
      if (oldLoading && !newLoading) {
        isSearching.value = false
        allLoadedRecords.value = [...fitnessRecords.value]

        if (hasData.value) {
          hasInitialData.value = true
          nextTick(() => {
            if (shouldShowChart.value && !isComponentUnmounted.value) {
              quickUpdateChart()
            }
          })
        }
      }
    }
)

safeWatch<{ typeIdList: number[], remark: string }>(
    () => ({
      typeIdList: [...(query?.value?.typeIdList || [])],
      remark: query?.value?.remark || ''
    }),
    (newVal, oldVal) => {
      if (isLoading.value || !hasInitialData.value) return

      const hasChange =
          JSON.stringify(newVal.typeIdList) !== JSON.stringify(oldVal.typeIdList) ||
          newVal.remark !== oldVal.remark

      if (hasChange) {
        debouncedFilterUpdate()
      }
    },
    { deep: true }
)

// 生命周期
onMounted(async () => {
  try {
    isComponentUnmounted.value = false

    await nextTick()
    isChartReady.value = true

    if (!query?.value?.startDate || !query?.value?.endDate) {
      const defaultRange = getDefaultRange()
      const { startDate, endDate } = parseDateRange(defaultRange)

      const defaultQuery = createSafeQuery({
        typeIdList: [],
        remark: '',
        startDate: startDate || '',
        endDate: endDate || ''
      })

      updateQuery(defaultQuery)
    }

    loadData()

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', resizeChart, {
        passive: true,
        capture: false
      })
    }
  } catch (error) {
    console.error('组件挂载错误', error)
    showNotification('组件初始化失败', 'error')
  }
})

onBeforeUnmount(() => {
  try {
    isComponentUnmounted.value = true

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
    cleanup()

    clearCommonMetaCache()
  } catch (error) {
    console.warn('Cleanup error:', error)
  }
})

onUnmounted(() => {
  isComponentUnmounted.value = true
})
</script>