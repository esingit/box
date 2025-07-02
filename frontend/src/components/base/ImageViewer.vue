<!--src/components/common/ImageViewer.vue-->
<template>
  <!-- 普通模式的模态框 -->
  <BaseModal
      v-if="!comparisonMode"
      :visible="visible"
      :hide-header="false"
      :draggable="true"
      :resizable="true"
      :z-index="3000"
      width="360px"
      height="850px"
      :min-width="300"
      :min-height="400"
      :max-width="1400"
      :max-height="1000"
      @update:visible="handleClose"
  >
    <template #header="{ close }">
      <div class="flex items-center justify-between w-full">
        <div class="flex items-center gap-4">
          <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 pointer-events-none">
            图片查看
          </h3>

          <!-- 对比模式开关 -->
          <div class="flex items-center gap-2 pointer-events-auto">
            <label class="flex items-center gap-2 cursor-pointer">
              <input
                  type="checkbox"
                  v-model="comparisonMode"
                  class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
              />
              <span class="text-sm text-gray-700 dark:text-gray-300">对比模式</span>
            </label>
          </div>
        </div>

        <button
            @click="close"
            class="text-gray-500 hover:text-gray-800 dark:hover:text-white transition-colors duration-200 pointer-events-auto cursor-pointer"
            aria-label="关闭弹窗"
            type="button"
            @mousedown.stop
        >
          <X class="w-5 h-5" />
        </button>
      </div>
    </template>

    <template #default>
      <div class="relative flex items-center justify-center h-full -mx-6 -my-4 bg-gray-50">
        <!-- 图片容器 -->
        <div class="w-full h-full flex items-center justify-center p-4">
          <img
              ref="imageRef"
              :src="src"
              alt="查看大图"
              :class="imageClass"
              @click.stop
              @load="onImageLoad"
              @error="onImageError"
          />
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-gray-100">
          <div class="flex flex-col items-center gap-3">
            <Loader2 class="w-8 h-8 text-blue-500 animate-spin" />
            <p class="text-gray-600 text-sm">图片加载中...</p>
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-gray-100">
          <div class="flex flex-col items-center gap-3 text-gray-500">
            <AlertTriangle class="w-12 h-12" />
            <p class="text-sm">图片加载失败</p>
            <button
                @click="retryLoad"
                class="flex items-center gap-2 px-3 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600 transition-colors"
            >
              <RotateCcw class="w-3 h-3" />
              重试
            </button>
          </div>
        </div>

        <!-- 普通模式控制按钮 -->
        <div class="absolute bottom-4 right-4 z-10 flex items-center gap-2">
          <button
              @click="fitToWindow"
              :class="[
                'bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200',
                fitMode === 'contain' ? 'ring-2 ring-white ring-opacity-50' : ''
              ]"
              aria-label="适应窗口"
              type="button"
              title="适应窗口大小"
          >
            <Maximize2 class="w-5 h-5" />
          </button>

          <button
              @click="resetSize"
              :class="[
                'bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200',
                fitMode === 'original' ? 'ring-2 ring-white ring-opacity-50' : ''
              ]"
              aria-label="原始尺寸"
              type="button"
              title="原始尺寸"
          >
            <Square class="w-5 h-5" />
          </button>

          <button
              @click="zoomIn"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              aria-label="放大"
              type="button"
              title="放大"
          >
            <ZoomIn class="w-5 h-5" />
          </button>

          <button
              @click="zoomOut"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              aria-label="缩小"
              type="button"
              title="缩小"
          >
            <ZoomOut class="w-5 h-5" />
          </button>
        </div>
      </div>
    </template>
  </BaseModal>

  <!-- 对比模式的浮动窗口 -->
  <Teleport to="body" v-if="comparisonMode && visible">
    <div class="comparison-overlay">
      <!-- 浮动图片容器 - 整个容器透明 -->
      <div
          class="floating-image-container"
          :style="{
            transform: `translate(${position.x}px, ${position.y}px)`,
            width: `${containerSize.width}px`,
            height: `${containerSize.height}px`,
            opacity: containerOpacity
          }"
          @mousedown="startDrag"
          @touchstart="startDrag"
      >
        <!-- 头部控制栏 -->
        <div class="floating-header">
          <div class="flex items-center gap-2 flex-1">
            <Move class="w-4 h-4 text-gray-600" />
            <span class="text-sm font-medium text-gray-700">对比</span>
          </div>

          <div class="flex items-center gap-1">
            <!-- 弹窗透明度控制 -->
            <div class="flex items-center gap-1">
              <span class="text-xs text-gray-600">透明度</span>
              <input
                  type="range"
                  v-model="containerOpacity"
                  min="0.3"
                  max="1"
                  step="0.1"
                  class="w-16 h-1 bg-gray-200 rounded-lg appearance-none cursor-pointer mini-slider"
                  @mousedown.stop
              />
            </div>

            <!-- 大小控制 -->
            <button
                @click.stop="toggleSize"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="切换大小"
            >
              <Maximize class="w-4 h-4 text-gray-600" />
            </button>

            <!-- 重置位置 -->
            <button
                @click.stop="resetPosition"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="重置位置"
            >
              <RotateCcw class="w-4 h-4 text-gray-600" />
            </button>

            <!-- 关闭对比模式 -->
            <button
                @click.stop="comparisonMode = false"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="退出对比模式"
            >
              <X class="w-4 h-4 text-gray-600" />
            </button>
          </div>
        </div>

        <!-- 图片内容区 -->
        <div class="floating-content">
          <img
              :src="src"
              alt="对比图片"
              class="floating-image"
              :style="{
                transform: `scale(${zoomLevel})`
              }"
              @wheel.prevent="handleFloatingWheel"
              @mousedown.stop
              draggable="false"
          />

          <!-- 缩放控制 -->
          <div class="floating-zoom-controls">
            <button
                @click.stop="zoomOut"
                class="zoom-btn"
                :disabled="zoomLevel <= 0.3"
                title="缩小"
            >
              <ZoomOut class="w-3 h-3" />
            </button>
            <span class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</span>
            <button
                @click.stop="zoomIn"
                class="zoom-btn"
                :disabled="zoomLevel >= 3"
                title="放大"
            >
              <ZoomIn class="w-3 h-3" />
            </button>
          </div>
        </div>

        <!-- 调整大小手柄 -->
        <div
            class="resize-handle"
            @mousedown.stop="startResize"
            @touchstart.stop="startResize"
        >
          <CornerDownRight class="w-3 h-3 text-gray-400" />
        </div>
      </div>

      <!-- 操作提示 -->
      <div class="comparison-hints">
        <div class="hint-bubble">
          <Mouse class="w-4 h-4" />
          <span>拖拽移动窗口</span>
        </div>
        <div class="hint-bubble">
          <MousePointer class="w-4 h-4" />
          <span>调节透明度可透视对比</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, reactive } from 'vue'
