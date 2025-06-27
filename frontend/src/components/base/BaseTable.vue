<!--src/components/base/BaseTable.vue-->
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

              <!-- 排序图标 - 使用SVG -->
              <div v-if="col.sortable" class="flex-shrink-0 ml-1">
                <!-- 升序图标 -->
                <svg
                    v-if="sortKey === col.key && sortOrder === 'asc'"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    class="text-green-600"
                >
                  <path d="m18 15-6-6-6 6"/>
                </svg>

                <!-- 降序图标 -->
                <svg
                    v-else-if="sortKey === col.key && sortOrder === 'desc'"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    class="text-green-600"
                >
                  <path d="m6 9 6 6 6-6"/>
                </svg>

                <!-- 可排序但未排序图标 -->
                <svg
                    v-else
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
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
                  @mousemove="handleCellMouseMove($event)"
                  @mouseleave="handleCellMouseLeave(col)"
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
                  @mousemove="handleCellMouseMove($event)"
                  @mouseleave="handleCellMouseLeave(col)"
              >
                  <span class="block truncate">
                    {{ formatCell(row, col.key) }}
                  </span>
              </div>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Tooltip -->
    <div
        v-if="tooltipVisible"
        class="fixed pointer-events-none select-none bg-gray-700 text-white text-xs rounded px-2 py-1 whitespace-nowrap shadow-lg"
        :style="{
          top: tooltipPosition.y + 'px',
          left: tooltipPosition.x + 'px',
          zIndex: 9999,
          maxWidth: '320px',
          wordWrap: 'break-word',
          whiteSpace: 'pre-wrap'
        }"
    >
      {{ tooltipContent }}
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, markRaw, computed, nextTick, onMounted, watch } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'
// 编辑器组件
import TextEditor from './editors/TextEditor.vue'
import NumberEditor from './editors/NumberEditor.vue'
import SelectEditor from './editors/SelectEditor.vue'
import DateEditor from './editors/DateEditor.vue'

const props = defineProps({
  columns: {type: Array, required: true},
  data: {type: Array, default: () => []},
  loading: {type: Boolean, default: false},
  editable: {type: Boolean, default: false}
})

const emit = defineEmits(['edit', 'delete', 'cell-change', 'cell-blur'])

const DEFAULT_WIDTH = 100
const columnWidths = reactive({})
const activeRowIndex = ref(null)
const contentRef = ref()
const isContentScrollable = ref(false)

// 排序状态
const sortKey = ref(null)
const sortOrder = ref(null) // 'asc' | 'desc' | null

// 排序后的数据
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

// 智能比较函数
function compareValues(aVal, bVal, order) {
  // 处理空值
  if (aVal == null && bVal == null) return 0
  if (aVal == null) return order === 'asc' ? 1 : -1
  if (bVal == null) return order === 'asc' ? -1 : 1

  // 先尝试日期比较（因为日期字符串也可能被识别为字符串）
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
function compareDates(aVal, bVal, order) {
  // 将值转为字符串
  const aStr = String(aVal).trim()
  const bStr = String(bVal).trim()

  // 检查是否符合日期格式
  const datePatterns = [
    /^\d{4}-\d{2}-\d{2}$/,                    // YYYY-MM-DD
    /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/,     // YYYY-MM-DD HH:mm
    /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}$/ // YYYY-MM-DD HH:mm:ss
  ]

  const isDate = (str) => datePatterns.some(pattern => pattern.test(str))

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

// 计算是否需要固定表头
const shouldStickyHeader = computed(() => {
  return isContentScrollable.value && sortedData.value.length > 0 && !props.loading
})

// 检查内容是否可滚动
function checkScrollable() {
  if (!contentRef.value) return

  const element = contentRef.value
  isContentScrollable.value = element.scrollHeight > element.clientHeight
}

// 处理排序
function handleSort(key) {
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

// 初始化列宽
props.columns.forEach(col => {
  columnWidths[col.key] = col.defaultWidth || DEFAULT_WIDTH
})

// 编辑器组件映射
const editorComponents = {
  text: markRaw(TextEditor),
  number: markRaw(NumberEditor),
  select: markRaw(SelectEditor),
  date: markRaw(DateEditor)
}

// 判断列是否包含下拉框
function hasDropdown(col) {
  return col.key === 'assetNameId'
}

// 设置活动行
function setActiveRow(rowIndex) {
  activeRowIndex.value = rowIndex
}

// 清除活动行
function clearActiveRow() {
  activeRowIndex.value = null
}

// 判断列是否可编辑
function isEditable(col) {
  return props.editable && col.editable !== false && col.type && col.type !== 'custom'
}

// 获取编辑器组件
function getEditorComponent(col) {
  return editorComponents[col.type] || editorComponents.text
}

// 处理单元格变更
function handleCellChange(rowIndex, key, value) {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex])
  emit('cell-change', props.data[originalIndex], key, value, originalIndex)
}

// 处理单元格失焦
function handleCellBlur(rowIndex, key) {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex])
  emit('cell-blur', props.data[originalIndex], key, originalIndex)
}

