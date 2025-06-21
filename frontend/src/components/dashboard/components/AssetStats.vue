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
              :disabled="!isRangeValid || isLoading"
              @click="onSearch"
              color="outline"
              :icon="LucideSearch"
              variant="search"
          />
          <BaseButton
              :disabled="isLoading"
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

      <!-- 图表显示选项 -->
      <div class="flex flex-wrap items-center gap-4 pt-3 border-t">
        <span class="text-sm font-medium text-gray-600">显示维度:</span>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="showTotalTrend"
              class="rounded"
          />
          <span class="text-sm">总金额趋势</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="showNameDimension"
              class="rounded"
          />
          <span class="text-sm">按资产名称</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="showTypeDimension"
              class="rounded"
          />
          <span class="text-sm">按资产类型</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input
              type="checkbox"
              v-model="showLocationDimension"
              class="rounded"
          />
          <span class="text-sm">按资产位置</span>
        </label>
      </div>
    </div>

    <!-- 调试信息（开发时可用） -->
    <div v-if="showDebugInfo" class="text-sm text-gray-500 bg-gray-50 p-2 rounded space-y-1">
      <div>数据条数: {{ records.length }}</div>
      <div>日期数: {{ dates.length }}</div>
      <div>加载状态: {{ isLoading ? '加载中' : '已完成' }}</div>
      <div>错误信息: {{ errorMessage || '无' }}</div>
      <div>图表状态: {{ chartInstance ? '已初始化' : '未初始化' }}</div>
      <div>资产名称数: {{ Object.keys(amountByNameDate).length }}</div>
      <div>资产类型数: {{ Object.keys(amountByTypeDate).length }}</div>
      <div>资产位置数: {{ Object.keys(amountByLocationDate).length }}</div>
    </div>

    <!-- 统计信息 -->
    <div v-if="hasData" class="grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
      <div class="bg-blue-50 p-3 rounded-lg">
        <div class="text-blue-600 font-medium">总记录数</div>
        <div class="text-lg font-bold text-blue-800">{{ records.length }}</div>
      </div>
      <div class="bg-green-50 p-3 rounded-lg">
        <div class="text-green-600 font-medium">资产名称数</div>
        <div class="text-lg font-bold text-green-800">{{ Object.keys(amountByNameDate).length }}</div>
      </div>
      <div class="bg-yellow-50 p-3 rounded-lg">
        <div class="text-yellow-600 font-medium">资产类型数</div>
        <div class="text-lg font-bold text-yellow-800">{{ Object.keys(amountByTypeDate).length }}</div>
      </div>
      <div class="bg-purple-50 p-3 rounded-lg">
        <div class="text-purple-600 font-medium">资产位置数</div>
        <div class="text-lg font-bold text-purple-800">{{ Object.keys(amountByLocationDate).length }}</div>
      </div>
    </div>

    <!-- 图表 -->
    <div class="relative min-h-[500px] h-[calc(100vh-400px)]">
      <div v-if="isLoading" class="flex items-center justify-center h-full text-gray-400">
        <div class="flex items-center gap-2">
          <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
          加载中...
        </div>
      </div>
      <div v-else-if="errorMessage" class="flex items-center justify-center h-full text-red-500">
        <div class="text-center">
          <p>加载失败</p>
          <p class="text-sm mt-2">{{ errorMessage }}</p>
          <button
              @click="retryLoad"
              class="mt-3 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition-colors"
          >
            重试
          </button>
        </div>
      </div>
      <div v-else-if="!hasData" class="flex items-center justify-center h-full text-gray-400">
        <div class="text-center">
          <p>暂无数据</p>
          <p class="text-sm mt-2 text-gray-500">请调整筛选条件后重新查询</p>
        </div>
      </div>
      <div v-else ref="chartRef" class="w-full h-full chart-container"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch, onUnmounted} from 'vue'
import * as echarts from 'echarts'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import {LucideChevronDown, LucideChevronUp, LucideRotateCcw, LucideSearch,} from 'lucide-vue-next'
import {useAssetStore} from '@/store/assetStore'

