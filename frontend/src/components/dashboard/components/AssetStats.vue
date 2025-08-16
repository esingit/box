<template>
  <div class="bg-white rounded-xl p-6 hover:shadow-md w-full space-y-4">
    <h2 class="text-lg font-semibold mb-4">资产统计</h2>

    <!-- 使用 AssetSearch 组件 -->
    <AssetSearch
        :query="query"
        :asset-name-options="processedAssetNameOptions"
        :asset-type-options="processedAssetTypeOptions"
        :asset-location-options="processedAssetLocationOptions"
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
          <input type="checkbox" v-model="chartOptions[option.key]" class="rounded"/>
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
        <div v-if="isFilterUpdating && !showLoading"
             class="absolute top-2 right-2 bg-blue-100 text-blue-700 px-3 py-1 rounded-lg text-sm z-10">
          更新中...
        </div>
      </transition>

      <!-- 错误状态 -->
      <div v-if="errorMessage && !showLoading" class="h-full">
        <BaseEmptyState icon="Wallet" :message="errorMessage" description="请检查网络连接或稍后重试"/>
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="showEmptyState && !showLoading" class="h-full">
        <BaseEmptyState icon="Wallet" message="暂无资产数据" :description="emptyStateDescription"/>
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
import {computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch} from 'vue'
import {storeToRefs} from 'pinia'
import type {EChartsType} from 'echarts/core'
import AssetSearch from '@/components/asset/AssetSearch.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import {useAssetStore} from '@/store/assetStore'
import {useMetaStore} from '@/store/metaStore'
import {useChart, useDateRange} from '@/utils/common'
import type {AssetRecord, AssetQueryConditions} from '@/types/asset'
import type {Option} from '@/types/common'
import {clearCommonMetaCache} from "@/utils/commonMeta"

// Composables
import {CHART_OPTIONS_CONFIG} from '@/composables/asset/useAssetConstants'
import {useAssetState} from '@/composables/asset/useAssetState'
import {useAssetUtils} from '@/composables/asset/useAssetUtils'
import {useAssetData} from '@/composables/asset/useAssetData'
import {useAssetStats} from '@/composables/asset/useAssetStats'
import {useAssetChart} from '@/composables/asset/useAssetChart'

// Props
const props = defineProps<{
  assetNameOptions: Option[]
  assetTypeOptions: Option[]
  assetLocationOptions: Option[]
  unitOptions: Option[]
}>()

// Store & Composables
const assetStore = useAssetStore()
const metaStore = useMetaStore()

const {query} = storeToRefs(assetStore)
const { allList } = storeToRefs(assetStore)
const {getDefaultRange, parseDateRange} = useDateRange()
const {chartRef, initChart, destroyChart, resizeChart} = useChart()

// State from composables
const {
  isLoading,
  errorMessage,
  isChartReady,
  isUpdatingChart,
  isSearching,
  hasInitialData,
  isFilterUpdating,
  showNotification,
  chartOptions,
  saveChartOptions
} = useAssetState()

const {debounce} = useAssetUtils()

// 状态管理
const chartInstance = shallowRef<EChartsType | null>(null)
const allLoadedRecords = ref<AssetRecord[]>([])

// 自定义映射函数，确保包含所有字段
function mapMetaToOptions(metaList: any[]): Option[] {
  return metaList.map(meta => ({
    label: meta.value1 || meta.label || String(meta.value),
    value: meta.id,
    id: meta.id,
    key1: meta.key1,
    key2: meta.key2,
    key3: meta.key3,
    key4: meta.key4,
    value1: meta.value1,
    value2: meta.value2,
    value3: meta.value3,
    value4: meta.value4
  }))
}

// 修改这些计算属性，确保包含所有字段
const assetTypeOptions = computed(() =>
    mapMetaToOptions(metaStore.typeMap?.ASSET_TYPE || [])
)

const assetLocationOptions = computed(() =>
    mapMetaToOptions(metaStore.typeMap?.ASSET_LOCATION || [])
)

