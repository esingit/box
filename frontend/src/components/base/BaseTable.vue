<template>
  <div class="table-container overflow-auto border border-gray-200 rounded-xl">
    <table class="min-w-full border-collapse table-fixed text-sm text-gray-800">
      <thead class="bg-gray-50 select-none">
      <tr>
        <th
            v-for="(col, idx) in columns"
            :key="col.key"
            class="px-3 py-2 font-medium text-left whitespace-nowrap relative group"
            :style="{ width: columnWidths[col.key] + 'px' }"
        >
          <div class="flex items-center justify-between">
            {{ col.label }}
            <div
                v-if="col.resizable && idx < columns.length - 1"
                class="absolute right-0 top-0 h-full w-1 cursor-col-resize group-hover:bg-gray-300"
                @mousedown.prevent="startResize($event, col.key)"
                @dblclick.prevent="resetColumnWidth(col.key)"
            ></div>
          </div>
        </th>
      </tr>
      </thead>

      <tbody>
      <!-- 加载中 -->
      <tr v-if="loading">
        <td :colspan="columns.length" class="py-8">
          <slot name="loading">
            <div class="space-y-2">
              <div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div>
            </div>
          </slot>
        </td>
      </tr>

      <!-- 空状态 -->
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

      <!-- 数据行 -->
      <tr
          v-else
          v-for="(row, rowIndex) in data"
          :key="row.id || rowIndex"
          class="hover:bg-gray-50 transition-colors"
      >
        <td
            v-for="col in columns"
            :key="col.key"
            class="px-3 py-2 truncate whitespace-nowrap"
            :style="{ width: columnWidths[col.key] + 'px' }"
            @mouseenter="showTooltip(rowIndex, col.key)"
            @mouseleave="hideTooltip"
        >
          <!-- 操作列 -->
          <template v-if="col.key === 'actions' && col.actions">
            <div class="text-center">
              <BaseActions :record="row" @edit="handleEdit(row)" @delete="handleDelete(row)" />
            </div>
          </template>

          <!-- 普通数据列 -->
          <template v-else>
            {{ formatCell(row, col.key) }}
          </template>

          <!-- Tooltip -->
          <Tooltip
              v-if="tooltipVisible && tooltipIndex === rowIndex && tooltipField === col.key && tooltipContent"
              :content="tooltipContent"
          />
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import BaseActions from '@/components/base/BaseActions.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import Tooltip from '@/components/base/BaseNotice.vue'

const props = defineProps({
  columns: {
    type: Array,
    required: true
    // 示例：[{ key: 'name', label: '名称', resizable: true, defaultWidth: 200 }]
  },
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit', 'delete'])

// 列宽初始化
const DEFAULT_WIDTH = 100
const columnWidths = reactive({})
props.columns.forEach(col => {
  columnWidths[col.key] = col.defaultWidth || DEFAULT_WIDTH
})

// 拖拽列宽
let resizingKey = null
let startX = 0
let startWidth = 0

function startResize(e, key) {
  resizingKey = key
  startX = e.pageX
  startWidth = columnWidths[key]
  window.addEventListener('mousemove', doResize)
  window.addEventListener('mouseup', stopResize)
}

function doResize(e) {
  if (!resizingKey) return
  const delta = e.pageX - startX
  columnWidths[resizingKey] = Math.max(60, startWidth + delta)
}

function stopResize() {
  resizingKey = null
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
}

function resetColumnWidth(key) {
  const col = props.columns.find(c => c.key === key)
  columnWidths[key] = col?.defaultWidth || DEFAULT_WIDTH
}

// Tooltip 控制
const tooltipVisible = ref(false)
const tooltipIndex = ref(null)
const tooltipField = ref(null)
const tooltipContent = ref('')

function showTooltip(index, field) {
  tooltipIndex.value = index
  tooltipField.value = field
  tooltipVisible.value = true

  const row = props.data[index]
  if (!row) {
    tooltipVisible.value = false
    return
  }

  switch (field) {
    case 'amount':
      tooltipContent.value = formatAmount(row.amount) + (row.unitValue ? ` ${row.unitValue}` : '')
      break
    case 'time':
      tooltipContent.value = formatDate(row.acquireTime)
      break
    default:
      tooltipContent.value = row[field] || '-'
      break
  }
}

function hideTooltip() {
  tooltipVisible.value = false
  tooltipIndex.value = null
  tooltipField.value = null
  tooltipContent.value = ''
}

// 格式化内容
function formatCell(row, key) {
  if (key === 'amount') return formatAmount(row.amount) + (row.unitValue ? ` ${row.unitValue}` : '')
  if (key === 'time') return formatDate(row.acquireTime)
  return row[key] ?? '-'
}

function formatAmount(amount) {
  if (amount == null) return '0.00'
  const abs = Math.abs(amount)
  return (amount < 0 ? '-' : '') + abs.toFixed(2)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

// 事件封装
function handleEdit(row) {
  emit('edit', row)
}
function handleDelete(row) {
  emit('delete', row)
}
</script>
