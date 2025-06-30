<template>
  <div class="relative border border-gray-200 rounded-xl" style="min-height: 520px; max-height: 520px; width: 100%;">
    <!-- 表格头部区域 -->
    <div
        :class="[
        'top-0 z-20 bg-gray-50 rounded-t-xl',
        shouldStickyHeader ? 'sticky' : ''
      ]"
    >
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <thead>
        <tr>
          <th
              v-for="(col, idx) in columns"
              :key="col.key"
              class="px-3 py-2 font-medium whitespace-nowrap relative group"
              :style="{ width: columnWidths[col.key] + 'px' }"
              :class="getHeaderAlignClass(col)"
          >
            <div
                :class="[
                  'w-full flex items-center gap-1',
                  getHeaderTextAlignClass(col),
                  'select-none truncate pr-2',
                  col.sortable ? 'cursor-pointer hover:text-green-600 transition-colors' : ''
                ]"
                @click="col.sortable ? handleSort(col.key) : null"
            >
              <span class="truncate">{{ col.label }}</span>

              <!-- 排序图标 -->
              <div v-if="col.sortable" class="flex-shrink-0 ml-1">
                <svg
                    v-if="sortKey === col.key && sortOrder === 'asc'"
                    width="14" height="14" viewBox="0 0 24 24"
                    fill="none" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round"
                    class="text-green-600"
                >
                  <path d="m18 15-6-6-6 6"/>
                </svg>
                <svg
                    v-else-if="sortKey === col.key && sortOrder === 'desc'"
                    width="14" height="14" viewBox="0 0 24 24"
                    fill="none" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round"
                    class="text-green-600"
                >
                  <path d="m6 9 6 6 6-6"/>
                </svg>
                <svg
                    v-else
                    width="14" height="14" viewBox="0 0 24 24"
                    fill="none" stroke="currentColor" stroke-width="2"
                    stroke-linecap="round" stroke-linejoin="round"
                    class="text-gray-400 group-hover:text-gray-600"
                >
                  <path d="m7 15 5 5 5-5"/>
                  <path d="m7 9 5-5 5 5"/>
                </svg>
              </div>
            </div>

            <div
                v-if="col.resizable && idx < columns.length - 1"
                class="absolute right-0 top-0 h-full w-1 cursor-col-resize group-hover:bg-gray-300"
                @mousedown.prevent="startResize($event, col.key)"
                @dblclick.prevent="resetColumnWidth(col.key)"
            ></div>
          </th>
        </tr>
        </thead>
      </table>
    </div>

    <!-- 表格内容区域 -->
    <div
        ref="contentRef"
        class="relative"
        style="height: 480px; overflow-y: auto;"
        @scroll="checkScrollable"
    >
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <colgroup>
          <col
              v-for="col in columns"
              :key="col.key"
              :style="{ width: columnWidths[col.key] + 'px' }"
          />
        </colgroup>

        <tbody>
        <tr v-if="loading">
          <td :colspan="columns.length" class="py-8">
            <slot name="loading">
              <div class="space-y-2">
                <div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div>
              </div>
            </slot>
          </td>
        </tr>

        <tr v-else-if="!sortedData.length">
          <td :colspan="columns.length" class="py-8 text-center text-gray-400">
            <slot name="empty">
              <BaseEmptyState
                  icon="Dumbbell"
                  message="暂无数据"
                  description="点击上方的添加按钮开始记录"
              />
            </slot>
          </td>
        </tr>

        <tr
            v-else
            v-for="(row, rowIndex) in sortedData"
            :key="row.id || rowIndex"
            class="hover:bg-gray-100 transition-colors"
            :class="{ 'relative z-[100]': activeRowIndex === rowIndex }"
        >
          <td
              v-for="col in columns"
              :key="col.key"
              class="px-3 py-2 whitespace-nowrap"
              :style="{
                width: columnWidths[col.key] + 'px',
                maxWidth: columnWidths[col.key] + 'px',
                minWidth: columnWidths[col.key] + 'px',
                position: hasDropdown(col) && activeRowIndex === rowIndex ? 'relative' : 'static',
                zIndex: hasDropdown(col) && activeRowIndex === rowIndex ? 200 : 'auto',
                overflow: hasDropdown(col) ? 'visible' : 'hidden'
              }"
              :class="getCellAlignClass(col)"
          >
            <!-- 操作列 -->
            <template v-if="col.actions">
              <div class="flex justify-end">
                <slot name="actions" :record="row" :index="rowIndex">
                  <BaseActions
                      :record="row"
                      @edit="handleEdit(row, rowIndex)"
                      @delete="handleDelete(row, rowIndex)"
                  />
                </slot>
              </div>
            </template>

            <!-- 自定义单元格内容 -->
            <template v-else-if="$slots[`cell-${col.key}`]">
              <div
                  :class="[
                    'w-full',
                    hasDropdown(col) ? 'overflow-visible' : 'overflow-hidden'
                  ]"
                  @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)"
                  @mousemove="updateTooltipPosition($event)"
                  @mouseleave="hideTooltip"
              >
                <slot
                    :name="`cell-${col.key}`"
                    :record="row"
                    :index="rowIndex"
                    :column="col"
                    :value="row[col.key]"
                    :set-active-row="setActiveRow"
                    :clear-active-row="clearActiveRow"
                />
              </div>
            </template>

            <!-- 可编辑单元格 -->
            <template v-else-if="isEditable(col)">
              <div
                  :class="[
                    'w-full',
                    hasDropdown(col) ? 'overflow-visible' : 'overflow-hidden'
                  ]"
                  @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)"
                  @mousemove="updateTooltipPosition($event)"
                  @mouseleave="hideTooltip"
              >
                <component
                    :is="getEditorComponent(col)"
                    :model-value="row[col.key]"
                    :column="col"
                    :record="row"
                    :index="rowIndex"
                    @update:model-value="handleCellChange(rowIndex, col.key, $event)"
                    @blur="handleCellBlur(rowIndex, col.key)"
                />
              </div>
            </template>

            <!-- 普通显示单元格 -->
            <template v-else>
              <div
                  class="w-full overflow-hidden"
                  @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)"
                  @mousemove="updateTooltipPosition($event)"
                  @mouseleave="hideTooltip"
              >
                  <span class="block truncate">
                    {{ formatCell(row, col.key, col) }}
                  </span>
              </div>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 单一全局Tooltip -->
    <Teleport to="body">
      <div
          v-if="tooltipVisible"
          class="fixed pointer-events-none select-none bg-gray-800 text-white text-xs rounded px-2 py-1 shadow-lg"
          :style="{
          top: tooltipPosition.y + 'px',
          left: tooltipPosition.x + 'px',
          zIndex: 9999,
          maxWidth: '400px',
          wordWrap: 'break-word',
          whiteSpace: 'pre-wrap'
        }"
      >
        {{ tooltipContent }}
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, markRaw, computed, nextTick, onMounted, watch, onUnmounted } from 'vue'
import type { Component } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'
// 编辑器组件
import TextEditor from './editors/TextEditor.vue'
import NumberEditor from './editors/NumberEditor.vue'
import SelectEditor from './editors/SelectEditor.vue'
import DateEditor from './editors/DateEditor.vue'