interface Option {
  label: string
  value: string | number
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

const props = defineProps<{
  assetNameOptions: Option[]
  assetTypeOptions: Option[]
  assetLocationOptions: Option[]
}>()

const assetStore = useAssetStore()

// 状态管理
const selectedTypeIds = ref<(string | number)[]>([])
const selectedNameIds = ref<(string | number)[]>([])
const selectedLocationIds = ref<(string | number)[]>([])
const remark = ref('')
const rangeValue = ref('')
const showMore = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const showDebugInfo = ref(false) // 开发时可设为 true
const chartRef = ref<HTMLDivElement | null>(null)
const isChartInitializing = ref(false)

// 图表显示选项
const showTotalTrend = ref(true)
const showNameDimension = ref(true)
const showTypeDimension = ref(true)
const showLocationDimension = ref(true)

let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

// 日期格式化
function formatDate(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

// 获取默认日期范围（最近一个月）
function getLastMonthRange(): string {
  const end = new Date()
  const start = new Date()
  start.setMonth(end.getMonth() - 1)
  return `${formatDate(start)} ~ ${formatDate(end)}`
}

// 初始化日期范围
rangeValue.value = getLastMonthRange()

// 验证日期范围
const isRangeValid = computed(() => {
  if (!rangeValue.value) return false
  const parts = rangeValue.value.split('~').map(s => s.trim())
  return parts.length === 2 && parts[0] !== '' && parts[1] !== ''
})

// 获取记录数据
const records = computed<AssetRecord[]>(() => {
  const list = assetStore.allList || []
  console.log('Records computed:', list.length)
  return list as AssetRecord[]
})

// 判断是否有数据
const hasData = computed(() => {
  return dates.value.length > 0 && records.value.length > 0 && !errorMessage.value
})

// 获取所有日期（按时间排序）
const dates = computed(() => {
  const dateSet = new Set<string>()

  records.value.forEach(item => {
    if (item.acquireTime) {
      const date = item.acquireTime.split('T')[0]
      dateSet.add(date)
    }
  })

  const sortedDates = Array.from(dateSet).sort()
  console.log('Dates:', sortedDates)
  return sortedDates
})

// 按日期汇总总金额
const totalAmountByDate = computed(() => {
  const map: Record<string, number> = {}

  records.value.forEach(item => {
    if (!item.acquireTime) return

    const date = item.acquireTime.split('T')[0]
    const amount = parseFloat(item.amount) || 0

    map[date] = (map[date] || 0) + amount
  })

  console.log('Total amount by date:', map)
  return map
})

// 按资产名称和日期汇总
const amountByNameDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  records.value.forEach(item => {
    if (!item.acquireTime) return

    const date = item.acquireTime.split('T')[0]
    // 优先使用 assetName，如果没有则用 ID
    const nameKey = item.assetName || `资产${item.assetNameId}`
    const amount = parseFloat(item.amount) || 0

    if (!map[nameKey]) map[nameKey] = {}
    map[nameKey][date] = (map[nameKey][date] || 0) + amount
  })

  console.log('Amount by name and date:', map)
  return map
})

// 按资产类型和日期汇总
const amountByTypeDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  records.value.forEach(item => {
    if (!item.acquireTime) return

    const date = item.acquireTime.split('T')[0]
    // 优先使用 assetTypeName，然后 assetTypeValue，最后用 ID
    const typeKey = item.assetTypeName || item.assetTypeValue || `类型${item.assetTypeId}`
    const amount = parseFloat(item.amount) || 0

    if (!map[typeKey]) map[typeKey] = {}
    map[typeKey][date] = (map[typeKey][date] || 0) + amount
  })

  console.log('Amount by type and date:', map)
  return map
})

