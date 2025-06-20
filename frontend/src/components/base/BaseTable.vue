<template>
  <div class="relative overflow-auto border border-gray-200 rounded-xl min-h-[520px] max-h-[520px]">
    <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
      <!-- 表头固定 -->
      <thead class="bg-gray-50 sticky top-0 z-10 block w-full select-none">
      <tr class="table w-full table-fixed">
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

      <tbody class="block w-full">
      <tr v-if="loading" class="table w-full table-fixed">
        <td :colspan="columns.length" class="py-8">
          <slot name="loading">
            <div class="space-y-2">
              <div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div>
            </div>
          </slot>
        </td>
      </tr>

      <tr v-else-if="!data.length" class="table w-full table-fixed">
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
          class="table w-full table-fixed hover:bg-gray-50 transition-colors"
      >
        <td
            v-for="col in columns"
            :key="col.key"
            class="px-3 py-2 truncate whitespace-nowrap"
            :style="{ width: columnWidths[col.key] + 'px' }"
            :class="cellAlignClass(col.key)"
            @mouseenter="col.key !== 'actions' && onMouseEnter(rowIndex, col.key, $event)"
            @mousemove="col.key !== 'actions' && onMouseMove($event)"
            @mouseleave="col.key !== 'actions' && onMouseLeave()"
        >
          <template v-if="col.key === 'actions' && col.actions">
            <div class="flex justify-end">
              <BaseActions
                  :record="row"
                  @edit="handleEdit(row)"
                  @delete="handleDelete(row)"
              />
            </div>
          </template>
          <template v-else>
            {{ formatCell(row, col.key) }}
          </template>
        </td>
      </tr>
      </tbody>
    </table>

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
import { reactive, ref } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'

const props = defineProps({
  columns: { type: Array, required: true },
  data: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['edit', 'delete'])

const DEFAULT_WIDTH = 100
const columnWidths = reactive({})
props.columns.forEach(col => {
  columnWidths[col.key] = col.defaultWidth || DEFAULT_WIDTH
})

// 拖拽列宽
let resizingKey = null
let startX = 0
let startWidth = 0
let resizeTimer = null

function doResize(e) {
  if (!resizingKey) return
  if (resizeTimer) return
  resizeTimer = setTimeout(() => {
    const delta = e.pageX - startX
    columnWidths[resizingKey] = Math.max(60, startWidth + delta)
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
const tooltipPosition = reactive({ x: 0, y: 0 })

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

function handleEdit(row) {
  emit('edit', row)
}
function handleDelete(row) {
  emit('delete', row)
}
</script>