// 导入需要的 stores
import { useAssetNameStore } from '@/store/assetNameStore'

// 类型定义
interface Option {
  label: string
  value: string | number
  [key: string]: any
}

interface Column {
  key: string
  label: string
  type?: 'text' | 'number' | 'select' | 'date' | 'time' | 'datetime' | 'money' | 'amount' | 'custom' | 'scan-result'
  defaultWidth?: number
  resizable?: boolean
  sortable?: boolean
  editable?: boolean
  actions?: boolean
  align?: 'left' | 'center' | 'right'
  headerAlign?: 'left' | 'center' | 'right'
  options?: Option[]
  displayField?: string
  formatter?: (row: any, key: string) => string
  tooltipFormatter?: (row: any, key: string) => string
}

interface Props {
  columns: Column[]
  data: any[]
  loading?: boolean
  editable?: boolean
}

// Props
const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false,
  editable: false
})

// Emits
const emit = defineEmits<{
  edit: [row: any, index: number]
  delete: [row: any, index: number]
  'cell-change': [row: any, key: string, value: any, index: number]
  'cell-blur': [row: any, key: string, index: number]
}>()

// 获取 store 实例
const assetNameStore = useAssetNameStore()

// 常量
const DEFAULT_WIDTH = 100

// 响应式数据
const columnWidths = reactive<Record<string, number>>({})
const activeRowIndex = ref<number | null>(null)
const contentRef = ref<HTMLDivElement>()
const isContentScrollable = ref(false)

