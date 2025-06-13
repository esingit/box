<template>
  <div class="data-table">
    <table class="table">
      <thead>
        <tr>
          <th v-for="(header, index) in tableHeaders" 
              :key="header.key"
              :class="header.class"
              :style="{ width: columnWidths[index] + 'px' }">
            {{ header.label }}
            <div v-if="index < tableHeaders.length - 1"
                 class="column-resizer"
                 @mousedown="startResize($event, index)"
                 @dblclick="resetColumnWidth(index)">
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="names.length === 0">
          <td :colspan="tableHeaders.length">
            <EmptyState
              icon="Wallet"
              message="暂无资产名称"
              description="点击上方的新增按钮添加资产名称"
            />
          </td>
        </tr>
        <tr v-for="name in names" :key="name.id" class="table-row">
          <td class="cell-text"
              :style="{ width: columnWidths[0] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, name.name)"
              @mouseleave="handleCellMouseLeave">
            {{ name.name }}
            <div v-if="name.name" class="cell-tooltip">{{ name.name }}</div>
          </td>
          <td class="cell-remark"
              :style="{ width: columnWidths[1] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, name.description)"
              @mouseleave="handleCellMouseLeave">
            <span v-if="name.description" class="remark-text">
              {{ name.description }}
              <div class="cell-tooltip">{{ name.description }}</div>
            </span>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="cell-actions">
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
import EmptyState from '@/components/common/EmptyState.vue'
import RecordActions from '@/components/common/RecordActions.vue'

// 默认列宽
const DEFAULT_COLUMN_WIDTHS = {
  name: 400,          // 资产名称
  description: 100,   // 描述
  actions: 100        // 操作
}

// 提示框相关
const tooltipTarget = ref(null)
const activeTooltip = ref(null)

// 列宽调整相关 
const columnWidths = ref(Object.values(DEFAULT_COLUMN_WIDTHS))
const resizing = ref(false)
const resizingIndex = ref(-1)
const startX = ref(0)
const startWidth = ref(0)

defineProps({
  names: {
    type: Array,
    required: true
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'name', label: '资产名称', class: 'cell-text' },
  { key: 'description', label: '描述', class: 'cell-remark' },
  { key: 'actions', label: '操作', class: 'cell-actions' }
]

// 提示框处理函数
function handleCellMouseEnter(event, content) {
  if (!content) return
  
  const cell = event.currentTarget
  const tooltip = cell.querySelector('.cell-tooltip')
  if (!tooltip) return

  // 获取鼠标位置  
  const mouseX = event.clientX
  const mouseY = event.clientY
  
  // 设置tooltip在鼠标指针上方
  tooltip.style.left = `${mouseX}px`
  tooltip.style.top = `${mouseY - 10}px`  // 在鼠标上方10px处
  tooltip.style.transform = 'translate(-50%, -100%)'
  
  tooltip.classList.add('visible')
  activeTooltip.value = tooltip
  tooltipTarget.value = cell

  // 确保tooltip不会超出视口
  setTimeout(() => {
    const tooltipRect = tooltip.getBoundingClientRect()
    
    // 处理水平方向的边界
    if (tooltipRect.left < 5) {
      tooltip.style.left = '5px'
      tooltip.style.transform = 'translate(0, -100%)'
    } else if (tooltipRect.right > window.innerWidth - 5) {
      tooltip.style.left = `${window.innerWidth - 5}px`
      tooltip.style.transform = 'translate(-100%, -100%)'
    }
    
    // 处理垂直方向的边界
    if (tooltipRect.top < 5) {
      // 如果上方空间不够，显示在下方
      tooltip.style.top = `${mouseY + 20}px`
      tooltip.style.transform = tooltip.style.transform.replace('-100%', '0')
    }
  }, 0)
}

function handleCellMouseLeave() {
  if (activeTooltip.value) {
    activeTooltip.value.classList.remove('visible')
    activeTooltip.value = null
    tooltipTarget.value = null
  }
}

// 列宽调整处理函数
function startResize(event, index) {
  resizing.value = true
  resizingIndex.value = index
  startX.value = event.pageX
  startWidth.value = columnWidths.value[index]
  
  // 添加事件监听器
  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  
  // 添加样式类
  event.target.classList.add('column-resizing')
}

function handleResize(event) {
  if (!resizing.value) return
  
  const index = resizingIndex.value
  const minWidth = 60 // 最小宽度
  const diff = event.pageX - startX.value
  const newWidth = Math.max(minWidth, startWidth.value + diff)
  
  columnWidths.value[index] = newWidth
  
  // 保存到localStorage
  saveColumnWidths()
}

function stopResize(event) {
  if (!resizing.value) return
  
  resizing.value = false
  resizingIndex.value = -1
  
  // 移除事件监听器
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  
  // 移除样式类
  const resizers = document.querySelectorAll('.column-resizing')
  resizers.forEach(el => el.classList.remove('column-resizing'))
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
    localStorage.setItem('assetNamesTableColumnWidths', JSON.stringify(widths))
  } catch (error) {
    console.error('保存列宽失败:', error)
  }
}

function loadColumnWidths() {
  try {
    const saved = localStorage.getItem('assetNamesTableColumnWidths')
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

// 处理滚动时取消提示
function handleScroll() {
  if (activeTooltip.value) {
    handleCellMouseLeave()
  }
}

onMounted(() => {
  loadColumnWidths()
  const tableContainer = document.querySelector('.table-container')
  if (tableContainer) {
    tableContainer.addEventListener('scroll', handleScroll)
  }
  window.addEventListener('scroll', handleScroll) 
})

onBeforeUnmount(() => {
  const tableContainer = document.querySelector('.table-container')
  if (tableContainer) {
    tableContainer.removeEventListener('scroll', handleScroll)
  }
  window.removeEventListener('scroll', handleScroll)
})
</script>
