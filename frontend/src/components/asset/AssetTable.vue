<template>
  <div class="table-container overflow-auto border border-gray-200 rounded-xl">
    <table class="min-w-full border-collapse table-fixed text-sm text-gray-800">
    <thead class="bg-gray-50 select-none">
      <tr>
        <th
            v-for="(header, index) in tableHeaders"
            :key="header.key"
            class="px-3 py-2 font-medium text-left whitespace-nowrap relative group"
            :style="{ width: columnWidths[index] + 'px' }"
        >
          <div class="flex items-center justify-between">
            {{ header.label }}
            <div
                v-if="index < tableHeaders.length - 1"
                class="absolute right-0 top-0 h-full w-1 cursor-col-resize group-hover:bg-gray-300"
                @mousedown.prevent="startResize($event, index)"
                @dblclick.prevent="resetColumnWidth(index)"
            ></div>
          </div>
        </th>
      </tr>
      </thead>

      <tbody>

      <!-- 加载中骨架 -->
      <tr v-if="loading">
        <td :colspan="tableHeaders.length" class="py-8">
          <div class="space-y-2">
            <div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div>
          </div>
        </td>
      </tr>

      <!-- 无数据 -->
      <tr v-else-if="!records.length">
        <td :colspan="tableHeaders.length" class="py-8 text-center text-gray-400">
          <BaseEmptyState
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
          />
        </td>
      </tr>

      <!-- 数据行 -->
      <tr
          v-else
          v-for="(record, index) in records"
          :key="record.id || index"
          class="hover:bg-gray-50 transition-colors"
      >
        <td
            v-for="header in tableHeaders"
            :key="header.key"
            class="px-3 py-2 truncate whitespace-nowrap"
            :style="{ width: columnWidths[header.key] + 'px' }"
            @mouseenter="showTooltip(index, header.key)"
            @mouseleave="hideTooltip"
        >
          <template v-if="header.key === 'amount'">
              <span :class="record.amount < 0 ? 'text-red-600' : ''">
                {{ formatAmount(record.amount) }}
              </span>
            {{ record.unitValue || '' }}
          </template>

          <template v-else-if="header.key === 'actions'">
            <div class="text-center">
              <BaseActions
                  :record="record"
                  type="asset"
                  @edit="$emit('edit', record.id)"
                  @delete="$emit('delete', record)"
              />
            </div>
          </template>

          <template v-else-if="header.key === 'time'">
            {{ formatDate(record.acquireTime) }}
          </template>

          <template v-else>
            {{ record[header.key] || '-' }}
          </template>

          <!-- Tooltip 只显示当前激活列 -->
          <Tooltip
              v-if="tooltipVisible && tooltipIndex === index && tooltipField === header.key && tooltipContent"
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
import Tooltip from '@/components/base/BaseNotice.vue'
import BaseEmptyState from "@/components/base/BaseEmptyState.vue";

const props = defineProps({
  records: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['edit', 'delete'])

// 列定义，支持宽度拖拽
const DEFAULT_COLUMN_WIDTHS = {
  assetName: 250,
  assetType: 120,
  amount: 130,
  location: 120,
  time: 120,
  remark: 150,
  actions: 100
}

const tableHeaders = [
  { key: 'assetName', label: '资产名称', resizable: true },
  { key: 'assetType', label: '类型', resizable: true },
  { key: 'amount', label: '金额', resizable: true },
  { key: 'location', label: '位置', resizable: true },
  { key: 'time', label: '时间', resizable: true },
  { key: 'remark', label: '备注', resizable: true },
  { key: 'actions', label: '操作', resizable: false }
]

const columnWidths = reactive({ ...DEFAULT_COLUMN_WIDTHS })

// 拖拽调整列宽逻辑
let resizingKey = null
let startX = 0
let startWidth = 0

function startResize(event, key) {
  resizingKey = key
  startX = event.pageX
  startWidth = columnWidths[key]
  window.addEventListener('mousemove', doResize)
  window.addEventListener('mouseup', stopResize)
}

function doResize(event) {
  if (!resizingKey) return
  const delta = event.pageX - startX
  const newWidth = Math.max(60, startWidth + delta)
  columnWidths[resizingKey] = newWidth
}

function stopResize() {
  resizingKey = null
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
}

function resetColumnWidth(key) {
  columnWidths[key] = DEFAULT_COLUMN_WIDTHS[key]
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

  const record = props.records[index]
  if (!record) {
    tooltipVisible.value = false
    return
  }

  switch (field) {
    case 'amount':
      tooltipContent.value = `${formatAmount(record.amount)} ${record.unitValue || ''}`.trim()
      break
    case 'time':
      tooltipContent.value = formatDate(record.acquireTime)
      break
    default:
      tooltipContent.value = record[field] || '-'
      break
  }
}

function hideTooltip() {
  tooltipVisible.value = false
  tooltipIndex.value = null
  tooltipField.value = null
  tooltipContent.value = ''
}

function formatAmount(amount) {
  if (amount == null) return '0.00'
  const abs = Math.abs(amount)
  const formatted = abs.toFixed(2)
  return (amount < 0 ? '-' : '') + formatted
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}
</script>