// 排序状态
const sortKey = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc' | null>(null)

// Tooltip 控制 - 使用单一全局tooltip
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipPosition = reactive({x: 0, y: 0})
const tooltipDebounceTimer = ref<number | null>(null)
const tooltipHideTimer = ref<number | null>(null)

// 拖拽调整列宽相关
let resizingKey: string | null = null
let startX = 0
let startWidth = 0
let resizeTimer: number | null = null

// 创建字段到store的映射
const storeMapping: Record<string, () => Option[]> = {
  assetNameId: () => {
    // 优先使用 assetNameOptions
    if (assetNameStore.assetNameOptions && assetNameStore.assetNameOptions.length > 0) {
      return assetNameStore.assetNameOptions
    }
    // 如果没有，尝试从 assetName 构建
    if (assetNameStore.assetName && assetNameStore.assetName.length > 0) {
      return assetNameStore.assetName.map((item: any) => ({
        label: item.name || item.label || item.assetName || '未知',
        value: String(item.id || item.value || item.assetNameId || '')
      }))
    }
    return []
  }
}

// 编辑器组件映射
const editorComponents: Record<string, Component> = {
  text: markRaw(TextEditor),
  number: markRaw(NumberEditor),
  select: markRaw(SelectEditor),
  date: markRaw(DateEditor)
}

// 计算属性
const sortedData = computed(() => {
  if (!sortKey.value || !sortOrder.value || !props.data.length) {
    return props.data
  }

  const key = sortKey.value
  const order = sortOrder.value

  return [...props.data].sort((a, b) => {
    return compareValues(a[key], b[key], order)
  })
})

const shouldStickyHeader = computed(() => {
  return isContentScrollable.value && sortedData.value.length > 0 && !props.loading
})

// 初始化列宽
props.columns.forEach(col => {
  columnWidths[col.key] = col.defaultWidth || DEFAULT_WIDTH
})

// 智能比较函数
function compareValues(aVal: any, bVal: any, order: 'asc' | 'desc'): number {
  // 处理空值
  if (aVal == null && bVal == null) return 0
  if (aVal == null) return order === 'asc' ? 1 : -1
  if (bVal == null) return order === 'asc' ? -1 : 1

  // 先尝试日期比较
  const dateResult = compareDates(aVal, bVal, order)
  if (dateResult !== null) return dateResult

  // 转换为字符串进行其他类型检测
  const aStr = String(aVal).trim()
  const bStr = String(bVal).trim()

  // 检测是否为数字
  const aNum = parseFloat(aStr)
  const bNum = parseFloat(bStr)
  const aIsNum = !isNaN(aNum) && isFinite(aNum) && aStr === String(aNum)
  const bIsNum = !isNaN(bNum) && isFinite(bNum) && bStr === String(bNum)

  // 数字比较
  if (aIsNum && bIsNum) {
    const result = aNum - bNum
    return order === 'asc' ? result : -result
  }

  // 字符串比较（支持中文）
  const result = aStr.localeCompare(bStr, 'zh-CN', {
    numeric: true,
    sensitivity: 'base'
  })

  return order === 'asc' ? result : -result
}

// 专门的日期比较函数
function compareDates(aVal: any, bVal: any, order: 'asc' | 'desc'): number | null {
  // 将值转为字符串
  const aStr = String(aVal).trim()
  const bStr = String(bVal).trim()

  // 检查是否符合日期格式
  const datePatterns = [
    /^\d{4}-\d{2}-\d{2}$/,                    // YYYY-MM-DD
    /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/,     // YYYY-MM-DD HH:mm
    /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}$/ // YYYY-MM-DD HH:mm:ss
  ]

  const isDate = (str: string) => datePatterns.some(pattern => pattern.test(str))

  if (isDate(aStr) && isDate(bStr)) {
    const aDate = new Date(aStr)
    const bDate = new Date(bStr)

    // 验证日期有效性
    if (!isNaN(aDate.getTime()) && !isNaN(bDate.getTime())) {
      const result = aDate.getTime() - bDate.getTime()
      return order === 'asc' ? result : -result
    }
  }

  return null // 不是日期，返回 null
}

