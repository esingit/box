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
        <tr v-if="records.length === 0">
          <td :colspan="tableHeaders.length">
            <EmptyState
              icon="Dumbbell"
              message="暂无健身记录"
              description="点击上方的添加记录按钮开始记录"
            />
          </td>
        </tr>
        <tr v-for="(record, idx) in records" :key="record.id || idx" class="table-row">
          <td class="cell-text"
              :style="{ width: columnWidths[0] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.typeValue)"
              @mouseleave="handleCellMouseLeave">
            {{ record.typeValue }}
            <div v-if="record.typeValue" class="cell-tooltip">{{ record.typeValue }}</div>
          </td>
          <td class="cell-number"
              :style="{ width: columnWidths[1] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.count + ' ' + record.unitValue)"
              @mouseleave="handleCellMouseLeave">
            {{ record.count }}
            <div v-if="record.count" class="cell-tooltip">{{ record.count + ' ' + record.unitValue }}</div>
          </td>
          <td class="cell-text"
              :style="{ width: columnWidths[2] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.unitValue)"
              @mouseleave="handleCellMouseLeave">
            {{ record.unitValue }}
            <div v-if="record.unitValue" class="cell-tooltip">{{ record.unitValue }}</div>
          </td>
          <td class="cell-date"
              :style="{ width: columnWidths[3] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, formatDate(record.finishTime))"
              @mouseleave="handleCellMouseLeave">
            {{ formatDate(record.finishTime) }}
            <div v-if="record.finishTime" class="cell-tooltip">{{ formatDate(record.finishTime) }}</div>
          </td>
          <td class="cell-remark"
              :style="{ width: columnWidths[4] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.remark)"
              @mouseleave="handleCellMouseLeave">
            <span v-if="record.remark" class="remark-text">
              {{ record.remark }}
              <div class="cell-tooltip">{{ record.remark }}</div>
            </span>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="cell-actions">
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
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import RecordActions from '@/components/common/RecordActions.vue'

// 默认列宽
const DEFAULT_COLUMN_WIDTHS = {
  type: 120,      // 类型
  count: 100,     // 数量  
  unit: 100,      // 单位
  date: 120,      // 日期
  remark: 200,    // 备注
  actions: 100    // 操作
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

const props = defineProps({
  records: {
    type: Array,
    required: true
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'type', label: '类型', class: 'cell-text' },
  { key: 'count', label: '数量', class: 'cell-number' },
  { key: 'unit', label: '单位', class: 'cell-text' },
  { key: 'date', label: '日期', class: 'cell-date' },
  { key: 'remark', label: '备注', class: 'cell-remark' },
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

function formatDate(dateString) {
  return dateString ? dateString.slice(0, 10) : '-'
}
</script>
