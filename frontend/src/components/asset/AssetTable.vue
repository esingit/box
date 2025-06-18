<template>
  <div class="overflow-auto bg-white border border-gray-200 rounded-md shadow-sm">
    <table class="min-w-full text-gray-900">
      <thead class="bg-gray-50 border-b border-gray-200">
      <tr>
        <th v-for="header in tableHeaders" :key="header.key"
            class="px-4 py-2 text-left font-semibold text-sm select-none whitespace-nowrap"
            :style="{ minWidth: header.minWidth }">
          <div class="flex items-center justify-between">
            <span>{{ header.label }}</span>
            <div
                v-if="header.resizable"
                class="w-1 cursor-col-resize h-6 ml-2 bg-gray-300 hover:bg-gray-500"
                @mousedown.prevent="startResize($event, header.key)"
            ></div>
          </div>
        </th>
      </tr>
      </thead>
      <tbody>
      <tr v-if="records.length === 0" class="text-center text-gray-500">
        <td :colspan="tableHeaders.length" class="py-10">
          暂无资产记录，点击上方按钮添加
        </td>
      </tr>
      <tr v-for="(record, idx) in records" :key="record.id || idx" class="hover:bg-gray-50">
        <td class="px-4 py-2 whitespace-nowrap" :style="{ minWidth: columnWidths.assetName + 'px' }"
            @mouseenter="showTooltip(idx, 'assetName')" @mouseleave="hideTooltip">
          {{ record.assetName || '-' }}
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'assetName'"
                   :content="record.assetName"/>
        </td>
        <td class="px-4 py-2 whitespace-nowrap" :style="{ minWidth: columnWidths.assetType + 'px' }"
            @mouseenter="showTooltip(idx, 'assetType')" @mouseleave="hideTooltip">
          {{ record.assetTypeValue || '-' }}
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'assetType'"
                   :content="record.assetTypeValue"/>
        </td>
        <td class="px-4 py-2 text-right whitespace-nowrap" :style="{ minWidth: columnWidths.amount + 'px' }"
            @mouseenter="showTooltip(idx, 'amount')" @mouseleave="hideTooltip">
          <span :class="record.amount < 0 ? 'text-red-600' : ''">{{ formatAmount(record.amount) }}</span>
          {{ record.unitValue || '' }}
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'amount'"
                   :content="formatAmount(record.amount) + ' ' + (record.unitValue || '')"/>
        </td>
        <td class="px-4 py-2 whitespace-nowrap" :style="{ minWidth: columnWidths.location + 'px' }"
            @mouseenter="showTooltip(idx, 'location')" @mouseleave="hideTooltip">
          {{ record.locationValue || '-' }}
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'location'"
                   :content="record.locationValue"/>
        </td>
        <td class="px-4 py-2 whitespace-nowrap" :style="{ minWidth: columnWidths.time + 'px' }"
            @mouseenter="showTooltip(idx, 'time')" @mouseleave="hideTooltip">
          {{ formatDate(record.acquireTime) }}
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'time'"
                   :content="formatDate(record.acquireTime)"/>
        </td>
        <td class="px-4 py-2 whitespace-nowrap" :style="{ minWidth: columnWidths.remark + 'px' }"
            @mouseenter="showTooltip(idx, 'remark')" @mouseleave="hideTooltip">
          <span v-if="record.remark">{{ record.remark }}</span>
          <span v-else class="text-gray-400">-</span>
          <Tooltip v-if="tooltipVisible && tooltipIndex === idx && tooltipField === 'remark'" :content="record.remark"/>
        </td>
        <td class="px-4 py-2 whitespace-nowrap text-center" :style="{ minWidth: columnWidths.actions + 'px' }">
          <BaseActions :record="record" type="asset" @edit="$emit('edit', idx)" @delete="$emit('delete', idx)"/>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import BaseActions from '@/components/base/BaseActions.vue'
import Tooltip from '@/components/base/BaseNotice.vue'

const props = defineProps({
  records: {
    type: Array,
    default: () => [],
  }
})
const emit = defineEmits(['edit', 'delete'])

// 你可以自由调整这里的初始列宽，单位 px
const columnWidths = reactive({
  assetName: 300,
  assetType: 120,
  amount: 130,
  location: 120,
  time: 120,
  remark: 150,
  actions: 100,
})

const tableHeaders = [
  {key: 'assetName', label: '资产名称', minWidth: '300px', resizable: true},
  {key: 'assetType', label: '类型', minWidth: '120px', resizable: true},
  {key: 'amount', label: '金额', minWidth: '130px', resizable: true},
  {key: 'location', label: '位置', minWidth: '120px', resizable: true},
  {key: 'time', label: '时间', minWidth: '120px', resizable: true},
  {key: 'remark', label: '备注', minWidth: '150px', resizable: true},
  {key: 'actions', label: '操作', minWidth: '100px', resizable: false},
]

// 简单的拖拽调整列宽逻辑示例
let resizingColumn = null
let startX = 0
let startWidth = 0

function startResize(event, key) {
  resizingColumn = key
  startX = event.pageX
  startWidth = columnWidths[key]

  window.addEventListener('mousemove', doResize)
  window.addEventListener('mouseup', stopResize)
}

function doResize(event) {
  if (!resizingColumn) return
  const delta = event.pageX - startX
  const newWidth = Math.max(60, startWidth + delta)
  columnWidths[resizingColumn] = newWidth
}

function stopResize() {
  resizingColumn = null
  window.removeEventListener('mousemove', doResize)
  window.removeEventListener('mouseup', stopResize)
}

// tooltip 显示控制
const tooltipVisible = ref(false)
const tooltipIndex = ref(null)
const tooltipField = ref(null)

function showTooltip(index, field) {
  tooltipVisible.value = true
  tooltipIndex.value = index
  tooltipField.value = field
}

function hideTooltip() {
  tooltipVisible.value = false
  tooltipIndex.value = null
  tooltipField.value = null
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

<style scoped>
/* Tooltip 简单样式 */
.cell-tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}
</style>