// 检查内容是否可滚动
function checkScrollable(): void {
  if (!contentRef.value) return

  const element = contentRef.value
  isContentScrollable.value = element.scrollHeight > element.clientHeight
}

// 处理排序
function handleSort(key: string): void {
  if (sortKey.value === key) {
    // 同一列：无排序 -> 升序 -> 降序 -> 无排序
    if (sortOrder.value === null) {
      sortOrder.value = 'asc'
    } else if (sortOrder.value === 'asc') {
      sortOrder.value = 'desc'
    } else {
      sortKey.value = null
      sortOrder.value = null
    }
  } else {
    // 不同列：直接设置为升序
    sortKey.value = key
    sortOrder.value = 'asc'
  }
}

// 判断列是否包含下拉框 - 按类型判断
function hasDropdown(col: Column): boolean {
  return col.type === 'select' || col.type === 'custom'
}

// 设置活动行
function setActiveRow(rowIndex: number): void {
  activeRowIndex.value = rowIndex
}

// 清除活动行
function clearActiveRow(): void {
  activeRowIndex.value = null
}

// 判断列是否可编辑
function isEditable(col: Column): boolean {
  return props.editable && col.editable !== false && !!col.type && col.type !== 'custom'
}

// 获取编辑器组件
function getEditorComponent(col: Column): Component {
  return editorComponents[col.type || 'text'] || editorComponents.text
}

// 处理单元格变更
function handleCellChange(rowIndex: number, key: string, value: any): void {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex])
  emit('cell-change', props.data[originalIndex], key, value, originalIndex)
}

// 处理单元格失焦
function handleCellBlur(rowIndex: number, key: string): void {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex])
  emit('cell-blur', props.data[originalIndex], key, originalIndex)
}

// 获取表头对齐样式 - 按类型判断
function getHeaderAlignClass(col: Column): string {
  if (col.headerAlign) {
    return `text-${col.headerAlign}`
  }
  if (col.actions) return 'text-center'
  if (col.type === 'number' || col.type === 'money' || col.type === 'amount') return 'text-right'
  if (col.type === 'date' || col.type === 'time' || col.type === 'datetime') return 'text-center'
  return 'text-left'
}

function getHeaderTextAlignClass(col: Column): string {
  const alignClass = getHeaderAlignClass(col)
  if (alignClass === 'text-right') return 'justify-end'
  if (alignClass === 'text-center') return 'justify-center'
  return 'justify-start'
}

function getCellAlignClass(col: Column): string {
  if (col.align) {
    return `text-${col.align}`
  }
  if (col.actions) return 'text-right'
  if (col.type === 'number' || col.type === 'money' || col.type === 'amount') return 'text-right'
  if (col.type === 'date' || col.type === 'time' || col.type === 'datetime') return 'text-center'
  return 'text-left'
}

// 拖拽列宽调整
function doResize(e: MouseEvent): void {
  if (!resizingKey) return
  if (resizeTimer) return
  resizeTimer = window.setTimeout(() => {
    const delta = e.pageX - startX
    const newWidth = Math.max(60, startWidth + delta)
    columnWidths[resizingKey] = newWidth
    resizeTimer = null
  }, 16)
}

function startResize(e: MouseEvent, key: string): void {
  resizingKey = key
  startX = e.pageX
  startWidth = columnWidths[key]
  window.addEventListener('mousemove', doResize)
  window.addEventListener('mouseup', stopResize)
}

function stopResize(): void {
  resizingKey = null
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
}

function resetColumnWidth(key: string): void {
  const col = props.columns.find(c => c.key === key)
  columnWidths[key] = col?.defaultWidth || DEFAULT_WIDTH
}

