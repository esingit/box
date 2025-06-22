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
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showTotalTrend"
              class="rounded"
          />
          <span class="text-sm">总金额趋势</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showNameDimension"
              class="rounded"
          />
          <span class="text-sm">按资产名称</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showTypeDimension"
              class="rounded"
          />
          <span class="text-sm">按资产类型</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="chartOptions.showLocationDimension"
              class="rounded"
          />
          <span class="text-sm">按资产位置</span>
        </label>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="hasData" class="grid grid-cols-1 md:grid-cols-5 gap-4 text-sm">
      <div class="bg-blue-50 p-3 rounded-lg">
        <div class="text-blue-600 font-medium">总记录数</div>
        <div class="text-lg font-bold text-blue-800">{{ assetRecords.length }}</div>
      </div>
      <div class="bg-green-50 p-3 rounded-lg">
        <div class="text-green-600 font-medium">资产名称数</div>
        <div class="text-lg font-bold text-green-800">{{ nameCount }}</div>
      </div>
      <div class="bg-yellow-50 p-3 rounded-lg">
        <div class="text-yellow-600 font-medium">资产类型数</div>
        <div class="text-lg font-bold text-yellow-800">{{ typeCount }}</div>
      </div>
      <div class="bg-purple-50 p-3 rounded-lg">
        <div class="text-purple-600 font-medium">资产位置数</div>
        <div class="text-lg font-bold text-purple-800">{{ locationCount }}</div>
      </div>
      <div class="bg-red-50 p-3 rounded-lg">
        <div class="text-red-600 font-medium">总金额</div>
        <div class="text-lg font-bold text-red-800">￥{{ totalAmount }}</div>
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
        <div class="flex justify-center mt-4">
          <button
              @click="handleRetry"
              class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors shadow-sm"
          >
            重新加载
          </button>
        </div>
      </div>

      <!-- 空数据状态 -->
      <div v-else-if="!hasData" class="h-full">
        <BaseEmptyState
            icon="Wallet"
            message="暂无资产数据"
            :description="emptyStateDescription"
        />
        <div v-if="hasSearchConditions" class="flex justify-center mt-4">
          <button
              @click="handleReset"
              class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors shadow-sm"
          >
            重置筛选条件
          </button>
        </div>
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
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import { useAssetStore } from '@/store/assetStore'
import { useDateRange, useChart } from '@/utils/common'
import emitter from '@/utils/eventBus'

// 接口定义
interface Option {
  label: string
  value: string | number
  id?: string
  name?: string
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
}>()

// Composables
const assetStore = useAssetStore()
const {
  dateRange,
  isDateRangeValid,
  getDefaultRange,
  parseDateRange,
  dateRangeDisplay
} = useDateRange()
const {
  chartRef,
  initChart,
  destroyChart,
  resizeChart
} = useChart()

// 响应式状态
const selectedTypeIds = ref<(string | number)[]>([])
const selectedNameIds = ref<(string | number)[]>([])
const selectedLocationIds = ref<(string | number)[]>([])
const remark = ref('')
const showMore = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const isChartReady = ref(false)

// 图表选项
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

// 创建名称映射 - 添加空值检查
const nameMapping = computed(() => {
  const map: Record<string, string> = {}
  if (!props.assetNameOptions || !Array.isArray(props.assetNameOptions)) {
    return map
  }

  props.assetNameOptions.forEach(option => {
    if (option) {
      const id = option.id || option.value
      const name = option.name || option.label
      if (id && name) {
        map[String(id)] = String(name)
      }
    }
  })
  return map
})

const typeMapping = computed(() => {
  const map: Record<string, string> = {}
  if (!props.assetTypeOptions || !Array.isArray(props.assetTypeOptions)) {
    return map
  }

  props.assetTypeOptions.forEach(option => {
    if (option) {
      const id = option.id || option.value
      const name = option.name || option.label
      if (id && name) {
        map[String(id)] = String(name)
      }
    }
  })
  return map
})