// 按资产位置和日期汇总
const amountByLocationDate = computed(() => {
  const map: Record<string, Record<string, number>> = {}

  records.value.forEach(item => {
    if (!item.acquireTime) return

    const date = item.acquireTime.split('T')[0]
    // 优先使用 assetLocationName，然后 assetLocationValue，最后用 ID
    const locationKey = item.assetLocationName || item.assetLocationValue || `位置${item.assetLocationId}`
    const amount = parseFloat(item.amount) || 0

    if (!map[locationKey]) map[locationKey] = {}
    map[locationKey][date] = (map[locationKey][date] || 0) + amount
  })

  console.log('Amount by location and date:', map)
  return map
})

// 填充数据系列
function fillSeriesData(
    dates: string[],
    dataMap: Record<string, Record<string, number>>,
    keys: string[]
): Array<{ name: string; data: number[] }> {
  return keys.map(key => ({
    name: key,
    data: dates.map(d => dataMap[key]?.[d] ?? 0)
  }))
}

// 填充简单数据
function fillSimpleData(dates: string[], dataMap: Record<string, number>): number[] {
  return dates.map(d => dataMap[d] ?? 0)
}

// 等待图表容器准备就绪
async function waitForChartContainer(maxAttempts = 20, interval = 100): Promise<boolean> {
  let attempts = 0

  while (attempts < maxAttempts) {
    if (chartRef.value) {
      const rect = chartRef.value.getBoundingClientRect()
      if (rect.width > 0 && rect.height > 0) {
        console.log('Chart container ready:', rect.width, 'x', rect.height)
        return true
      }
    }

    console.log(`Waiting for chart container, attempt ${attempts + 1}/${maxAttempts}`)
    await new Promise(resolve => setTimeout(resolve, interval))
    attempts++
  }

  return false
}

// 销毁图表实例
function destroyChart() {
  if (chartInstance) {
    console.log('Destroying chart instance')
    chartInstance.dispose()
    chartInstance = null
  }
}