import {
  X,
  Maximize2,
  Square,
  ZoomIn,
  ZoomOut,
  Loader2,
  AlertTriangle,
  RotateCcw,
  Move,
  Maximize,
  CornerDownRight,
  Mouse,
  MousePointer
} from 'lucide-vue-next'

const props = defineProps<{
  visible: boolean
  src: string
}>()

const emit = defineEmits(['close'])

// 响应式状态
const imageRef = ref<HTMLImageElement>()
const loading = ref(true)
const error = ref(false)
const imageLoaded = ref(false)
const fitMode = ref<'contain' | 'original' | 'zoom'>('contain')
const zoomLevel = ref(1)

// 对比模式相关状态
const comparisonMode = ref(false)
const containerOpacity = ref(0.8) // 整个弹窗的透明度
const isDragging = ref(false)
const isResizing = ref(false)

// 浮动窗口位置和大小
const position = reactive({ x: 100, y: 100 })
const containerSize = reactive({ width: 350, height: 850 })
const sizeMode = ref<'small' | 'medium' | 'large'>('medium')

// 拖拽相关状态
const dragState = reactive({
  isDragging: false,
  startX: 0,
  startY: 0,
  startPosX: 0,
  startPosY: 0
})

// 调整大小相关状态
const resizeState = reactive({
  isResizing: false,
  startX: 0,
  startY: 0,
  startWidth: 0,
  startHeight: 0
})

// 计算图片样式类
const imageClass = computed(() => {
  const baseClass = 'rounded-lg shadow-2xl transition-all duration-300'

  if (fitMode.value === 'contain') {
    return `${baseClass} max-w-full max-h-full object-contain`
  } else if (fitMode.value === 'original') {
    return `${baseClass} object-none max-w-none max-h-none`
  } else {
    return `${baseClass} object-contain transform transition-transform duration-300`
  }
})