// 优化的Tooltip处理
function handleCellMouseEnter(rowIndex: number, fieldKey: string, event: MouseEvent, row: any, col: Column): void {
  // 清除任何现有的定时器
  if (tooltipDebounceTimer.value) {
    clearTimeout(tooltipDebounceTimer.value)
    tooltipDebounceTimer.value = null
  }

  if (tooltipHideTimer.value) {
    clearTimeout(tooltipHideTimer.value)
    tooltipHideTimer.value = null
  }

  // 只跳过操作列，其他所有列都显示tooltip
  if (col.actions) {
    return
  }

  // 使用防抖延迟显示tooltip，避免快速移动鼠标时频繁显示
  tooltipDebounceTimer.value = window.setTimeout(() => {
    // 生成 tooltip 内容
    let content = getTooltipContent(row, fieldKey, col)

    // 如果内容为空，尝试从原始值获取
    if (!content || content === '-' || content === '') {
      const rawValue = row[fieldKey]
      if (rawValue != null && rawValue !== '') {
        content = String(rawValue)
      }
    }

    // 如果还是没有内容，就不显示tooltip
    if (!content || content === '-' || content === '') {
      return
    }

    tooltipContent.value = content
    tooltipVisible.value = true
    updateTooltipPosition(event)
  }, 200) // 200ms延迟，减少频繁触发
}

function updateTooltipPosition(event: MouseEvent): void {
  if (!tooltipVisible.value) return

  // 计算更合适的位置，避免tooltip超出视口
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const tooltipWidth = 400 // 最大宽度
  const tooltipHeight = 50 // 估计高度

  let x = event.clientX + 12
  let y = event.clientY + 20

  // 确保tooltip不会超出右侧边界
  if (x + tooltipWidth > viewportWidth) {
    x = Math.max(0, viewportWidth - tooltipWidth - 10)
  }

  // 确保tooltip不会超出底部边界
  if (y + tooltipHeight > viewportHeight) {
    y = event.clientY - tooltipHeight - 10
  }

  tooltipPosition.x = x
  tooltipPosition.y = y
}

function hideTooltip(): void {
  // 使用延迟隐藏，避免鼠标在单元格间快速移动时tooltip闪烁
  if (tooltipHideTimer.value) {
    clearTimeout(tooltipHideTimer.value)
  }

  tooltipHideTimer.value = window.setTimeout(() => {
    tooltipVisible.value = false
    tooltipContent.value = ''
    tooltipHideTimer.value = null
  }, 100)

  // 清除显示定时器
  if (tooltipDebounceTimer.value) {
    clearTimeout(tooltipDebounceTimer.value)
    tooltipDebounceTimer.value = null
  }
}

function getTooltipContent(row: any, key: string, col: Column): string {
  // 优先使用列的自定义tooltip格式化函数
  if (col.tooltipFormatter && typeof col.tooltipFormatter === 'function') {
    return col.tooltipFormatter(row, key)
  }

  // 特殊处理扫描结果列
  if (col.type === 'scan-result') {
    const originalAssetName = row[key] || '暂无扫描结果'
    let content = originalAssetName
    if (row.matchScore) {
      content += `\n匹配度: ${(row.matchScore * 100).toFixed(0)}%`
    }
    return content
  }

  // 对于select类型，尝试显示友好的标签
  if (col.type === 'select') {
    // 如果有显示字段映射，优先使用
    if (col.displayField && row[col.displayField]) {
      return row[col.displayField]
    }

    // 如果列配置有options，尝试找到对应的label
    if (col.options && Array.isArray(col.options)) {
      const option = col.options.find((opt: Option) => String(opt.value) === String(row[key]))
      if (option && option.label) {
        return option.label
      }
    }

    // 从 store 映射中获取 options
    const getOptions = storeMapping[key]
    if (getOptions && typeof getOptions === 'function') {
      const options = getOptions()
      if (options && options.length > 0) {
        // 确保值的比较是字符串类型，避免类型不匹配
        const option = options.find((opt: Option) => String(opt.value) === String(row[key]))
        if (option && option.label) {
          return option.label
        }
      }
    }

    // 如果还是找不到，检查是否有通用的匹配规则
    if (key.includes('assetName') || key.includes('AssetName')) {
      const options = storeMapping.assetNameId()
      if (options && options.length > 0) {
        const option = options.find((opt: Option) => String(opt.value) === String(row[key]))
        if (option && option.label) {
          return option.label
        }
      }
    }

    // 否则显示原始值
    return row[key] ?? '-'
  }

  // 根据列类型进行格式化
  if (col.type === 'number' || col.type === 'money' || col.type === 'amount') {
    const formatted = formatAmount(row[key])
    const unit = getUnitSymbol(row.unitValue) || row.unitValue
    return formatted + (unit ? ` ${unit}` : '')
  }

  if (col.type === 'date' || col.type === 'time' || col.type === 'datetime') {
    return formatDate(row[key], col.type)
  }

  // 处理数组类型的数据
  if (Array.isArray(row[key])) {
    return row[key].join(', ')
  }

  // 处理对象类型的数据
  if (typeof row[key] === 'object' && row[key] !== null) {
    return JSON.stringify(row[key], null, 2)
  }

  // 处理布尔值
  if (typeof row[key] === 'boolean') {
    return row[key] ? '是' : '否'
  }

  // 如果列指定了显示字段映射
  if (col.displayField && row[col.displayField]) {
    return row[col.displayField]
  }

  // 默认返回原始值
  const value = row[key]
  return value != null ? String(value) : '-'
}