// Data composables
const {
  processedAssetNameOptions,
  processedAssetTypeOptions,
  processedAssetLocationOptions,
  filteredRecords,
  allDates,
  formattedDates,
  lastDateWithRecords,
  lastDateRecords,
  hasData,
  dateRangeDisplay,
  emptyStateDescription,
  amountByDimension,
  totalAmountByDate
} = useAssetData({
  query,
  allLoadedRecords,
  assetNameOptions: props.assetNameOptions,
  assetTypeOptions: assetTypeOptions.value, // 使用完整的类型选项
  assetLocationOptions: assetLocationOptions.value, // 使用完整的位置选项
  unitOptions: props.unitOptions
})

// Stats composables
const {statisticsData, statisticsCards} = useAssetStats({
  lastDateRecords,
  assetTypeOptions: assetTypeOptions.value, // 使用完整的类型选项
  unitOptions: props.unitOptions
})

// Chart composables
const {echartConfig} = useAssetChart({
  hasData,
  allDates,
  formattedDates,
  amountByDimension,
  totalAmountByDate,
  dateRangeDisplay,
  lastDateWithRecords,
  statisticsData,
  chartOptions
})

// 计算属性 - 显示控制
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

// 获取资产记录（从 storeToRefs 获取的 ref）
const assetRecords = computed<AssetRecord[]>(() =>
    Array.isArray(allList.value) ? allList.value : []
)

// 图表操作函数
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
  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = '图表初始化失败'
  }
}

// 防抖函数
const debouncedUpdateChart = debounce(async () => {
  await updateChartData()
}, 200)

const debouncedLoadData = debounce(async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    // 强制刷新，避免因参数缓存导致点击查询无响应
    await assetStore.loadAllRecords(true)
    allLoadedRecords.value = [...assetRecords.value]
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

// 数据加载与事件处理
async function loadData(): Promise<void> {
  if (!query.value?.startDate || !query.value?.endDate) {
    showNotification('请选择有效的日期范围', 'error')
    return
  }

  await debouncedLoadData()
}

async function handleQueryUpdate(newQuery: Partial<AssetQueryConditions>) {
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

async function handleSearch(searchQuery?: AssetQueryConditions): Promise<void> {
  try {
    isSearching.value = true

    // 使用 Partial，避免类型要求完整字段
    let q: Partial<AssetQueryConditions> | undefined = searchQuery ? { ...searchQuery } : undefined

    // 如果没有日期范围，自动填充默认范围
    if (!q || !q.startDate || !q.endDate) {
      const defaultRange = getDefaultRange()
      const parsed = parseDateRange(defaultRange)
      q = {
        assetNameIdList: q?.assetNameIdList || [],
        assetTypeIdList: q?.assetTypeIdList || [],
        assetLocationIdList: q?.assetLocationIdList || [],
        remark: q?.remark || '',
        startDate: parsed.startDate,
        endDate: parsed.endDate
      }
    }

    if (q) {
      const needReload = q.startDate !== query.value.startDate ||
          q.endDate !== query.value.endDate

      assetStore.updateQuery(q)

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

async function handleReset(): Promise<void> {
  try {
    isSearching.value = true

    assetStore.resetQuery()

    const defaultRange = getDefaultRange()
    const {startDate, endDate} = parseDateRange(defaultRange)
    assetStore.updateQuery({startDate, endDate})

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
    console.error('处理重置请求失败', error)
    showNotification('重置失败，请重试', 'error')
    isSearching.value = false
  }
}

// 生命周期
onMounted(async () => {
  await nextTick()
  isChartReady.value = true

  if (!query.value.startDate || !query.value.endDate) {
    const defaultRange = getDefaultRange()
    const {startDate, endDate} = parseDateRange(defaultRange)
    assetStore.updateQuery({startDate, endDate})
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
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', resizeChart)
  }

  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }

  destroyChart()
  clearCommonMetaCache()
})
// 监听器
watch(
    () => chartOptions,
    () => {
      if (shouldShowChart.value) {
        debouncedUpdateChart()
      }
      saveChartOptions()
    },
    {deep: true}
)

watch(
    () => isLoading.value,
    (newLoading, oldLoading) => {
      if (oldLoading && !newLoading) {
        isSearching.value = false

        if (hasData.value) {
          hasInitialData.value = true
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
    {deep: true}
)

watch(
    () => query.value.assetTypeIdList,
    () => {
      assetStore.updateQuery({assetNameIdList: []})
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
        debouncedFilterUpdate()
      }
    },
    {deep: true}
)
</script>