// 预设大小
const sizePresets = {
  small: { width: 270, height: 600 },
  medium: { width: 355, height: 800 },
  large: { width: 560, height: 750 }
}

// 图片加载完成
function onImageLoad() {
  loading.value = false
  error.value = false
  imageLoaded.value = true
}

// 图片加载错误
function onImageError() {
  loading.value = false
  error.value = true
  imageLoaded.value = false
}

// 重试加载
function retryLoad() {
  if (imageRef.value) {
    loading.value = true
    error.value = false
    imageLoaded.value = false
    imageRef.value.src = props.src + '?t=' + Date.now()
  }
}

// 适应窗口
function fitToWindow() {
  fitMode.value = 'contain'
  zoomLevel.value = 1
  updateImageTransform()
}

// 原始尺寸
function resetSize() {
  fitMode.value = 'original'
  zoomLevel.value = 1
  updateImageTransform()
}

// 放大
function zoomIn() {
  if (comparisonMode.value) {
    zoomLevel.value = Math.min(zoomLevel.value * 1.2, 3)
  } else {
    fitMode.value = 'zoom'
    zoomLevel.value = Math.min(zoomLevel.value * 1.2, 5)
    updateImageTransform()
  }
}

// 缩小
function zoomOut() {
  if (comparisonMode.value) {
    zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.3)
  } else {
    fitMode.value = 'zoom'
    zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.1)
    updateImageTransform()
  }
}

// 更新图片变换
function updateImageTransform() {
  if (imageRef.value && fitMode.value === 'zoom' && !comparisonMode.value) {
    imageRef.value.style.transform = `scale(${zoomLevel.value})`
  } else if (imageRef.value && !comparisonMode.value) {
    imageRef.value.style.transform = ''
  }
}

// 切换大小
function toggleSize() {
  const modes: ('small' | 'medium' | 'large')[] = ['small', 'medium', 'large']
  const currentIndex = modes.indexOf(sizeMode.value)
  const nextIndex = (currentIndex + 1) % modes.length
  sizeMode.value = modes[nextIndex]

  const newSize = sizePresets[sizeMode.value]
  containerSize.width = newSize.width
  containerSize.height = newSize.height
}

// 重置位置
function resetPosition() {
  position.x = 100
  position.y = 100
  zoomLevel.value = 1
  containerOpacity.value = 0.8
  containerSize.width = sizePresets.medium.width
  containerSize.height = sizePresets.medium.height
  sizeMode.value = 'medium'
}

// 拖拽功能
function startDrag(e: MouseEvent | TouchEvent) {
  e.preventDefault()
  e.stopPropagation()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  dragState.isDragging = true
  dragState.startX = clientX
  dragState.startY = clientY
  dragState.startPosX = position.x
  dragState.startPosY = position.y

  isDragging.value = true

  document.addEventListener('mousemove', handleDrag, { passive: false })
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', handleDrag, { passive: false })
  document.addEventListener('touchend', stopDrag)
}

function handleDrag(e: MouseEvent | TouchEvent) {
  if (!dragState.isDragging) return

  e.preventDefault()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  const deltaX = clientX - dragState.startX
  const deltaY = clientY - dragState.startY

  position.x = dragState.startPosX + deltaX
  position.y = dragState.startPosY + deltaY
}

function stopDrag() {
  dragState.isDragging = false
  isDragging.value = false

  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', handleDrag)
  document.removeEventListener('touchend', stopDrag)
}

// 调整大小功能
function startResize(e: MouseEvent | TouchEvent) {
  e.preventDefault()
  e.stopPropagation()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  resizeState.isResizing = true
  resizeState.startX = clientX
  resizeState.startY = clientY
  resizeState.startWidth = containerSize.width
  resizeState.startHeight = containerSize.height

  isResizing.value = true

  document.addEventListener('mousemove', handleResize, { passive: false })
  document.addEventListener('mouseup', stopResize)
  document.addEventListener('touchmove', handleResize, { passive: false })
  document.addEventListener('touchend', stopResize)
}

function handleResize(e: MouseEvent | TouchEvent) {
  if (!resizeState.isResizing) return

  e.preventDefault()

  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY

  const deltaX = clientX - resizeState.startX
  const deltaY = clientY - resizeState.startY

  containerSize.width = Math.max(200, resizeState.startWidth + deltaX)
  containerSize.height = Math.max(150, resizeState.startHeight + deltaY)
}