const locationMapping = computed(() => {
  const map: Record<string, string> = {}
  if (!props.assetLocationOptions || !Array.isArray(props.assetLocationOptions)) {
    return map
  }

  props.assetLocationOptions.forEach(option => {
    if (option) {
      const id = option.id || option.value
      const name = option.name || option.label
      if (id && name) {
        map[String(id)] = String(name)
      }
    }
  })
  return map
})

// 工具函数
function getDisplayName(id: string, mapping: Record<string, string>, fallback?: string | null, prefix = '未知'): string {
  return mapping[id] || fallback || `${prefix}${id}`
}

// 计算属性 - 添加空值检查
const assetRecords = computed<AssetRecord[]>(() => {
  const list = assetStore.allList
  if (!list || !Array.isArray(list)) {
    return []
  }
  return list as AssetRecord[]
})

const hasData = computed(() => {
  return assetRecords.value.length > 0 && allDates.value.length > 0 && !errorMessage.value
})

// 检查是否有搜索条件
const hasSearchConditions = computed(() => {
  return selectedTypeIds.value.length > 0 ||
      selectedNameIds.value.length > 0 ||
      selectedLocationIds.value.length > 0 ||
      remark.value.trim() !== ''
})

// 空状态描述
const emptyStateDescription = computed(() => {
  if (!isDateRangeValid.value) {
    return '请选择日期范围查看资产数据'
  }
  if (hasSearchConditions.value) {
    return '当前筛选条件下没有找到资产记录，请尝试调整筛选条件'
  }
  return `${dateRangeDisplay.value}期间暂无资产记录`
})

// 获取所有日期并排序 - 添加空值检查
const allDates = computed(() => {
  const dateSet = new Set<string>()

  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return []
  }

  assetRecords.value.forEach(record => {
    if (record && record.acquireTime) {
      const date = record.acquireTime.split('T')[0]
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

// 按日期汇总总金额 - 添加空值检查
const totalAmountByDate = computed(() => {
  const map: Record<string, number> = {}

  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return map
  }

  assetRecords.value.forEach(record => {
    if (!record || !record.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const amount = parseFloat(record.amount) || 0
    map[date] = (map[date] || 0) + amount
  })
  return map
})

// 按维度汇总数据 - 添加空值检查
const amountByNameDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return map
  }

  assetRecords.value.forEach(record => {
    if (!record || !record.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const nameKey = getDisplayName(record.assetNameId, nameMapping.value, record.assetName, '资产')
    const amount = parseFloat(record.amount) || 0
    if (!map[nameKey]) map[nameKey] = {}
    map[nameKey][date] = (map[nameKey][date] || 0) + amount
  })
  return map
})

const amountByTypeDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return map
  }

  assetRecords.value.forEach(record => {
    if (!record || !record.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const typeKey = getDisplayName(
        record.assetTypeId,
        typeMapping.value,
        record.assetTypeName || record.assetTypeValue,
        '类型'
    )
    const amount = parseFloat(record.amount) || 0
    if (!map[typeKey]) map[typeKey] = {}
    map[typeKey][date] = (map[typeKey][date] || 0) + amount
  })
  return map
})

const amountByLocationDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return map
  }

  assetRecords.value.forEach(record => {
    if (!record || !record.acquireTime) return
    const date = record.acquireTime.split('T')[0]
    const locationKey = getDisplayName(
        record.assetLocationId,
        locationMapping.value,
        record.assetLocationName || record.assetLocationValue,
        '位置'
    )
    const amount = parseFloat(record.amount) || 0
    if (!map[locationKey]) map[locationKey] = {}
    map[locationKey][date] = (map[locationKey][date] || 0) + amount
  })
  return map
})

// 统计信息 - 添加空值检查
const nameCount = computed(() => {
  return amountByNameDate.value ? Object.keys(amountByNameDate.value).length : 0
})

const typeCount = computed(() => {
  return amountByTypeDate.value ? Object.keys(amountByTypeDate.value).length : 0
})

const locationCount = computed(() => {
  return amountByLocationDate.value ? Object.keys(amountByLocationDate.value).length : 0
})

