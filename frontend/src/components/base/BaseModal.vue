<!-- src/components/base/BaseModal.vue -->
<template>
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="visible"
          class="fixed inset-0 flex items-center justify-center bg-black/50"
          :style="{ zIndex: computedZIndex }"
          @click.self="close"
      >
        <div
            ref="modalRef"
            class="bg-white rounded-2xl shadow-xl dark:bg-gray-800 flex flex-col relative"
            :class="{ 'cursor-move': isDragging, 'select-none': isDragging || isResizing }"
            :style="modalStyle"
            @click.stop
        >
          <!-- 调整大小的句柄 -->
          <template v-if="resizable">
            <!-- 四个角的句柄 -->
            <div class="resize-handle corner top-left" @mousedown="startResize('nw')"></div>
            <div class="resize-handle corner top-right" @mousedown="startResize('ne')"></div>
            <div class="resize-handle corner bottom-left" @mousedown="startResize('sw')"></div>
            <div class="resize-handle corner bottom-right" @mousedown="startResize('se')"></div>

            <!-- 四条边的句柄 -->
            <div class="resize-handle edge top" @mousedown="startResize('n')"></div>
            <div class="resize-handle edge bottom" @mousedown="startResize('s')"></div>
            <div class="resize-handle edge left" @mousedown="startResize('w')"></div>
            <div class="resize-handle edge right" @mousedown="startResize('e')"></div>
          </template>

          <!-- Header - 可拖拽 -->
          <div
              v-if="!hideHeader"
              ref="headerRef"
              class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700 shrink-0 cursor-move select-none rounded-t-2xl"
              @mousedown="startDrag"
          >
            <h2 class="text-lg font-semibold text-gray-800 dark:text-gray-100 pointer-events-none">{{ title }}</h2>
            <button
                @click="close"
                class="text-gray-500 hover:text-gray-800 dark:hover:text-white text-xl transition-colors duration-200 pointer-events-auto cursor-pointer"
                aria-label="关闭弹窗"
                type="button"
                @mousedown.stop
            >
              ✕
            </button>
          </div>

          <!-- 没有头部时的拖拽区域 -->
          <div
              v-if="hideHeader && draggable"
              class="absolute top-4 left-4 right-16 h-8 cursor-move z-30 flex items-center justify-center"
              @mousedown="startDrag"
          >
            <div class="bg-black bg-opacity-20 rounded px-3 py-1 opacity-0 hover:opacity-100 transition-opacity duration-200">
              <svg class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                <path d="M7 2a1 1 0 011 1v2h4V3a1 1 0 112 0v4a1 1 0 11-2 0V6h-4v1a1 1 0 11-2 0V3a1 1 0 011-1zM7 14a1 1 0 011 1v2h4v-2a1 1 0 112 0v4a1 1 0 11-2 0v-1H8v1a1 1 0 11-2 0v-4a1 1 0 011-1z"/>
              </svg>
            </div>
          </div>

          <!-- 内容插槽区域：支持滚动 -->
          <div class="flex-1 overflow-y-auto px-6 py-4 text-gray-800 dark:text-gray-200 min-h-0">
            <slot />
          </div>

          <!-- 底部插槽区域：固定在底部 -->
          <div
              v-if="$slots.footer"
              class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-right rounded-b-2xl shrink-0"
          >
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, reactive, watch, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  width?: string
  widthClass?: string
  zIndex?: number
  hideHeader?: boolean
  draggable?: boolean
  resizable?: boolean
  minWidth?: number
  minHeight?: number
  maxWidth?: number
  maxHeight?: number
}>(), {
  draggable: true,
  resizable: true,
  minWidth: 300,
  minHeight: 200,
  maxWidth: 1400,
  maxHeight: 900
})

const emit = defineEmits(['update:visible'])

// 引用
const modalRef = ref<HTMLElement>()

// 状态
const isDragging = ref(false)
const isResizing = ref(false)
const resizeDirection = ref('')

// 位置和尺寸状态
const transform = reactive({
  x: 0,
  y: 0
})

const size = reactive({
  width: 0,
  height: 0
})

const dragState = reactive({
  startX: 0,
  startY: 0,
  startTransformX: 0,
  startTransformY: 0
})

const resizeState = reactive({
  startX: 0,
  startY: 0,
  startWidth: 0,
  startHeight: 0,
  startTransformX: 0,
  startTransformY: 0
})

// 计算样式
const computedZIndex = computed(() => {
  return props.zIndex ?? 2000
})

const modalStyle = computed(() => {
  const style: any = {
    zIndex: computedZIndex.value + 10,
    maxHeight: '90vh'
  }

  // 设置宽度
  if (props.width) {
    style.width = props.width
  } else {
    style.width = '600px'
  }

  // 如果有自定义尺寸，应用它
  if (size.width > 0) {
    style.width = `${size.width}px`
  }
  if (size.height > 0) {
    style.height = `${size.height}px`
    style.maxHeight = `${size.height}px`
  }

  // 应用变换
  if (transform.x !== 0 || transform.y !== 0) {
    style.transform = `translate(${transform.x}px, ${transform.y}px)`
  }

  return style
})

// 重置状态
const resetState = () => {
  transform.x = 0
  transform.y = 0
  size.width = 0
  size.height = 0
  isDragging.value = false
  isResizing.value = false
}

const close = () => {
  emit('update:visible', false)
  resetState()
}

// 获取当前尺寸
const getCurrentSize = () => {
  if (!modalRef.value) return { width: 600, height: 400 }

  const rect = modalRef.value.getBoundingClientRect()
  return {
    width: size.width || rect.width,
    height: size.height || rect.height
  }
}