function stopResize() {
  resizeState.isResizing = false
  isResizing.value = false

  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)
}

// 浮动窗口的滚轮事件
function handleFloatingWheel(e: WheelEvent) {
  e.preventDefault()

  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

// 关闭处理
function handleClose() {
  emit('close')
  comparisonMode.value = false
  resetPosition()
  fitMode.value = 'contain'
  zoomLevel.value = 1
}

// 键盘事件处理
function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) return

  switch (event.key) {
    case 'Escape':
      if (comparisonMode.value) {
        comparisonMode.value = false
      } else {
        emit('close')
      }
      break
    case '1':
      resetSize()
      break
    case '0':
      fitToWindow()
      break
    case '+':
    case '=':
      zoomIn()
      break
    case '-':
      zoomOut()
      break
    case 'c':
    case 'C':
      comparisonMode.value = !comparisonMode.value
      break
    case 'r':
    case 'R':
      if (comparisonMode.value) {
        resetPosition()
      }
      break
  }
}

// 监听可见性变化
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    loading.value = true
    error.value = false
    imageLoaded.value = false
    fitMode.value = 'contain'
    zoomLevel.value = 1
    comparisonMode.value = false
    resetPosition()
  }
})

// 监听 src 变化
watch(() => props.src, () => {
  if (props.visible) {
    loading.value = true
    error.value = false
    imageLoaded.value = false
    fitMode.value = 'contain'
    zoomLevel.value = 1
  }
})

// 监听对比模式变化
watch(comparisonMode, (newMode) => {
  if (newMode) {
    resetPosition()
    zoomLevel.value = 1
    // 设置为手机宽高
    containerSize.width = 270
    containerSize.height = 600
  }
})

onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', handleDrag)
  document.removeEventListener('touchend', stopDrag)
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)
})
</script>

<style scoped>
:deep(.modal-content) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.modal-body) {
  flex: 1;
  display: flex;
  padding: 0;
}

.comparison-overlay {
  position: fixed;
  inset: 0;
  z-index: 2500;
  pointer-events: none;
}

.floating-image-container {
  position: absolute;
  background: #fff;
  border-radius: 12px;
  box-shadow: none;
  border: 1px solid #ccc;
  overflow: hidden;
  pointer-events: auto;
  cursor: move;
  transition: opacity 0.2s ease;
  min-width: 200px;
  min-height: 150px;
}

.floating-image-container:hover {
  box-shadow: none;
}

.floating-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f5f5;
  border-bottom: 1px solid #e5e5e5;
  cursor: move;
}

.floating-content {
  background: #ffffff;
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: calc(100% - 40px);
  overflow: hidden;
}

.floating-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.2s ease;
}

.floating-zoom-controls {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 6px;
  padding: 4px 8px;
}

.zoom-btn {
  width: 24px;
  height: 24px;
  background: transparent;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.zoom-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.zoom-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.zoom-level {
  color: white;
  font-size: 0.75rem;
  font-weight: 500;
  min-width: 35px;
  text-align: center;
}

.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  background: #e5e5e5;
  border-top-left-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: nw-resize;
}

.resize-handle:hover {
  background: #d4d4d4;
}

.mini-slider {
  appearance: none;
  height: 2px;
  background: #ccc;
  border-radius: 9999px;
  padding: 0;
  margin: 0;
  outline: none;
  border: none;
}

.mini-slider:focus {
  outline: none;
  box-shadow: none;
}

.mini-slider::-webkit-slider-thumb {
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #000;
  cursor: pointer;
  border: none;
}

.mini-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #000;
  border: none;
  cursor: pointer;
}

.comparison-hints {
  position: absolute;
  bottom: 20px;
  left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

.hint-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 20px;
  font-size: 0.875rem;
  backdrop-filter: blur(8px);
  opacity: 0;
  animation: fadeInOut 6s ease-in-out;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0; transform: translateY(10px); }
  15%, 85% { opacity: 1; transform: translateY(0); }
}

.hint-bubble:nth-child(2) {
  animation-delay: 3s;
}

:deep(.modal-body)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

:deep(.modal-body)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

:deep(.modal-body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

:deep(.modal-body)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

