<!-- /components/fitness/FitnessTable.vue -->
<template>
  <div class="table-container overflow-auto border border-gray-200 rounded-md">
    <table class="min-w-full border-collapse table-fixed text-sm text-gray-700">
      <thead class="bg-gray-100 select-none">
      <tr>
        <th v-for="(header, index) in tableHeaders"
            :key="header.key"
            :class="['px-3 py-2 font-medium text-left whitespace-nowrap', header.class]"
            :style="{ width: columnWidths[index] + 'px' }"
        >
          <div class="relative flex items-center">
            {{ header.label }}
            <div v-if="index < tableHeaders.length - 1"
                 class="column-resizer absolute right-0 top-0 h-full w-1 cursor-col-resize hover:bg-blue-500"
                 @mousedown.prevent="startResize($event, index)"
                 @dblclick.prevent="resetColumnWidth(index)">
            </div>
          </div>
        </th>
      </tr>
      </thead>
      <tbody>
      <tr v-if="loading">
        <td :colspan="tableHeaders.length" class="py-8">
          <div class="space-y-2">
            <div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div>
          </div>
        </td>
      </tr>

      <tr v-else-if="records.length === 0">
        <td :colspan="tableHeaders.length" class="py-8 text-center text-gray-400">
          <EmptyState
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
          />
        </td>
      </tr>

      <tr v-else v-for="(record, idx) in records" :key="record.id"
          class="hover:bg-gray-50 transition-colors duration-150">
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[0] + 'px' }"
            @mouseenter="showTooltip($event, record.typeValue)"
            @mouseleave="hideTooltip">
          {{ record.typeValue }}
        </td>
        <td class="px-3 py-2 text-right"
            :style="{ width: columnWidths[1] + 'px' }"
            @mouseenter="showTooltip($event, record.count + ' ' + record.unitValue)"
            @mouseleave="hideTooltip">
          {{ record.count }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[2] + 'px' }"
            @mouseenter="showTooltip($event, record.unitValue)"
            @mouseleave="hideTooltip">
          {{ record.unitValue }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[3] + 'px' }"
            @mouseenter="showTooltip($event, formatDate(record.finishTime))"
            @mouseleave="hideTooltip">
          {{ formatDate(record.finishTime) }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[4] + 'px' }"
            @mouseenter="showTooltip($event, record.remark)"
            @mouseleave="hideTooltip">
          <span v-if="record.remark">{{ record.remark }}</span>
          <span v-else class="text-gray-400">-</span>
        </td>
        <td class="px-3 py-2 text-center"
            :style="{ width: columnWidths[5] + 'px' }">
          <RecordActions
              :record="record"
              type="fitness"
              @edit="$emit('edit', record.id)"
              @delete="$emit('delete', record.id)"
          />
        </td>
      </tr>
      </tbody>
    </table>

    <div v-if="tooltip.visible"
         :style="tooltip.style"
         class="fixed z-50 max-w-xs rounded-md bg-gray-900 text-white text-xs py-1 px-2 shadow-lg pointer-events-none select-none transition-opacity duration-150">
      {{ tooltip.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import EmptyState from '@/components/base/EmptyState.vue'
import RecordActions from '@/components/base/RecordActions.vue'

// 接收 props
const props = defineProps<{ records: any[]; loading: boolean }>()
const emit = defineEmits<{
  (e: 'edit', id: number): void
  (e: 'delete', id: number): void
}>()

const tableHeaders = [
  { key: 'type', label: '类型', class: '' },
  { key: 'count', label: '数量', class: '' },
  { key: 'unit', label: '单位', class: '' },
  { key: 'date', label: '日期', class: '' },
  { key: 'remark', label: '备注', class: '' },
  { key: 'actions', label: '操作', class: '' }
]

const DEFAULT_COLUMN_WIDTHS = {
  type: 120,
  count: 100,
  unit: 100,
  date: 120,
  remark: 200,
  actions: 100
}

const columnWidths = ref(Object.values(DEFAULT_COLUMN_WIDTHS))
const tooltip = ref({ visible: false, content: '', style: { top: '0px', left: '0px', opacity: '0' } })

function saveColumnWidths() {
  localStorage.setItem(
      'fitnessTableColumnWidths',
      JSON.stringify(Object.fromEntries(
          tableHeaders.map((h, i) => [h.key, columnWidths.value[i]])
      ))
  )
}
function loadColumnWidths() {
  const saved = localStorage.getItem('fitnessTableColumnWidths')
  if (saved) {
    const widths = JSON.parse(saved)
    columnWidths.value = tableHeaders.map(h => widths[h.key] || DEFAULT_COLUMN_WIDTHS[h.key])
  }
}

let resizing = false
let resizingIndex = -1
let startX = 0
let startWidth = 0

function startResize(e: MouseEvent, index: number) {
  resizing = true
  resizingIndex = index
  startX = e.pageX
  startWidth = columnWidths.value[index]
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  ;(e.target as HTMLElement).classList.add('column-resizing')
}
function handleResize(e: MouseEvent) {
  if (!resizing) return
  const diff = e.pageX - startX
  const newWidth = Math.max(60, startWidth + diff)
  columnWidths.value[resizingIndex] = newWidth
  saveColumnWidths()
}
function stopResize() {
  if (!resizing) return
  resizing = false
  resizingIndex = -1
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.querySelectorAll('.column-resizing').forEach(el => el.classList.remove('column-resizing'))
}
function resetColumnWidth(index: number) {
  columnWidths.value[index] = DEFAULT_COLUMN_WIDTHS[tableHeaders[index].key]
  saveColumnWidths()
}

function showTooltip(e: MouseEvent, content: string) {
  if (!content) return hideTooltip()
  tooltip.value.content = content
  tooltip.value.visible = true
  nextTick(() => {
    tooltip.value.style.left = `${e.clientX}px`
    tooltip.value.style.top = `${e.clientY - 10}px`
    tooltip.value.style.opacity = '1'
  })
}
function hideTooltip() {
  tooltip.value.visible = false
  tooltip.value.style.opacity = '0'
}

onMounted(() => {
  loadColumnWidths()
  window.addEventListener('scroll', hideTooltip)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', hideTooltip)
})

function formatDate(str: string) {
  return str?.slice(0, 10) || '-'
}
</script>