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
          {{ header.label }}
          <div
              v-if="index < tableHeaders.length - 1"
              class="absolute right-0 top-0 h-full w-1 cursor-col-resize group-hover:bg-gray-300"
              @mousedown.prevent="startResize($event, index)"
              @dblclick.prevent="resetColumnWidth(index)"
          ></div>
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

      <tr v-else-if="!records.length">
        <td :colspan="tableHeaders.length" class="py-8 text-center text-gray-400">
          <BaseEmptyState
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
          />
        </td>
      </tr>

      <tr
          v-else
          v-for="record in records"
          :key="record.id"
          class="hover:bg-gray-50 transition-colors"
      >
        <td
            v-for="(header, i) in tableHeaders"
            :key="i"
            class="px-3 py-2 truncate"
            :style="{ width: columnWidths[i] + 'px' }"
        >
          <template v-if="header.key === 'type'">
            <span
                @mouseenter="showTooltip($event, record.typeValue)"
                @mouseleave="hideTooltip"
            >
              {{ record.typeValue }}
            </span>
          </template>
          <template v-else-if="header.key === 'count'">
            <span
                @mouseenter="showTooltip($event, record.count + ' ' + record.unitValue)"
                @mouseleave="hideTooltip"
            >
              {{ record.count }}
            </span>
          </template>
          <template v-else-if="header.key === 'unit'">
            <span
                @mouseenter="showTooltip($event, record.unitValue)"
                @mouseleave="hideTooltip"
            >
              {{ record.unitValue }}
            </span>
          </template>
          <template v-else-if="header.key === 'date'">
            <span
                @mouseenter="showTooltip($event, formatDate(record.finishTime))"
                @mouseleave="hideTooltip"
            >
              {{ formatDate(record.finishTime) }}
            </span>
          </template>
          <template v-else-if="header.key === 'remark'">
            <span
                @mouseenter="showTooltip($event, record.remark)"
                @mouseleave="hideTooltip"
            >
              {{ record.remark || '-' }}
            </span>
          </template>
          <template v-else-if="header.key === 'actions'">
            <div class="text-center">
              <BaseActions
                  :record="record"
                  type="fitness"
                  @edit="$emit('edit', record.id)"
                  @delete="$emit('delete', record)"
              />
            </div>
          </template>
        </td>
      </tr>
      </tbody>
    </table>

    <div
        v-if="tooltip.visible"
        :style="tooltip.style"
        class="fixed z-50 max-w-xs rounded-md bg-gray-900 text-white text-xs py-1 px-2 shadow-lg pointer-events-none select-none transition-opacity"
    >
      {{ tooltip.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'

const props = defineProps<{ records: any[]; loading: boolean }>()
const emit = defineEmits<{ (e: 'edit', id: number): void; (e: 'delete', record: any): void }>()

const tableHeaders = [
  { key: 'type', label: '类型' },
  { key: 'count', label: '数量' },
  { key: 'unit', label: '单位' },
  { key: 'date', label: '日期' },
  { key: 'remark', label: '备注' },
  { key: 'actions', label: '操作' }
]

const DEFAULT_COLUMN_WIDTHS: Record<string, number> = {
  type: 120,
  count: 100,
  unit: 100,
  date: 120,
  remark: 200,
  actions: 100
}

const columnWidths = ref<number[]>([])
const tooltip = ref({
  visible: false,
  content: '',
  style: { top: '0px', left: '0px', opacity: '0' }
})

function loadColumnWidths() {
  const saved = localStorage.getItem('fitnessTableColumnWidths')
  if (saved) {
    const parsed = JSON.parse(saved)
    columnWidths.value = tableHeaders.map(h => parsed[h.key] || DEFAULT_COLUMN_WIDTHS[h.key])
  } else {
    columnWidths.value = Object.values(DEFAULT_COLUMN_WIDTHS)
  }
}

function saveColumnWidths() {
  const widths = Object.fromEntries(tableHeaders.map((h, i) => [h.key, columnWidths.value[i]]))
  localStorage.setItem('fitnessTableColumnWidths', JSON.stringify(widths))
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
}

function handleResize(e: MouseEvent) {
  if (!resizing) return
  const diff = e.pageX - startX
  columnWidths.value[resizingIndex] = Math.max(60, startWidth + diff)
  saveColumnWidths()
}

function stopResize() {
  if (!resizing) return
  resizing = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
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

function formatDate(str: string) {
  return str?.slice(0, 10) || '-'
}

onMounted(() => {
  loadColumnWidths()
  window.addEventListener('scroll', hideTooltip)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', hideTooltip)
})
</script>
