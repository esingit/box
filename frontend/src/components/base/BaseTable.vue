<template>
  <div class="relative border border-gray-200 rounded-xl" style="min-height: 520px; max-height: 520px; width: 100%;">
    <!-- 表格头部区域 -->
    <div
        :class="[
        'top-0 z-20 bg-gray-50 rounded-t-xl',
        shouldStickyHeader ? 'sticky' : ''
      ]"
    >
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <thead>
        <tr>
          <!-- 使用 processedColumns -->
          <th
              v-for="(col, idx) in processedColumns"
              :key="col.key"
              class="px-3 py-2 font-medium whitespace-nowrap relative group"
              :style="{ width: columnWidths[col.key] + 'px' }"
              :class="getHeaderAlignClass(col)"
          >
            <div
                :class="[
                  'w-full flex items-center gap-1',
                  getHeaderTextAlignClass(col),
                  'select-none truncate pr-2',
                  col.sortable ? 'cursor-pointer hover:text-green-600 transition-colors' : ''
                ]"
                @click="col.sortable ? handleSort(col.key) : null"
            >
              <span class="truncate">{{ col.label }}</span>
              <div v-if="col.sortable" class="flex-shrink-0 ml-1">
                <svg v-if="sortKey === col.key && sortOrder === 'asc'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-green-600"><path d="m18 15-6-6-6 6"/></svg>
                <svg v-else-if="sortKey === col.key && sortOrder === 'desc'" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-green-600"><path d="m6 9 6 6 6-6"/></svg>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400 group-hover:text-gray-600"><path d="m7 15 5 5 5-5"/><path d="m7 9 5-5 5 5"/></svg>
              </div>
            </div>
            <div v-if="col.resizable && idx < processedColumns.length - 1" class="absolute right-0 top-0 h-full w-1 cursor-col-resize group-hover:bg-gray-300" @mousedown.prevent="startResize($event, col.key)" @dblclick.prevent="resetColumnWidth(col.key)"></div>
          </th>
        </tr>
        </thead>
      </table>
    </div>

    <!-- 表格内容区域 -->
    <div ref="contentRef" class="relative" style="height: 480px; overflow-y: auto;" @scroll="checkScrollable">
      <table class="min-w-full table-fixed text-sm text-gray-800 border-separate border-spacing-0">
        <colgroup>
          <!-- 使用 processedColumns -->
          <col v-for="col in processedColumns" :key="col.key" :style="{ width: columnWidths[col.key] + 'px' }" />
        </colgroup>
        <tbody>
        <tr v-if="loading">
          <td :colspan="processedColumns.length" class="py-8">
            <slot name="loading"><div class="space-y-2"><div v-for="i in 5" :key="i" class="h-6 bg-gray-200 rounded animate-pulse"></div></div></slot>
          </td>
        </tr>
        <tr v-else-if="!sortedData.length">
          <td :colspan="processedColumns.length" class="py-8 text-center text-gray-400">
            <slot name="empty"><BaseEmptyState icon="Dumbbell" message="暂无数据" description="点击上方的添加按钮开始记录" /></slot>
          </td>
        </tr>
        <tr v-else v-for="(row, rowIndex) in sortedData" :key="row.id || rowIndex" class="hover:bg-gray-100 transition-colors" :class="{ 'relative z-[100]': activeRowIndex === rowIndex }">
          <!-- 使用 processedColumns -->
          <td v-for="col in processedColumns" :key="col.key" class="px-3 py-2 whitespace-nowrap" :style="{ width: columnWidths[col.key] + 'px', maxWidth: columnWidths[col.key] + 'px', minWidth: columnWidths[col.key] + 'px', position: hasDropdown(col) && activeRowIndex === rowIndex ? 'relative' : 'static', zIndex: hasDropdown(col) && activeRowIndex === rowIndex ? 200 : 'auto', overflow: hasDropdown(col) ? 'visible' : 'hidden' }" :class="getCellAlignClass(col)">
            <template v-if="col.actions">
              <div class="flex justify-end"><slot name="actions" :record="row" :index="rowIndex"><BaseActions :record="row" @edit="handleEdit(row, rowIndex)" @delete="handleDelete(row, rowIndex)"/></slot></div>
            </template>
            <template v-else-if="$slots[`cell-${col.key}`]">
              <div :class="['w-full', hasDropdown(col) ? 'overflow-visible' : 'overflow-hidden']" @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)" @mousemove="updateTooltipPosition($event)" @mouseleave="hideTooltip">
                <slot :name="`cell-${col.key}`" :record="row" :index="rowIndex" :column="col" :value="row[col.key]" :set-active-row="setActiveRow" :clear-active-row="clearActiveRow" />
              </div>
            </template>
            <template v-else-if="isEditable(col)">
              <div :class="['w-full', hasDropdown(col) ? 'overflow-visible' : 'overflow-hidden']" @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)" @mousemove="updateTooltipPosition($event)" @mouseleave="hideTooltip">
                <component :is="getEditorComponent(col)" :model-value="row[col.key]" :column="col" :record="row" :index="rowIndex" @update:model-value="handleCellChange(rowIndex, col.key, $event)" @blur="handleCellBlur(rowIndex, col.key)" />
              </div>
            </template>
            <template v-else>
              <div class="w-full overflow-hidden" @mouseenter="handleCellMouseEnter(rowIndex, col.key, $event, row, col)" @mousemove="updateTooltipPosition($event)" @mouseleave="hideTooltip">
                <span class="block truncate">{{ formatCell(row, col.key, col) }}</span>
              </div>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 单一全局Tooltip -->
    <Teleport to="body">
      <div v-if="tooltipVisible" class="fixed pointer-events-none select-none bg-gray-800 text-white text-xs rounded px-2 py-1 shadow-lg" :style="{ top: tooltipPosition.y + 'px', left: tooltipPosition.x + 'px', zIndex: 9999, maxWidth: '400px', wordWrap: 'break-word', whiteSpace: 'pre-wrap' }">
        {{ tooltipContent }}
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, markRaw, computed, nextTick, onMounted, watch, onUnmounted } from 'vue'
import type { Component } from 'vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseActions from '@/components/base/BaseActions.vue'
import TextEditor from './editors/TextEditor.vue'
import NumberEditor from './editors/NumberEditor.vue'
import SelectEditor from './editors/SelectEditor.vue'
import DateEditor from './editors/DateEditor.vue'
import { useAssetNameStore } from '@/store/assetNameStore'