const totalAmount = computed(() => {
  if (!assetRecords.value || !Array.isArray(assetRecords.value)) {
    return '0.00'
  }

  return assetRecords.value
      .reduce((sum, record) => sum + (parseFloat(record?.amount || '0') || 0), 0)
      .toFixed(2)
})

// 生成图表系列数据
function createSeriesData(
    dates: string[],
    dataMap: Record<string, Record<string, number>>,
    keys: string[]
): Array<{ name: string; data: number[] }> {
  if (!dates || !Array.isArray(dates) || !dataMap || !keys || !Array.isArray(keys)) {
    return []
  }

  return keys.map(key => ({
    name: key,
    data: dates.map(date => dataMap[key]?.[date] ?? 0)
  }))
}

const chartSeries = computed(() => {
  if (!hasData.value || !allDates.value.length) return []

  const series: any[] = []
  let colorIndex = 0

  try {
    // 总金额趋势线
    if (chartOptions.showTotalTrend && totalAmountByDate.value) {
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

    // 按资产名称维度
    if (chartOptions.showNameDimension && amountByNameDate.value) {
      const nameKeys = Object.keys(amountByNameDate.value)
      const nameSeries = createSeriesData(allDates.value, amountByNameDate.value, nameKeys)
      nameSeries.forEach((item, index) => {
        if (item.data.some(v => v > 0)) {
          const color = CHART_COLORS[(colorIndex + index) % CHART_COLORS.length]
          series.push({
            name: `💰 ${item.name}`,
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 5,
            data: item.data,
            lineStyle: { width: 2, color, shadowColor: `${color}33`, shadowBlur: 2 },
            itemStyle: { color, borderWidth: 1, borderColor: '#fff' },
            emphasis: { focus: 'series' }
          })
        }
      })
      colorIndex += nameSeries.length
    }

    // 按资产类型维度
    if (chartOptions.showTypeDimension && amountByTypeDate.value) {
      const typeKeys = Object.keys(amountByTypeDate.value)
      const typeSeries = createSeriesData(allDates.value, amountByTypeDate.value, typeKeys)
      typeSeries.forEach((item, index) => {
        if (item.data.some(v => v > 0)) {
          const color = CHART_COLORS[(colorIndex + index) % CHART_COLORS.length]
          series.push({
            name: `🏷️ ${item.name}`,
            type: 'line',
            smooth: true,
            symbol: 'triangle',
            symbolSize: 5,
            data: item.data,
            lineStyle: { width: 2, type: 'dashed', color, shadowColor: `${color}33`, shadowBlur: 2 },
            itemStyle: { color, borderWidth: 1, borderColor: '#fff' },
            emphasis: { focus: 'series' }
          })
        }
      })
      colorIndex += typeSeries.length
    }

    // 按资产位置维度
    if (chartOptions.showLocationDimension && amountByLocationDate.value) {
      const locationKeys = Object.keys(amountByLocationDate.value)
      const locationSeries = createSeriesData(allDates.value, amountByLocationDate.value, locationKeys)
      locationSeries.forEach((item, index) => {
        if (item.data.some(v => v > 0)) {
          const color = CHART_COLORS[(colorIndex + index) % CHART_COLORS.length]
          series.push({
            name: `📍 ${item.name}`,
            type: 'line',
            smooth: true,
            symbol: 'diamond',
            symbolSize: 5,
            data: item.data,
            lineStyle: { width: 2, type: 'dotted', color, shadowColor: `${color}33`, shadowBlur: 2 },
            itemStyle: { color, borderWidth: 1, borderColor: '#fff' },
            emphasis: { focus: 'series' }
          })
        }
      })
    }

    console.log('Generated chart series:', series.length)
    return series
  } catch (error) {
    console.error('Error generating chart series:', error)
    return []
  }
})

// 生成 ECharts 配置
const echartConfig = computed(() => {
  if (!hasData.value || !chartSeries.value.length || !allDates.value.length) {
    console.log('No chart config - hasData:', hasData.value, 'series length:', chartSeries.value.length, 'dates:', allDates.value.length)
    return null
  }

  try {
    const hasMultipleDates = allDates.value.length > 7
    const allValues = chartSeries.value.flatMap(s => s.data || [])
    const maxValue = Math.max(...allValues)
    const yAxisMax = maxValue > 0 ? Math.ceil(maxValue * 1.1) : 100

    console.log('Generating chart config with series:', chartSeries.value.length)

    return {
      title: {
        text: '资产金额趋势分析',
        subtext: `统计期间: ${dateRangeDisplay.value}`,
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

          const date = allDates.value[params[0]?.dataIndex] || ''
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
                  result += `<div style="display: flex; align-items: center; gap: 8px; margin-top: 4px">
                    <span style="display: inline-block; width: 8px; height: 8px; background: ${item.color}; border-radius: 50%"></span>
                    <span>${item.seriesName.replace(/[💰🏷️📍📈]/g, '').trim()}: <strong>￥${item.value.toFixed(2)}</strong></span>
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
        name: '金额（元）',
        nameTextStyle: { fontSize: 12, color: '#718096' },
        axisLabel: {
          fontSize: 11,
          color: '#718096',
          formatter: (value: number) => value >= 10000 ? `￥${(value / 10000).toFixed(1)}万` : `￥${value.toFixed(0)}`
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
    }
  } catch (error) {
    console.error('Error generating chart config:', error)
    return null
  }
})

// 通知函数
function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
  emitter.emit('notify', { message, type })
}

// 初始化图表的函数
async function initializeChart(): Promise<void> {
  console.log('=== initializeChart called ===')
  console.log('Chart ready:', isChartReady.value)
  console.log('Has data:', hasData.value)
  console.log('Chart config exists:', !!echartConfig.value)
  console.log('Chart ref exists:', !!chartRef.value)

  if (!isChartReady.value || !hasData.value || !echartConfig.value) {
    console.log('Chart initialization skipped - conditions not met')
    return
  }

  try {
    // 多次等待确保DOM完全准备好
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 100))

    if (!chartRef.value) {
      console.warn('Chart container not found after waiting')
      return
    }

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
      assetTypeIdList: selectedTypeIds.value,
      assetNameIdList: selectedNameIds.value,
      assetLocationIdList: selectedLocationIds.value,
      startDate,
      endDate,
      remark: remark.value.trim()
    })

    await assetStore.loadAllRecords()

    console.log('Data loaded, records count:', assetRecords.value.length)

    // 等待数据更新到计算属性后再初始化图表
    await nextTick()
    if (hasData.value) {
      showNotification('资产数据加载成功', 'success')
      await initializeChart()
    } else {
      console.log('No data after loading')
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
  chartOptions.showTotalTrend = true
  chartOptions.showNameDimension = true
  chartOptions.showTypeDimension = true
  chartOptions.showLocationDimension = true
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
  console.log('=== Component mounted ===')

  // 等待DOM完全渲染
  await nextTick()

  // 标记图表容器准备好
  isChartReady.value = true

  // 设置默认日期范围
  dateRange.value = getDefaultRange()

  // 加载数据
  await loadData()

  // 监听窗口大小变化
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  destroyChart()
})

// 监听图表选项变化
watch(
    () => [
      chartOptions.showTotalTrend,
      chartOptions.showNameDimension,
      chartOptions.showTypeDimension,
      chartOptions.showLocationDimension
    ],
    async () => {
      console.log('Chart options changed')
      if (isChartReady.value && !isLoading.value) {
        await initializeChart()
      }
    },
    { deep: true }
)

// 监听数据变化
watch(
    () => assetStore.allList,
    async () => {
      console.log('Asset store data changed')
      if (isChartReady.value && !isLoading.value) {
        await nextTick()
        await initializeChart()
      }
    },
    { deep: true }
)

// 监听图表容器变化
watch(chartRef, async (newRef) => {
  console.log('Chart ref changed:', !!newRef)
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

/* 自定义复选框样式 */
input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #4A5568;
}

/* 按钮样式优化 */
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