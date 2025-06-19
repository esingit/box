<template>
  <div class="max-w-full bg-white rounded-md shadow border border-gray-200 overflow-x-auto">
    <table class="min-w-[600px] w-full table-fixed text-sm text-gray-800">
      <thead class="bg-gray-50 select-none relative">
      <tr>
        <th
            v-for="(header, i) in tableHeaders"
            :key="header.key"
            :style="{ width: columnWidths[i] + 'px' }"
            class="px-4 py-3 font-medium text-left text-gray-900 relative"
        >
          {{ header.label }}
          <div
              v-if="i < tableHeaders.length - 1"
              class="absolute top-0 right-0 h-full w-1 cursor-col-resize hover:bg-gray-300"
              @mousedown.prevent="startResize($event, i)"
              @dblclick.prevent="resetColumnWidth(i)"
          ></div>
        </th>
      </tr>
      </thead>

      <tbody>
      <tr v-if="names.length === 0" class="text-center text-gray-500">
        <td :colspan="tableHeaders.length" class="py-8">
          <BaseEmptyState
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
            @mouseenter="showTooltip($event, name.name)"
            @mouseleave="hideTooltip"
        >
          {{ name.name }}
          <div class="cell-tooltip" ref="tooltipName">{{ name.name }}</div>
        </td>
        <td
            class="px-4 py-3 whitespace-nowrap truncate relative text-gray-600"
            :style="{ width: columnWidths[1] + 'px' }"
            @mouseenter="showTooltip($event, name.description)"
            @mouseleave="hideTooltip"
        >
            <span v-if="name.description" class="select-text">
              {{ name.description }}
              <div class="cell-tooltip" ref="tooltipDesc">{{ name.description }}</div>
            </span>
          <span v-else class="text-gray-400 select-none">-</span>
        </td>
        <td class="px-4 py-3 text-right" :style="{ width: columnWidths[2] + 'px' }">
          <BaseActions
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'

const props = defineProps({
  names: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['edit', 'delete'])

const DEFAULT_WIDTHS = {
  name: 400,
  description: 200,
  actions: 100,
}

const tableHeaders = [
  { key: 'name', label: '资产名称' },
  { key: 'description', label: '描述' },
  { key: 'actions', label: '操作' },
]

const columnWidths = ref(Object.values(DEFAULT_WIDTHS))

const resizing = ref(false)
const resizingIndex = ref(-1)
const startX = ref(0)
const startWidth = ref(0)

function startResize(event, index) {
  resizing.value = true
  resizingIndex.value = index
  startX.value = event.pageX
  startWidth.value = columnWidths.value[index]

  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  event.target.classList.add('column-resizing')
}

function onResize(event) {
  if (!resizing.value) return
  const idx = resizingIndex.value
  const minWidth = 80
  const delta = event.pageX - startX.value
  const newWidth = Math.max(minWidth, startWidth.value + delta)
  columnWidths.value[idx] = newWidth
  saveColumnWidths()
}

function stopResize() {
  if (!resizing.value) return
  resizing.value = false
  resizingIndex.value = -1
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
  document.querySelectorAll('.column-resizing').forEach(el => el.classList.remove('column-resizing'))
}

function resetColumnWidth(index) {
  columnWidths.value[index] = DEFAULT_WIDTHS[tableHeaders[index].key]
  saveColumnWidths()
}

function saveColumnWidths() {
  try {
    const saved = {}
    tableHeaders.forEach((h, i) => {
      saved[h.key] = columnWidths.value[i]
    })
    localStorage.setItem('assetNameTableColumnWidths', JSON.stringify(saved))
  } catch (e) {
    // ignore
  }
}

function loadColumnWidths() {
  try {
    const saved = localStorage.getItem('assetNameTableColumnWidths')
    if (saved) {
      const widths = JSON.parse(saved)
      tableHeaders.forEach((h, i) => {
        columnWidths.value[i] = widths[h.key] || DEFAULT_WIDTHS[h.key]
      })
    }
  } catch (e) {
    // ignore
  }
}

const activeTooltip = ref(null)

function showTooltip(event, content) {
  if (!content) return
  const cell = event.currentTarget
  let tooltip = cell.querySelector('.cell-tooltip')
  if (!tooltip) return

  // 清理之前的动画
  tooltip.style.opacity = '0'
  tooltip.style.left = '0'
  tooltip.style.top = '0'
  tooltip.style.transform = 'none'

  nextTick(() => {
    const rect = cell.getBoundingClientRect()
    tooltip.style.opacity = '1'
    tooltip.style.position = 'fixed'
    tooltip.style.left = event.clientX + 'px'
    tooltip.style.top = event.clientY - 12 + 'px'
    tooltip.style.transform = 'translate(-50%, -100%)'
    tooltip.style.pointerEvents = 'none'
    tooltip.style.zIndex = '1000'

    // 防止超出视窗
    const tipRect = tooltip.getBoundingClientRect()
    if (tipRect.left < 8) {
      tooltip.style.left = '8px'
      tooltip.style.transform = 'translate(0, -100%)'
    } else if (tipRect.right > window.innerWidth - 8) {
      tooltip.style.left = `${window.innerWidth - 8}px`
      tooltip.style.transform = 'translate(-100%, -100%)'
    }
    if (tipRect.top < 8) {
      tooltip.style.top = event.clientY + 18 + 'px'
      tooltip.style.transform = tooltip.style.transform.replace('-100%', '0')
    }

    activeTooltip.value = tooltip
  })
}

function hideTooltip() {
  if (activeTooltip.value) {
    activeTooltip.value.style.opacity = '0'
    activeTooltip.value = null
  }
}

function onScroll() {
  hideTooltip()
}

onMounted(() => {
  loadColumnWidths()
  window.addEventListener('scroll', onScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.cell-tooltip {
  position: absolute;
  background-color: #1f2937; /* Tailwind gray-800 */
  color: white;
  font-size: 0.75rem;
  border-radius: 0.375rem;
  padding: 0.125rem 0.5rem;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
  user-select: none;
  z-index: 10;
}

.column-resizing {
  background-color: #cbd5e1 !important; /* Tailwind slate-300 */
}
</style>
