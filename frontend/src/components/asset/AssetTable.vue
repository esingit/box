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
              icon="Wallet"
              message="暂无资产记录"
              description="点击上方的添加记录按钮开始记录"
            />
          </td>
        </tr>
        <tr v-for="(record, idx) in records" :key="record.id || idx">
          <td class="cell-text" 
              :style="{ width: columnWidths[0] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.assetName)" 
              @mouseleave="handleCellMouseLeave">
            {{ record.assetName }}
            <div v-if="record.assetName" class="cell-tooltip">{{ record.assetName }}</div>
          </td>
          <td class="cell-text" 
              :style="{ width: columnWidths[1] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.assetTypeValue)" 
              @mouseleave="handleCellMouseLeave">
            {{ record.assetTypeValue }}
            <div v-if="record.assetTypeValue" class="cell-tooltip">{{ record.assetTypeValue }}</div>
          </td>
          <td class="cell-number" 
              :style="{ width: columnWidths[2] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, formatAmount(record.amount) + ' ' + record.unitValue)" 
              @mouseleave="handleCellMouseLeave">
            <span :class="{ 'negative': record.amount < 0 }">
              {{ formatAmount(record.amount) }}
            </span>
            {{ record.unitValue }}
            <div v-if="record.amount" class="cell-tooltip">{{ formatAmount(record.amount) + ' ' + record.unitValue }}</div>
          </td>
          <td class="cell-text" 
              :style="{ width: columnWidths[3] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, record.locationValue)" 
              @mouseleave="handleCellMouseLeave">
            {{ record.locationValue }}
            <div v-if="record.locationValue" class="cell-tooltip">{{ record.locationValue }}</div>
          </td>
          <td class="cell-date" 
              :style="{ width: columnWidths[4] + 'px' }"
              @mouseenter="handleCellMouseEnter($event, formatDate(record.acquireTime))" 
              @mouseleave="handleCellMouseLeave">
            {{ formatDate(record.acquireTime) }}
            <div v-if="record.acquireTime" class="cell-tooltip">{{ formatDate(record.acquireTime) }}</div>
          </td>
          <td class="cell-remark" 
              :style="{ width: columnWidths[5] + 'px' }"
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
              type="asset"
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
import EmptyState from '@/components/base/EmptyState.vue'
import RecordActions from '@/components/base/RecordActions.vue'

// 默认列宽
const DEFAULT_COLUMN_WIDTHS = {
  assetName: 400,    // 资产名称
  assetType: 100,    // 类型
  amount: 150,       // 金额
  location: 100,     // 位置
  time: 120,         // 时间
  remark: 100,       // 备注
  actions: 100       // 操作
}

const columnWidths = ref(Object.values(DEFAULT_COLUMN_WIDTHS))
const resizing = ref(false)
const resizingIndex = ref(-1)
const startX = ref(0)
const startWidth = ref(0)

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
  
  // 保存到 localStorage
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
    localStorage.setItem('assetTableColumnWidths', JSON.stringify(widths))
  } catch (error) {
    console.error('Failed to save column widths:', error)
  }
}

function loadColumnWidths() {
  try {
    const saved = localStorage.getItem('assetTableColumnWidths')
    if (saved) {
      const widths = JSON.parse(saved)
      tableHeaders.forEach((header, index) => {
        columnWidths.value[index] = widths[header.key] || DEFAULT_COLUMN_WIDTHS[header.key]
      })
    }
  } catch (error) {
    console.error('Failed to load column widths:', error)
  }
}

// 组件挂载时加载保存的列宽
onMounted(() => {
  loadColumnWidths()
})

defineProps({
  records: {
    type: Array,
    required: true,
    default: () => []
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'assetName', label: '资产名称', class: 'cell-text' },
  { key: 'assetType', label: '类型', class: 'cell-text' },
  { key: 'amount', label: '金额', class: 'cell-number' },
  { key: 'location', label: '位置', class: 'cell-text' },
  { key: 'time', label: '时间', class: 'cell-date' },
  { key: 'remark', label: '备注', class: 'cell-remark' },
  { key: 'actions', label: '操作', class: 'cell-actions' }
]

function formatAmount(amount) {
  if (!amount) return '0.00'
  const formatted = new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Math.abs(amount))
  return (amount < 0 ? '-' : '') + formatted
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

const tooltipTarget = ref(null)
const activeTooltip = ref(null)

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

function handleScroll() {
  if (activeTooltip.value) {
    handleCellMouseLeave()
  }
}

onMounted(() => {
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