// --- 类型定义 ---
type FormatterType = 'text' | 'number' | 'unit-number' | 'select' | 'date' | 'time' | 'datetime' | 'custom' | 'scan-result';
interface Option { label: string; value: string | number; [key: string]: any; }
interface Column {
  key: string; label: string; type?: FormatterType; defaultWidth?: number; resizable?: boolean; sortable?: boolean; editable?: boolean; actions?: boolean; align?: 'left' | 'center' | 'right'; headerAlign?: 'left' | 'center' | 'right'; options?: Option[]; displayField?: string; formatter?: (row: any, key: string) => string; tooltipFormatter?: (row: any, key: string) => string;
}
interface Props { columns: Column[]; data: any[]; loading?: boolean; editable?: boolean; }

// --- Props & Emits ---
const props = withDefaults(defineProps<Props>(), { data: () => [], loading: false, editable: false })
const emit = defineEmits<{
  edit: [row: any, index: number]; delete: [row: any, index: number]; 'cell-change': [row: any, key: string, value: any, index: number]; 'cell-blur': [row: any, key: string, index: number];
}>()

// --- 常量与Store ---
const assetNameStore = useAssetNameStore()
const DEFAULT_WIDTH = 100
const unitSymbolMap: Record<string, string> = { '人民币': '¥', '美元': '$', '欧元': '€' }
const editorComponents: Record<string, Component> = { text: markRaw(TextEditor), number: markRaw(NumberEditor), select: markRaw(SelectEditor), date: markRaw(DateEditor) }
const storeMapping: Record<string, () => Option[]> = {
  assetNameId: () => {
    if (assetNameStore.assetNameOptions?.length) return assetNameStore.assetNameOptions;
    if (assetNameStore.assetName?.length) return assetNameStore.assetName.map((item: any) => ({ label: item.name || item.label || item.assetName || '未知', value: String(item.id || item.value || item.assetNameId || '') }));
    return [];
  }
}

// --- 响应式数据 ---
const columnWidths = reactive<Record<string, number>>({})
const activeRowIndex = ref<number | null>(null)
const contentRef = ref<HTMLDivElement>()
const isContentScrollable = ref(false)
const sortKey = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc' | null>(null)
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipPosition = reactive({ x: 0, y: 0 })
const tooltipDebounceTimer = ref<number | null>(null)
const tooltipHideTimer = ref<number | null>(null)
let resizingKey: string | null = null
let startX = 0
let startWidth = 0
let resizeTimer: number | null = null

// --- 计算属性 (核心修改) ---

/**
 * 预处理列定义，为 key='count' 的列自动设置 type='unit-number'
 */