const unitSymbolMap: Record<string, string> = {
  '人民币': '¥',
  '美元': '$',
  '欧元': '€',
}

function getUnitSymbol(unit?: string): string | undefined {
  return unit ? unitSymbolMap[unit] || unit : undefined
}

// 格式化单位
function formatCell(row: any, key: string, col: Column): string {
  // 对于 select 类型，也需要显示 label
  if (col.type === 'select') {
    // 如果有显示字段映射，优先使用
    if (col.displayField && row[col.displayField]) {
      return row[col.displayField]
    }

    // 如果列配置有options
    if (col.options && Array.isArray(col.options)) {
      const option = col.options.find((opt: Option) => String(opt.value) === String(row[key]))
      if (option && option.label) {
        return option.label
      }
    }

    // 从 store 映射中获取
    const getOptions = storeMapping[key]
    if (getOptions && typeof getOptions === 'function') {
      const options = getOptions()
      if (options && options.length > 0) {
        const option = options.find((opt: Option) => String(opt.value) === String(row[key]))
        if (option && option.label) {
          return option.label
        }
      }
    }

    // 返回原始值
    return row[key] ?? '-'
  }

  if (col.type === 'number' || col.type === 'money' || col.type === 'amount') {
    const unit = getUnitSymbol(row.unitValue) || row.unitValue
    return formatAmount(row[key]) + (unit ? ` ${unit}` : '')
  }

  if (col.type === 'date' || col.type === 'time' || col.type === 'datetime') {
    return formatDate(row[key], col.type)
  }

  // 如果列有自定义格式化函数
  if (col.formatter && typeof col.formatter === 'function') {
    return col.formatter(row, key)
  }

  // 处理布尔值
  if (typeof row[key] === 'boolean') {
    return row[key] ? '是' : '否'
  }

  // 处理数组
  if (Array.isArray(row[key])) {
    return row[key].join(', ')
  }

  return row[key] ?? '-'
}

function formatAmount(value: any): string {
  if (value == null || isNaN(value)) return '0.00'
  const abs = Math.abs(value)
  return (value < 0 ? '-' : '') + abs.toFixed(2)
}

function formatDate(dateStr: any, type: string = 'date'): string {
  if (!dateStr) return '-'

  try {
    const date = new Date(dateStr)
    if (isNaN(date.getTime())) return dateStr // 如果不是有效日期，返回原始值

    switch (type) {
      case 'datetime':
        return date.toLocaleString('zh-CN')
      case 'time':
        return date.toLocaleTimeString('zh-CN')
      default:
        return date.toLocaleDateString('zh-CN')
    }
  } catch (e) {
    return dateStr // 出错时返回原始值
  }
}

function handleEdit(row: any, index: number): void {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === row)
  emit('edit', row, originalIndex)
}

function handleDelete(row: any, index: number): void {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === row)
  emit('delete', row, originalIndex)
}

// 清理定时器函数
function clearAllTimers(): void {
  if (tooltipDebounceTimer.value) {
    clearTimeout(tooltipDebounceTimer.value)
    tooltipDebounceTimer.value = null
  }

  if (tooltipHideTimer.value) {
    clearTimeout(tooltipHideTimer.value)
    tooltipHideTimer.value = null
  }

  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
}

// 监听数据变化，重新检查是否需要滚动
watch(() => sortedData.value, async () => {
  await nextTick()
  checkScrollable()
}, {deep: true})

watch(() => props.loading, async () => {
  await nextTick()
  checkScrollable()
})

onMounted(() => {
  nextTick(() => {
    checkScrollable()
  })
})

onUnmounted(() => {
  clearAllTimers()
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
})
</script>
