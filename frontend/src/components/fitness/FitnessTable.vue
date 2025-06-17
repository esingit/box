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
      <tr v-if="records.length === 0">
        <td :colspan="tableHeaders.length" class="py-8 text-center text-gray-400">
          <EmptyState
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
          />
        </td>
      </tr>
      <tr v-for="(record, idx) in records" :key="record.id || idx"
          class="hover:bg-gray-50 transition-colors duration-150"
      >
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[0] + 'px' }"
            @mouseenter="showTooltip($event, record.typeValue)"
            @mouseleave="hideTooltip"
        >
          {{ record.typeValue }}
        </td>
        <td class="px-3 py-2 text-right"
            :style="{ width: columnWidths[1] + 'px' }"
            @mouseenter="showTooltip($event, record.count + ' ' + record.unitValue)"
            @mouseleave="hideTooltip"
        >
          {{ record.count }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[2] + 'px' }"
            @mouseenter="showTooltip($event, record.unitValue)"
            @mouseleave="hideTooltip"
        >
          {{ record.unitValue }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[3] + 'px' }"
            @mouseenter="showTooltip($event, formatDate(record.finishTime))"
            @mouseleave="hideTooltip"
        >
          {{ formatDate(record.finishTime) }}
        </td>
        <td class="px-3 py-2 truncate"
            :style="{ width: columnWidths[4] + 'px' }"
            @mouseenter="showTooltip($event, record.remark)"
            @mouseleave="hideTooltip"
        >
          <span v-if="record.remark" class="text-gray-700">{{ record.remark }}</span>
          <span v-else class="text-gray-400">-</span>
        </td>
        <td class="px-3 py-2 text-center"
            :style="{ width: columnWidths[5] + 'px' }"
        >
          <RecordActions
              :record="record"
              type="fitness"
              @edit="$emit('edit', idx)"
              @delete="$emit('delete', idx)"
          />
        </td>
      </tr>
      </tbody>
    </table>

    <!-- 通用 Tooltip -->
    <div v-if="tooltip.visible"
         :style="tooltip.style"
         class="fixed z-50 max-w-xs rounded-md bg-gray-900 text-white text-xs py-1 px-2 shadow-lg pointer-events-none select-none transition-opacity duration-150"
    >
      {{ tooltip.content }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import EmptyState from '@/components/base/EmptyState.vue'
import RecordActions from '@/components/base/RecordActions.vue'

const DEFAULT_COLUMN_WIDTHS = {
  type: 120,
  count: 100,
  unit: 100,
  date: 120,
  remark: 200,
  actions: 100
}

const props = defineProps({
  records: {
    type: Array,
    default: () => []   // 这里加默认空数组，防止未传或 undefined
  }
})

const emit = defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'type', label: '类型', class: '' },
  { key: 'count', label: '数量', class: '' },
  { key: 'unit', label: '单位', class: '' },
  { key: 'date', label: '日期', class: '' },
  { key: 'remark', label: '备注', class: '' },
  { key: 'actions', label: '操作', class: '' }
]

const columnWidths = ref(Object.values(DEFAULT_COLUMN_WIDTHS))
const resizing = ref(false)
const resizingIndex = ref(-1)
const startX = ref(0)
const startWidth = ref(0)

const tooltip = ref({
  visible: false,
  content: '',
  style: {
    top: '0px',
    left: '0px',
    opacity: '0'
  }
})

function saveColumnWidths() {
  try {
    const widths = {}
    tableHeaders.forEach((header, index) => {
      widths[header.key] = columnWidths.value[index]
    })
    localStorage.setItem('fitnessTableColumnWidths', JSON.stringify(widths))
  } catch {
    // 忽略存储失败
  }
}

function loadColumnWidths() {
  try {
    const saved = localStorage.getItem('fitnessTableColumnWidths')
    if (saved) {
      const widths = JSON.parse(saved)
      tableHeaders.forEach((header, index) => {
        columnWidths.value[index] = widths[header.key] || DEFAULT_COLUMN_WIDTHS[header.key]
      })
    }
  } catch {
    // 忽略读取失败
  }
}

function startResize(event, index) {
  resizing.value = true
  resizingIndex.value = index
  startX.value = event.pageX
  startWidth.value = columnWidths.value[index]

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  event.target.classList.add('column-resizing')
}

function handleResize(event) {
  if (!resizing.value) return
  const index = resizingIndex.value
  const minWidth = 60
  const diff = event.pageX - startX.value
  const newWidth = Math.max(minWidth, startWidth.value + diff)
  columnWidths.value[index] = newWidth
  saveColumnWidths()
}

function stopResize() {
  if (!resizing.value) return
  resizing.value = false
  resizingIndex.value = -1
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.querySelectorAll('.column-resizing').forEach(el => el.classList.remove('column-resizing'))
}

function resetColumnWidth(index) {
  columnWidths.value[index] = DEFAULT_COLUMN_WIDTHS[tableHeaders[index].key]
  saveColumnWidths()
}

function showTooltip(event, content) {
  if (!content) return hideTooltip()
  tooltip.value.content = content
  tooltip.value.visible = true

  nextTick(() => {
    const offsetX = event.clientX
    const offsetY = event.clientY - 10
    tooltip.value.style.left = `${offsetX}px`
    tooltip.value.style.top = `${offsetY}px`
    tooltip.value.style.opacity = '1'
  })
}

function hideTooltip() {
  tooltip.value.visible = false
  tooltip.value.opacity = '0'
}

function handleScroll() {
  hideTooltip()
}

onMounted(() => {
  loadColumnWidths()
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})

function formatDate(dateString) {
  return dateString ? dateString.slice(0, 10) : '-'
}
</script>

<style scoped>
.column-resizer {
  user-select: none;
  /* 鼠标改成列调整光标 */
  cursor: col-resize;
  transition: background-color 0.2s;
  z-index: 10;
}

.column-resizer:hover {
  background-color: rgba(59, 130, 246, 0.5); /* Tailwind 蓝色500半透明 */
}

.column-resizing {
  background-color: rgba(59, 130, 246, 0.8);
}

.fixed {
  position: fixed !important;
  z-index: 9999;
}

.table-container::-webkit-scrollbar {
  height: 6px;
  width: 6px;
}

.table-container::-webkit-scrollbar-thumb {
  background-color: rgba(107, 114, 128, 0.5);
  border-radius: 3px;
}
</style>