const processedColumns = computed(() => {
  return props.columns.map(col => {
    // 如果列的 key 是 'count' 且没有显式定义 type，则自动设置为 'unit-number' 用于带单位的格式化
    if (col.key === 'count' && col.type === undefined) {
      return { ...col, type: 'unit-number' as FormatterType };
    }
    return col;
  });
});

const sortedData = computed(() => {
  if (!sortKey.value || !sortOrder.value || !props.data.length) return props.data;
  const key = sortKey.value
  const order = sortOrder.value
  return [...props.data].sort((a, b) => compareValues(a[key], b[key], order))
})

const shouldStickyHeader = computed(() => isContentScrollable.value && sortedData.value.length > 0 && !props.loading)

// --- 初始化列宽 ---
watch(() => processedColumns.value, (newColumns) => {
  newColumns.forEach(col => {
    if (!columnWidths[col.key]) {
      columnWidths[col.key] = col.defaultWidth || DEFAULT_WIDTH;
    }
  });
}, { immediate: true, deep: true });


// --- 核心格式化逻辑 ---
function formatCell(row: any, key: string, col: Column): string {
  if (col.formatter) return col.formatter(row, key);
  const value = row[key];
  if (value === null || value === undefined || value === '') return '-';

  switch (col.type) {
    case 'unit-number': { // 【关键】处理所有带单位的数字，由 key='count' 自动触发
      const unitText = row.unitValue || '';
      const unit = unitSymbolMap[unitText] || unitText;
      return formatAmount(value) + (unit ? ` ${unit}` : '');
    }
    case 'number': { // 处理不带单位的纯数字
      return formatAmount(value);
    }
    case 'date':
    case 'time':
    case 'datetime':
      return formatDate(value, col.type);
    case 'select': {
      if (col.displayField && row[col.displayField]) return row[col.displayField];
      const options = (col.options || []).concat((storeMapping[key]?.() || []));
      const option = options.find((opt: Option) => String(opt.value) === String(value));
      return option?.label ?? String(value);
    }
  }
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (Array.isArray(value)) return value.join(', ');
  return String(value);
}

function formatAmount(value: any): string {
  const num = Number(value);
  return isNaN(num) ? '0.00' : num.toFixed(2);
}

function formatDate(dateStr: any, type: string = 'date'): string {
  if (!dateStr) return '-';
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return String(dateStr);
    const p = (n: number) => String(n).padStart(2, '0');
    const y = date.getFullYear(), m = p(date.getMonth() + 1), d = p(date.getDate());
    const H = p(date.getHours()), M = p(date.getMinutes()), S = p(date.getSeconds());
    switch (type) {
      case 'datetime': return `${y}-${m}-${d} ${H}:${M}:${S}`;
      case 'time': return `${H}:${M}:${S}`;
      default: return `${y}-${m}-${d}`;
    }
  } catch (e) { return String(dateStr); }
}

function getTooltipContent(row: any, key: string, col: Column): string {
  if (col.tooltipFormatter) return col.tooltipFormatter(row, key);
  return formatCell(row, key, col);
}


// --- 其他辅助函数与方法 ---
function compareValues(aVal: any, bVal: any, order: 'asc' | 'desc'): number {
  if (aVal == null && bVal == null) return 0;
  if (aVal == null) return order === 'asc' ? 1 : -1;
  if (bVal == null) return order === 'asc' ? -1 : 1;
  const dateA = new Date(aVal), dateB = new Date(bVal);
  if (!isNaN(dateA.getTime()) && !isNaN(dateB.getTime())) {
    return order === 'asc' ? dateA.getTime() - dateB.getTime() : dateB.getTime() - dateA.getTime();
  }
  const numA = parseFloat(aVal), numB = parseFloat(bVal);
  if (!isNaN(numA) && !isNaN(numB) && String(aVal).trim() === String(numA) && String(bVal).trim() === String(numB)) {
    return order === 'asc' ? numA - numB : numB - numA;
  }
  const result = String(aVal).localeCompare(String(bVal), 'zh-CN', { numeric: true });
  return order === 'asc' ? result : -result;
}

function checkScrollable(): void {
  if (contentRef.value) isContentScrollable.value = contentRef.value.scrollHeight > contentRef.value.clientHeight;
}

function handleSort(key: string): void {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : null;
    if (sortOrder.value === null) sortKey.value = null;
  } else {
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
}

function getHeaderAlignClass(col: Column): string {
  if (col.headerAlign) return `text-${col.headerAlign}`;
  if (col.actions) return 'text-center';
  if (['number', 'unit-number'].includes(col.type || '')) return 'text-right'; // 更新对齐规则
  if (['date', 'time', 'datetime'].includes(col.type || '')) return 'text-center';
  return 'text-left';
}