// 拖拽功能
const startDrag = (event: MouseEvent) => {
  if (!props.draggable || isResizing.value) return

  event.preventDefault()
  event.stopPropagation()

  isDragging.value = true

  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.startTransformX = transform.x
  dragState.startTransformY = transform.y

  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'move'
}

const handleDrag = (event: MouseEvent) => {
  if (!isDragging.value) return

  event.preventDefault()

  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY

  const newX = dragState.startTransformX + deltaX
  const newY = dragState.startTransformY + deltaY

  // 边界限制
  const windowWidth = window.innerWidth
  const windowHeight = window.innerHeight
  const currentSize = getCurrentSize()

  const maxX = windowWidth / 2 - 50
  const minX = -windowWidth / 2 + currentSize.width - 50
  const maxY = windowHeight / 2 - 50
  const minY = -windowHeight / 2 + currentSize.height - 50

  transform.x = Math.max(minX, Math.min(maxX, newX))
  transform.y = Math.max(minY, Math.min(maxY, newY))
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}

// 调整大小功能
const startResize = (direction: string) => {
  if (!props.resizable) return

  isResizing.value = true
  resizeDirection.value = direction

  const event = window.event as MouseEvent
  event.preventDefault()
  event.stopPropagation()

  const currentSize = getCurrentSize()

  resizeState.startX = event.clientX
  resizeState.startY = event.clientY
  resizeState.startWidth = currentSize.width
  resizeState.startHeight = currentSize.height
  resizeState.startTransformX = transform.x
  resizeState.startTransformY = transform.y

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.userSelect = 'none'

  // 设置光标
  const cursors: Record<string, string> = {
    'n': 'n-resize',
    's': 's-resize',
    'e': 'e-resize',
    'w': 'w-resize',
    'ne': 'ne-resize',
    'nw': 'nw-resize',
    'se': 'se-resize',
    'sw': 'sw-resize'
  }
  document.body.style.cursor = cursors[direction] || 'default'
}

const handleResize = (event: MouseEvent) => {
  if (!isResizing.value) return

  event.preventDefault()

  const deltaX = event.clientX - resizeState.startX
  const deltaY = event.clientY - resizeState.startY

  let newWidth = resizeState.startWidth
  let newHeight = resizeState.startHeight
  let newTransformX = resizeState.startTransformX
  let newTransformY = resizeState.startTransformY

  const direction = resizeDirection.value

  // 处理宽度变化
  if (direction.includes('e')) {
    newWidth = resizeState.startWidth + deltaX
  } else if (direction.includes('w')) {
    newWidth = resizeState.startWidth - deltaX
    newTransformX = resizeState.startTransformX + deltaX
  }

  // 处理高度变化
  if (direction.includes('s')) {
    newHeight = resizeState.startHeight + deltaY
  } else if (direction.includes('n')) {
    newHeight = resizeState.startHeight - deltaY
    newTransformY = resizeState.startTransformY + deltaY
  }

  // 应用最小/最大尺寸限制
  const constrainedWidth = Math.max(props.minWidth, Math.min(props.maxWidth, newWidth))
  const constrainedHeight = Math.max(props.minHeight, Math.min(props.maxHeight, newHeight))

  // 如果达到最小尺寸，调整位置
  if (constrainedWidth === props.minWidth && direction.includes('w')) {
    newTransformX = resizeState.startTransformX + (resizeState.startWidth - props.minWidth)
  }
  if (constrainedHeight === props.minHeight && direction.includes('n')) {
    newTransformY = resizeState.startTransformY + (resizeState.startHeight - props.minHeight)
  }

  size.width = constrainedWidth
  size.height = constrainedHeight
  transform.x = newTransformX
  transform.y = newTransformY
}

const stopResize = () => {
  isResizing.value = false
  resizeDirection.value = ''
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}

// ESC键关闭
const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') close()
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 调整大小句柄样式 */
.resize-handle {
  position: absolute;
  background: transparent;
  z-index: 40;
}

.resize-handle:hover {
  background: rgba(59, 130, 246, 0.3);
}

/* 角落句柄 */
.resize-handle.corner {
  width: 12px;
  height: 12px;
}

.resize-handle.top-left {
  top: -6px;
  left: -6px;
  cursor: nw-resize;
  border-radius: 6px 0 0 0;
}

.resize-handle.top-right {
  top: -6px;
  right: -6px;
  cursor: ne-resize;
  border-radius: 0 6px 0 0;
}

.resize-handle.bottom-left {
  bottom: -6px;
  left: -6px;
  cursor: sw-resize;
  border-radius: 0 0 0 6px;
}

.resize-handle.bottom-right {
  bottom: -6px;
  right: -6px;
  cursor: se-resize;
  border-radius: 0 0 6px 0;
}

/* 边缘句柄 */
.resize-handle.edge {
  background: transparent;
}

.resize-handle.top {
  top: -4px;
  left: 12px;
  right: 12px;
  height: 8px;
  cursor: n-resize;
}

.resize-handle.bottom {
  bottom: -4px;
  left: 12px;
  right: 12px;
  height: 8px;
  cursor: s-resize;
}

.resize-handle.left {
  left: -4px;
  top: 12px;
  bottom: 12px;
  width: 8px;
  cursor: w-resize;
}

.resize-handle.right {
  right: -4px;
  top: 12px;
  bottom: 12px;
  width: 8px;
  cursor: e-resize;
}

/* 防止文本选择 */
.select-none {
  user-select: none;
}
</style>