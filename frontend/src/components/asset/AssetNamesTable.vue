<template>
  <div class="max-w-full bg-white rounded-md shadow border border-gray-200 overflow-x-auto">
    <table class="min-w-[600px] w-full table-fixed text-sm text-gray-800">
      <thead class="bg-gray-50 select-none">
      <tr>
        <th
            v-for="(header, index) in tableHeaders"
            :key="header.key"
            :style="{ width: columnWidths[index] + 'px' }"
            class="px-4 py-3 font-medium text-left text-gray-900 relative"
        >
          {{ header.label }}
          <div
              v-if="index < tableHeaders.length - 1"
              class="absolute top-0 right-0 h-full w-1 cursor-col-resize hover:bg-gray-300"
              @mousedown.prevent="startResize($event, index)"
              @dblclick.prevent="resetColumnWidth(index)"
          ></div>
        </th>
      </tr>
      </thead>
      <tbody>
      <tr v-if="names.length === 0" class="text-center text-gray-500">
        <td :colspan="tableHeaders.length" class="py-8">
          <EmptyState
              icon="Wallet"
              message="暂无资产名称"
              description="点击上方的新增按钮添加资产名称"
          />
        </td>
      </tr>
      <tr
          v-for="name in names"
          :key="name.id"
          class="border-b border-gray-100 hover:bg-gray-50 transition-colors duration-150"
      >
        <td
            class="px-4 py-3 whitespace-nowrap truncate relative"
            :style="{ width: columnWidths[0] + 'px' }"
            @mouseenter="handleCellMouseEnter($event, name.name)"
            @mouseleave="handleCellMouseLeave"
        >
          {{ name.name }}
          <div
              v-if="name.name"
              class="absolute bg-gray-800 text-white text-xs rounded px-2 py-1 pointer-events-none opacity-0 transition-opacity duration-150 z-10 cell-tooltip"
          >
            {{ name.name }}
          </div>
        </td>
        <td
            class="px-4 py-3 whitespace-nowrap truncate relative text-gray-600"
            :style="{ width: columnWidths[1] + 'px' }"
            @mouseenter="handleCellMouseEnter($event, name.description)"
            @mouseleave="handleCellMouseLeave"
        >
            <span v-if="name.description" class="select-text">
              {{ name.description }}
              <div
                  class="absolute bg-gray-800 text-white text-xs rounded px-2 py-1 pointer-events-none opacity-0 transition-opacity duration-150 z-10 cell-tooltip"
              >
                {{ name.description }}
              </div>
            </span>
          <span v-else class="text-gray-400 select-none">-</span>
        </td>
        <td class="px-4 py-3 text-right" :style="{ width: columnWidths[2] + 'px' }">
          <RecordActions
              :record="name"
              type="asset-name"
              @edit="$emit('edit', name)"
              @delete="$emit('delete', name)"
          />
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import EmptyState from '@/components/base/EmptyState.vue'
import RecordActions from '@/components/base/RecordActions.vue'

const DEFAULT_COLUMN_WIDTHS = {
  name: 400,
  description: 200,
  actions: 100,
}

const columnWidths = ref(Object.values(DEFAULT_COLUMN_WIDTHS))
const resizing = ref(false)
const resizingIndex = ref(-1)
const startX = ref(0)
const startWidth = ref(0)

defineProps({
  names: {
    type: Array,
    required: true,
  },
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'name', label: '资产名称' },
  { key: 'description', label: '描述' },
  { key: 'actions', label: '操作' },
]

const tooltipTarget = ref(null)
const activeTooltip = ref(null)

function handleCellMouseEnter(event, content) {
  if (!content) return

  const cell = event.currentTarget
  const tooltip = cell.querySelector('.cell-tooltip')
  if (!tooltip) return

  const mouseX = event.clientX
  const mouseY = event.clientY

  tooltip.style.left = `${mouseX}px`
  tooltip.style.top = `${mouseY - 12}px`
  tooltip.style.transform = 'translate(-50%, -100%)'
  tooltip.classList.add('opacity-100')
  tooltip.classList.remove('opacity-0')
  activeTooltip.value = tooltip
  tooltipTarget.value = cell

  setTimeout(() => {
    const rect = tooltip.getBoundingClientRect()
    if (rect.left < 8) {
      tooltip.style.left = '8px'
      tooltip.style.transform = 'translate(0, -100%)'
    } else if (rect.right > window.innerWidth - 8) {
      tooltip.style.left = `${window.innerWidth - 8}px`
      tooltip.style.transform = 'translate(-100%, -100%)'
    }
    if (rect.top < 8) {
      tooltip.style.top = `${mouseY + 18}px`
      tooltip.style.transform = tooltip.style.transform.replace('-100%', '0')
    }
  }, 0)
}

function handleCellMouseLeave() {
  if (activeTooltip.value) {
    activeTooltip.value.classList.add('opacity-0')
    activeTooltip.value.classList.remove('opacity-100')
    activeTooltip.value = null
    tooltipTarget.value = null
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
  const minWidth = 80
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

function saveColumnWidths() {
  try {
    const widths = {}
    tableHeaders.forEach((header, index) => {
      widths[header.key] = columnWidths.value[index]
    })
    localStorage.setItem('fitnessTableColumnWidths', JSON.stringify(widths))
  } catch (error) {
    console.error('保存列宽失败:', error)
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
  } catch (error) {
    console.error('加载列宽失败:', error)
  }
}

function handleScroll() {
  if (activeTooltip.value) {
    handleCellMouseLeave()
  }
}

onMounted(() => {
  loadColumnWidths()
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.cell-tooltip {
  pointer-events: none;
  transition: opacity 0.15s ease;
  opacity: 0;
  white-space: nowrap;
  user-select: none;
  z-index: 10;
}
.column-resizing {
  background-color: #cbd5e1 !important; /* Tailwind slate-300 */
}
</style>