function getHeaderTextAlignClass(col: Column): string {
  const align = getHeaderAlignClass(col);
  if (align === 'text-right') return 'justify-end';
  if (align === 'text-center') return 'justify-center';
  return 'justify-start';
}

function getCellAlignClass(col: Column): string {
  if (col.align) return `text-${col.align}`;
  return getHeaderAlignClass(col);
}

function hasDropdown(col: Column): boolean { return col.type === 'select' || col.type === 'custom'; }
function setActiveRow(rowIndex: number): void { activeRowIndex.value = rowIndex; }
function clearActiveRow(): void { activeRowIndex.value = null; }
function isEditable(col: Column): boolean { return props.editable === true && col.editable !== false && !!col.type && col.type !== 'custom'; }
function getEditorComponent(col: Column): Component {
  // 对于 'unit-number' 类型，也使用 NumberEditor
  if (col.type === 'unit-number') return editorComponents['number'];
  return editorComponents[col.type || 'text'] || editorComponents.text;
}

function handleCellChange(rowIndex: number, key: string, value: any): void {
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex]);
  if (originalIndex > -1) emit('cell-change', props.data[originalIndex], key, value, originalIndex);
}

function handleCellBlur(rowIndex: number, key: string): void {
  const originalIndex = props.data.findIndex(item => item === sortedData.value[rowIndex]);
  if (originalIndex > -1) emit('cell-blur', props.data[originalIndex], key, originalIndex);
}

function startResize(e: MouseEvent, key: string): void {
  resizingKey = key; startX = e.pageX; startWidth = columnWidths[key];
  document.addEventListener('mousemove', doResize);
  document.addEventListener('mouseup', stopResize);
}

function doResize(e: MouseEvent): void {
  if (!resizingKey) return;
  if (resizeTimer) return;
  resizeTimer = window.setTimeout(() => {
    const delta = e.pageX - startX;
    columnWidths[resizingKey!] = Math.max(60, startWidth + delta);
    resizeTimer = null;
  }, 16);
}

function stopResize(): void {
  resizingKey = null;
  document.removeEventListener('mousemove', doResize);
  document.removeEventListener('mouseup', stopResize);
  if (resizeTimer) clearTimeout(resizeTimer);
  resizeTimer = null;
}

function resetColumnWidth(key: string): void {
  const col = processedColumns.value.find(c => c.key === key);
  columnWidths[key] = col?.defaultWidth || DEFAULT_WIDTH;
}

function handleCellMouseEnter(rowIndex: number, fieldKey: string, event: MouseEvent, row: any, col: Column): void {
  if (tooltipDebounceTimer.value) clearTimeout(tooltipDebounceTimer.value);
  if (tooltipHideTimer.value) clearTimeout(tooltipHideTimer.value);
  if (col.actions) return;
  tooltipDebounceTimer.value = window.setTimeout(() => {
    const content = getTooltipContent(row, fieldKey, col);
    if (!content || content === '-') return;
    tooltipContent.value = content;
    tooltipVisible.value = true;
    updateTooltipPosition(event);
  }, 200);
}

function updateTooltipPosition(event: MouseEvent): void {
  if (!tooltipVisible.value) return;
  tooltipPosition.x = event.clientX + 12;
  tooltipPosition.y = event.clientY + 20;
}

function hideTooltip(): void {
  if (tooltipDebounceTimer.value) clearTimeout(tooltipDebounceTimer.value);
  tooltipHideTimer.value = window.setTimeout(() => { tooltipVisible.value = false; }, 100);
}

function handleEdit(row: any, index: number): void {
  const originalIndex = props.data.findIndex(item => item === row);
  if (originalIndex > -1) emit('edit', row, originalIndex);
}

function handleDelete(row: any, index: number): void {
  const originalIndex = props.data.findIndex(item => item === row);
  if (originalIndex > -1) emit('delete', row, originalIndex);
}

function clearAllTimers(): void {
  if (tooltipDebounceTimer.value) clearTimeout(tooltipDebounceTimer.value);
  if (tooltipHideTimer.value) clearTimeout(tooltipHideTimer.value);
  if (resizeTimer) clearTimeout(resizeTimer);
}

// --- Lifecycle & Watchers ---
watch(() => sortedData.value, () => nextTick(checkScrollable), { deep: true, immediate: true });
watch(() => props.loading, () => nextTick(checkScrollable));

onMounted(() => nextTick(checkScrollable));
onUnmounted(() => {
  clearAllTimers();
  document.removeEventListener('mousemove', doResize);
  document.removeEventListener('mouseup', stopResize);
});
</script>