// 初始化图表
async function initChart() {
  console.log('=== initChart called ===')
  console.log('Current state:', {
    isLoading: isLoading.value,
    errorMessage: errorMessage.value,
    hasData: hasData.value,
    recordsLength: records.value.length,
    datesLength: dates.value.length,
    isChartInitializing: isChartInitializing.value
  })

  if (isChartInitializing.value) {
    console.log('Chart is already initializing, skipping...')
    return
  }

  if (isLoading.value || errorMessage.value || !hasData.value) {
    console.log('Conditions not met for chart rendering')
    return
  }

  isChartInitializing.value = true

  try {
    await nextTick()

    const containerReady = await waitForChartContainer()
    if (!containerReady) {
      throw new Error('图表容器未能准备就绪')
    }

    destroyChart()

    console.log('Creating new chart instance')
    chartInstance = echarts.init(chartRef.value!, 'default', {
      renderer: 'canvas',
      useDirtyRect: false
    })

    // 准备数据
    const totalData = fillSimpleData(dates.value, totalAmountByDate.value)

    // 获取各维度的数据
    const nameKeys = Object.keys(amountByNameDate.value)
    const typeKeys = Object.keys(amountByTypeDate.value)
    const locationKeys = Object.keys(amountByLocationDate.value)

    const nameSeries = fillSeriesData(dates.value, amountByNameDate.value, nameKeys)
    const typeSeries = fillSeriesData(dates.value, amountByTypeDate.value, typeKeys)
    const locationSeries = fillSeriesData(dates.value, amountByLocationDate.value, locationKeys)

    console.log('Chart data prepared:', {
      totalDataLength: totalData.length,
      nameSeriesCount: nameSeries.length,
      typeSeriesCount: typeSeries.length,
      locationSeriesCount: locationSeries.length
    })

    // 配置颜色方案
    const colors = [
      '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899',
      '#14b8a6', '#f97316', '#06b6d4', '#84cc16', '#f43f5e', '#6366f1',
      '#8b5a2b', '#dc2626', '#059669', '#7c3aed', '#db2777', '#0891b2'
    ]

    // 构建系列数据
    const series: echarts.SeriesOption[] = []
    let colorIndex = 0

    // 1. 总金额趋势线（最粗、最突出）
    if (showTotalTrend.value) {
      series.push({
        name: '📈 总金额趋势',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 10,
        data: totalData,
        lineStyle: {
          width: 4,
          color: colors[colorIndex]
        },
        itemStyle: {
          color: colors[colorIndex],
          borderWidth: 2,
          borderColor: '#fff'
        },
        emphasis: {
          focus: 'series',
          scale: true
        },
        z: 10 // 最高层级
      })
      colorIndex++
    }

    // 2. 按资产名称维度
    if (showNameDimension.value && nameSeries.length > 0) {
      nameSeries.forEach((item, index) => {
        series.push({
          name: `💰 ${item.name}`,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: item.data,
          lineStyle: {
            width: 2,
            color: colors[(colorIndex + index) % colors.length]
          },
          itemStyle: {
            color: colors[(colorIndex + index) % colors.length]
          },
          emphasis: {
            focus: 'series'
          }
        })
      })
      colorIndex += nameSeries.length
    }

    // 3. 按资产类型维度
    if (showTypeDimension.value && typeSeries.length > 0) {
      typeSeries.forEach((item, index) => {
        series.push({
          name: `🏷️ ${item.name}`,
          type: 'line',
          smooth: true,
          symbol: 'triangle',
          symbolSize: 6,
          data: item.data,
          lineStyle: {
            width: 2,
            type: 'dashed',
            color: colors[(colorIndex + index) % colors.length]
          },
          itemStyle: {
            color: colors[(colorIndex + index) % colors.length]
          },
          emphasis: {
            focus: 'series'
          }
        })
      })
      colorIndex += typeSeries.length
    }

    // 4. 按资产位置维度
    if (showLocationDimension.value && locationSeries.length > 0) {
      locationSeries.forEach((item, index) => {
        series.push({
          name: `📍 ${item.name}`,
          type: 'line',
          smooth: true,
          symbol: 'diamond',
          symbolSize: 6,
          data: item.data,
          lineStyle: {
            width: 2,
            type: 'dotted',
            color: colors[(colorIndex + index) % colors.length]
          },
          itemStyle: {
            color: colors[(colorIndex + index) % colors.length]
          },
          emphasis: {
            focus: 'series'
          }
        })
      })
    }

    // 计算Y轴最大值，用于更好的显示效果
    const allValues = series.flatMap(s => (s.data as number[]) || [])
    const maxValue = Math.max(...allValues)
    const yAxisMax = maxValue > 0 ? Math.ceil(maxValue * 1.1) : 100

    // 图表配置
    const option: echarts.EChartsOption = {
      title: {
        text: '资产金额趋势分析',
        subtext: `数据时间范围: ${dates.value[0]} 至 ${dates.value[dates.value.length - 1]}`,
        left: 'center',
        top: 15,
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold',
          color: '#374151'
        },
        subtextStyle: {
          fontSize: 12,
          color: '#6b7280'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985'
          }
        },
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e5e7eb',
        borderWidth: 1,
        textStyle: {
          color: '#374151'
        },
        formatter: (params: any) => {
          if (!Array.isArray(params)) return ''

          let result = `<div style="font-weight: bold; margin-bottom: 8px; color: #1f2937">${params[0]?.axisValue}</div>`

          // 按系列类型分组显示
          const totalSeries = params.filter((p: any) => p.seriesName.includes('总金额'))
          const nameSeries = params.filter((p: any) => p.seriesName.includes('💰'))
          const typeSeries = params.filter((p: any) => p.seriesName.includes('🏷️'))
          const locationSeries = params.filter((p: any) => p.seriesName.includes('📍'))

          const renderSeries = (series: any[], title: string) => {
            if (series.length === 0) return ''
            let html = `<div style="margin-top: 6px; font-weight: 600; color: #4b5563">${title}</div>`
            series.forEach((item: any) => {
              if (item.value !== undefined && item.value > 0) {
                html += `<div style="display: flex; align-items: center; gap: 6px; margin-top: 3px">
                  <span style="display: inline-block; width: 8px; height: 8px; background: ${item.color}; border-radius: 50%"></span>
                  <span style="color: #374151">${item.seriesName.replace(/[💰🏷️📍📈]/g, '').trim()}: ￥${item.value.toFixed(2)}</span>
                </div>`
              }
            })
            return html
          }

          result += renderSeries(totalSeries, '总计')
          result += renderSeries(nameSeries, '按资产名称')
          result += renderSeries(typeSeries, '按资产类型')
          result += renderSeries(locationSeries, '按资产位置')

          return result
        }
      },
      legend: {
        type: 'scroll',
        orient: 'horizontal',
        bottom: 15,
        data: series.map(s => s.name),
        pageButtonItemGap: 8,
        pageButtonGap: 20,
        pageIconSize: 14,
        pageTextStyle: {
          fontSize: 12
        },
        textStyle: {
          fontSize: 12
        },
        itemWidth: 12,
        itemHeight: 12
      },
      grid: {
        left: 100,
        right: 50,
        bottom: 120,
        top: 80,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates.value,
        boundaryGap: false,
        axisLabel: {
          rotate: 45,
          interval: 0,
          fontSize: 11,
          color: '#6b7280',
          formatter: (value: string) => {
            const parts = value.split('-')
            return `${parts[1]}/${parts[2]}`
          }
        },
        axisLine: {
          lineStyle: {
            color: '#d1d5db'
          }
        },
        axisTick: {
          alignWithLabel: true,
          lineStyle: {
            color: '#d1d5db'
          }
        }
      },
      yAxis: {
        type: 'value',
        name: '金额（元）',
        nameLocation: 'middle',
        nameGap: 60,
        nameTextStyle: {
          fontSize: 12,
          color: '#6b7280'
        },
        axisLabel: {
          formatter: (value: number) => {
            if (value >= 10000) {
              return `￥${(value / 10000).toFixed(1)}万`
            } else {
              return `￥${value.toFixed(0)}`
            }
          },
          fontSize: 11,
          color: '#6b7280'
        },
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#e5e7eb'
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
      series,
      dataZoom: [
        {
          type: 'inside',
          start: 0,
          end: 100,
          zoomOnMouseWheel: true,
          moveOnMouseMove: true
        },
        {
          type: 'slider',
          show: true,
          start: 0,
          end: 100,
          height: 25,
          bottom: 60,
          handleStyle: {
            color: '#3b82f6'
          },
          textStyle: {
            fontSize: 11
          }
        }
      ],
      animation: true,
      animationDuration: 1000,
      animationEasing: 'cubicOut'
    }

    console.log('Setting chart option...')

    chartInstance.clear()
    chartInstance.setOption(option, true)

    await nextTick()
    chartInstance.resize()

    console.log('Chart initialized successfully')

  } catch (error) {
    console.error('Failed to initialize chart:', error)
    errorMessage.value = error instanceof Error ? error.message : '图表初始化失败'
    destroyChart()
  } finally {
    isChartInitializing.value = false
  }
}