// 获取表头对齐样式
function getHeaderAlignClass(col) {
  if (col.headerAlign) {
    return `text-${col.headerAlign}`
  }
  if (col.key === 'actions') return 'text-center'
  if (['amount', 'count', 'price', 'money'].includes(col.key)) return 'text-right'
  if (['acquireTime', 'finishTime', 'date', 'time'].includes(col.key)) return 'text-center'
  return 'text-left'
}

function getHeaderTextAlignClass(col) {
  const alignClass = getHeaderAlignClass(col)
  if (alignClass === 'text-right') return 'justify-end'
  if (alignClass === 'text-center') return 'justify-center'
  return 'justify-start'
}

function getCellAlignClass(col) {
  if (col.align) {
    return `text-${col.align}`
  }
  if (col.key === 'actions') return 'text-right'
  if (['amount', 'count', 'price', 'money'].includes(col.key)) return 'text-right'
  if (['acquireTime', 'finishTime', 'date', 'time'].includes(col.key)) return 'text-center'
  return 'text-left'
}

// 拖拽列宽调整
let resizingKey = null
let startX = 0
let startWidth = 0
let resizeTimer = null

function doResize(e) {
  if (!resizingKey) return
  if (resizeTimer) return
  resizeTimer = setTimeout(() => {
    const delta = e.pageX - startX
    const newWidth = Math.max(60, startWidth + delta)
    columnWidths[resizingKey] = newWidth
    resizeTimer = null
  }, 16)
}

function startResize(e, key) {
  resizingKey = key
  startX = e.pageX
  startWidth = columnWidths[key]
  window.addEventListener('mousemove', doResize)
  window.addEventListener('mouseup', stopResize)
}

function stopResize() {
  resizingKey = null
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
}

function resetColumnWidth(key) {
  const col = props.columns.find(c => c.key === key)
  columnWidths[key] = col?.defaultWidth || DEFAULT_WIDTH
}

// Tooltip 控制
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipPosition = reactive({x: 0, y: 0})

function handleCellMouseEnter(rowIndex, fieldKey, event, row, col) {
  // 跳过操作列
  if (col.actions || col.key === 'actions') {
    return
  }

  // 如果是下拉框且处于活动状态，跳过
  if (hasDropdown(col) && activeRowIndex.value === rowIndex) {
    return
  }

  // 生成 tooltip 内容
  let content = ''

  // 特殊处理扫描结果列
  if (fieldKey === 'assetName' || fieldKey === 'assetNameDisplay') {
    const originalAssetName = row.assetName || row.assetNameDisplay || '暂无扫描结果'
    content = originalAssetName
    if (row.matchScore) {
      content += `\n匹配度: ${(row.matchScore * 100).toFixed(0)}%`
    }
  } else {
    content = formatTooltipContent(row, fieldKey)
  }

  if (!content || content === '-') {
    return
  }

  tooltipContent.value = content
  tooltipVisible.value = true
  handleCellMouseMove(event)
}

function handleCellMouseMove(event) {
  if (!tooltipVisible.value) return

  tooltipPosition.x = event.clientX + 12
  tooltipPosition.y = event.clientY + 20
}

function handleCellMouseLeave(col) {
  tooltipVisible.value = false
  tooltipContent.value = ''
}

function formatTooltipContent(row, key) {
  if (['amount', 'count'].includes(key)) {
    const formatted = formatAmount(row[key])
    return formatted + (row.unitValue ? ` ${row.unitValue}` : '')
  }
  if (['acquireTime', 'finishTime'].includes(key)) {
    return formatDate(row[key])
  }

  // 处理资产名称ID显示
  if (key === 'assetNameId' && row.assetNameIdDisplay) {
    return row.assetNameIdDisplay
  }

  return row[key] ?? '-'
}

const unitSymbolMap = {
  '人民币': '¥',
  '美元': '$',
  '欧元': '€',
}
// 格式化单位
function formatCell(row, key) {
  if (key === 'amount' || key === 'count') {
    const unit = unitSymbolMap[row.unitValue] || row.unitValue
    return formatAmount(row[key]) + (unit ? ` ${unit}` : '')
  }
  if (key === 'acquireTime' || key === 'finishTime') {
    return formatDate(row[key])
  }
  return row[key] ?? '-'
}


function formatAmount(value) {
  if (value == null || isNaN(value)) return '0.00'
  const abs = Math.abs(value)
  return (value < 0 ? '-' : '') + abs.toFixed(2)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

function handleEdit(row, index) {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === row)
  emit('edit', row, originalIndex)
}

function handleDelete(row, index) {
  // 需要找到原始数据中的索引
  const originalIndex = props.data.findIndex(item => item === row)
  emit('delete', row, originalIndex)
}

// 监听数据变化，重新检查是否需要滚动
watch(() => sortedData.value, async () => {
  await nextTick()
  checkScrollable()
}, { deep: true })

watch(() => props.loading, async () => {
  await nextTick()
  checkScrollable()
})

onMounted(() => {
  nextTick(() => {
    checkScrollable()
  })
})
</script>