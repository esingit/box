<!--src/components/base/BaseTable.vue-->
<template>
  <!-- 固定表格容器宽度 -->
  <div class="relative border border-gray-200 rounded-xl" style="min-height: 520px; width: 100%;">
    <!-- 表格头部区域 -->
    <div class="sticky top-0 z-20 bg-gray-50 rounded-t-xl">
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <thead>
        <tr>
          <th
              v-for="(col, idx) in columns"
              :key="col.key"
              class="px-3 py-2 font-medium whitespace-nowrap relative group"
              :style="{ width: columnWidths[col.key] + 'px' }"
              :class="headerAlignClass(col.key)"
          >
            <div :class="['w-full', textAlignClass(col.key), 'select-none truncate pr-2']">
              {{ col.label }}
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
    <div class="relative" style="max-height: 460px; overflow-y: auto;">
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <!-- 隐藏的列宽定义行 -->
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

        <tr v-else-if="!data.length">
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
            v-for="(row, rowIndex) in data"
            :key="row.id || rowIndex"
            class="hover:bg-gray-50 transition-colors"
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
                  // 根据是否包含下拉框来设置overflow
                  overflow: hasDropdown(col) ? 'visible' : 'hidden'
                }"
              :class="cellAlignClass(col.key)"
              @mouseenter="!isEditable(col) && col.key !== 'actions' && onMouseEnter(rowIndex, col.key, $event)"
              @mousemove="!isEditable(col) && col.key !== 'actions' && onMouseMove($event)"
              @mouseleave="!isEditable(col) && col.key !== 'actions' && onMouseLeave()"
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
              <!-- 根据是否包含下拉框使用不同的容器样式 -->
              <div
                  :class="[
                    'w-full',
                    hasDropdown(col) ? 'overflow-visible' : 'overflow-hidden'
                  ]"
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

            <!-- 普通显示单元格 - 充分利用空间 -->
            <template v-else>
              <div class="w-full overflow-hidden">
                  <span
                      :title="formatTooltipContent(row, col.key)"
                      class="block truncate"
                  >
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
    <Teleport to="body">
      <div
          v-if="tooltipVisible"
          class="z-50 pointer-events-none select-none bg-gray-700 text-white text-xs rounded px-2 py-1 whitespace-nowrap"
          :style="{ position: 'fixed', top: tooltipPosition.y + 'px', left: tooltipPosition.x + 'px' }"
      >
        {{ tooltipContent }}
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import {reactive, ref, markRaw} from 'vue'
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

// 判断列是否包含下拉框 - 更精确的判断
function hasDropdown(col) {
  return col.type === 'select' || col.key === 'assetNameId' ||
      (col.type === 'custom' && col.key === 'assetNameId')
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
  emit('cell-change', props.data[rowIndex], key, value, rowIndex)
}

// 处理单元格失焦
function handleCellBlur(rowIndex, key) {
  emit('cell-blur', props.data[rowIndex], key, rowIndex)
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

// 对齐类
function textAlignClass(key) {
  if (['amount', 'count'].includes(key)) return 'text-right'
  if (['acquireTime', 'finishTime'].includes(key)) return 'text-center'
  if (key === 'actions') return 'text-center'
  return 'text-left'
}

function headerAlignClass(key) {
  return textAlignClass(key)
}

function cellAlignClass(key) {
  if (key === 'actions') return 'text-right'
  return textAlignClass(key)
}

// Tooltip 控制
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipPosition = reactive({x: 0, y: 0})

let currentRow = null
let currentField = null

function onMouseEnter(index, field, event) {
  const row = props.data[index]
  if (!row) return
  if (currentRow !== index || currentField !== field) {
    tooltipContent.value = formatTooltipContent(row, field)
    currentRow = index
    currentField = field
  }
  tooltipVisible.value = true
  onMouseMove(event)
}

function onMouseMove(event) {
  if (!tooltipVisible.value) return
  tooltipPosition.x = event.clientX + 12
  tooltipPosition.y = event.clientY + 20
}

function onMouseLeave() {
  tooltipVisible.value = false
  currentRow = null
  currentField = null
  tooltipContent.value = ''
}

function formatTooltipContent(row, key) {
  if (['amount', 'count'].includes(key)) {
    return formatAmount(row[key]) + (row.unitValue ? ` ${row.unitValue}` : '')
  }
  if (['acquireTime', 'finishTime'].includes(key)) {
    return formatDate(row[key])
  }
  return row[key] ?? '-'
}

// 格式化显示
function formatCell(row, key) {
  if (key === 'amount' || key === 'count') {
    return formatAmount(row[key]) + (row.unitValue ? ` ${row.unitValue}` : '')
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
  emit('edit', row, index)
}

function handleDelete(row, index) {
  emit('delete', row, index)
}
</script>