// 加载数据
async function loadData() {
  console.log('=== loadData called ===')

  if (!isRangeValid.value) {
    console.error('Date range is invalid')
    errorMessage.value = '请选择有效的日期范围'
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  const [start, end] = rangeValue.value.split('~').map(s => s.trim())
  console.log('Loading data for range:', start, 'to', end)

  try {
    assetStore.updateQuery({
      assetTypeIdList: selectedTypeIds.value,
      assetNameIdList: selectedNameIds.value,
      assetLocationIdList: selectedLocationIds.value,
      startDate: start,
      endDate: end,
      remark: remark.value.trim(),
    })

    await assetStore.loadAllRecords()

    console.log('Data loaded successfully, records:', assetStore.allList?.length || 0)

  } catch (error) {
    console.error('Failed to load data:', error)
    errorMessage.value = error instanceof Error ? error.message : '数据加载失败'
  } finally {
    isLoading.value = false
  }
}

// 重试加载
async function retryLoad() {
  console.log('=== Retry load ===')
  await loadData()
}

// 搜索
async function onSearch() {
  console.log('=== Search clicked ===')
  await loadData()
}

// 重置
async function onReset() {
  console.log('=== Reset clicked ===')

  selectedTypeIds.value = []
  selectedNameIds.value = []
  selectedLocationIds.value = []
  remark.value = ''
  rangeValue.value = getLastMonthRange()
  errorMessage.value = ''

  // 重置显示选项
  showTotalTrend.value = true
  showNameDimension.value = true
  showTypeDimension.value = true
  showLocationDimension.value = true

  assetStore.allList = []
  await loadData()
}

// 切换更多选项
function toggleMore() {
  showMore.value = !showMore.value
}

// 处理类型变化
function handleTypeChange() {
  selectedNameIds.value = []
}

// 处理窗口大小变化
function handleResize() {
  if (chartInstance && !isChartInitializing.value) {
    console.log('Resizing chart')
    chartInstance.resize()
  }
}

// 设置 ResizeObserver
function setupResizeObserver() {
  if (chartRef.value && !resizeObserver) {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(chartRef.value)
  }
}

// 清理 ResizeObserver
function cleanupResizeObserver() {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
}

// 监听数据变化
watch(
    [() => assetStore.allList, isLoading],
    async ([newList, newIsLoading], [oldList, oldIsLoading]) => {
      console.log('=== Data or loading state changed ===')
      console.log('Loading changed:', oldIsLoading, '->', newIsLoading)
      console.log('Data changed:', oldList?.length || 0, '->', newList?.length || 0)

      if (oldIsLoading && !newIsLoading && newList && newList.length > 0) {
        console.log('Data loading completed, initializing chart...')
        await nextTick()
        await initChart()
      }
    },
    {deep: true}
)

// 监听图表显示选项变化
watch(
    [showTotalTrend, showNameDimension, showTypeDimension, showLocationDimension],
    async () => {
      if (hasData.value && !isLoading.value && !isChartInitializing.value) {
        console.log('Chart display options changed, reinitializing chart...')
        await nextTick()
        await initChart()
      }
    }
)

// 监听图表容器的变化
watch(chartRef, async (newRef) => {
  console.log('=== chartRef changed ===', !!newRef)

  if (newRef) {
    setupResizeObserver()

    if (hasData.value && !isLoading.value && !isChartInitializing.value) {
      console.log('Chart container is ready, initializing chart...')
      await nextTick()
      await initChart()
    }
  } else {
    cleanupResizeObserver()
    destroyChart()
  }
})

// 组件挂载
onMounted(async () => {
  console.log('=== Component mounted ===')

  window.addEventListener('resize', handleResize)

  // 初始加载最近一个月的数据
  await loadData()
})

// 组件卸载
onBeforeUnmount(() => {
  console.log('=== Component unmounting ===')

  window.removeEventListener('resize', handleResize)

  cleanupResizeObserver()
  destroyChart()
})

onUnmounted(() => {
  console.log('=== Component unmounted ===')
})

// 暴露方法供外部调用
defineExpose({
  loadData,
  initChart,
  retryLoad,
  records,
  dates,
  totalAmountByDate,
  amountByNameDate,
  amountByTypeDate,
  amountByLocationDate,
  chartInstance: () => chartInstance
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
  accent-color: #3b82f6;
}

/* 自定义滚动条样式 */
:deep(.echarts-legend-scroll) {
  scrollbar-width: thin;
  scrollbar-color: #e5e7eb #f3f4f6;
}

:deep(.echarts-legend-scroll::-webkit-scrollbar) {
  height: 8px;
}

:deep(.echarts-legend-scroll::-webkit-scrollbar-track) {
  background: #f3f4f6;
  border-radius: 4px;
}

:deep(.echarts-legend-scroll::-webkit-scrollbar-thumb) {
  background: #e5e7eb;
  border-radius: 4px;
}

:deep(.echarts-legend-scroll::-webkit-scrollbar-thumb:hover) {
  background: #d1d5db;
}
</style>