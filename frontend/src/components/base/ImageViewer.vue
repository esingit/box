<template>
  <!-- 普通模式的模态框 -->
  <BaseModal
      v-if="!comparisonMode && visible"
      :visible="true"
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
        </div>
        <button
            @click="close"
            class="text-gray-500 hover:text-gray-800 dark:hover:text-white transition-colors duration-200 pointer-events-auto cursor-pointer"
            aria-label="关闭弹窗"
            type="button"
            @mousedown.stop
        >
          <X class="w-5 h-5"/>
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
            <Loader2 class="w-8 h-8 text-blue-500 animate-spin"/>
            <p class="text-gray-600 text-sm">图片加载中...</p>
          </div>
        </div>

        <!-- 错误状态 -->
        <div v-if="error" class="absolute inset-0 flex items-center justify-center bg-gray-100">
          <div class="flex flex-col items-center gap-3 text-gray-500">
            <AlertTriangle class="w-12 h-12"/>
            <p class="text-sm">图片加载失败</p>
            <button
                @click="retryLoad"
                class="flex items-center gap-2 px-3 py-1 bg-blue-500 text-white rounded text-sm hover:bg-blue-600 transition-colors"
            >
              <RotateCcw class="w-3 h-3"/>
              重试
            </button>
          </div>
        </div>

        <!-- 普通模式控制按钮 -->
        <div class="absolute bottom-4 right-4 z-10 flex items-center gap-2">
          <button
              @click="openComparisonMode"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              title="图片对比"
          >
            <Image class="w-5 h-5"/>
          </button>
          <button
              @click="fitToWindow"
              :class="[
                'bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200',
                fitMode === 'contain' ? 'ring-2 ring-white ring-opacity-50' : ''
              ]"
              title="适应窗口大小"
          >
            <Maximize2 class="w-5 h-5"/>
          </button>

          <button
              @click="resetSize"
              :class="[
                'bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200',
                fitMode === 'original' ? 'ring-2 ring-white ring-opacity-50' : ''
              ]"
              title="原始尺寸"
          >
            <Square class="w-5 h-5"/>
          </button>

          <button
              @click="zoomIn"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              title="放大"
          >
            <ZoomIn class="w-5 h-5"/>
          </button>

          <button
              @click="zoomOut"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              title="缩小"
          >
            <ZoomOut class="w-5 h-5"/>
          </button>
        </div>
      </div>
    </template>
  </BaseModal>

  <!-- 对比模式 -->
  <Teleport to="body" v-if="comparisonMode && visible">
    <div class="comparison-overlay">
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
        <div class="floating-header">
          <div class="flex items-center gap-2 flex-1">
            <Move class="w-4 h-4 text-gray-600"/>
          </div>

          <div class="flex items-center gap-1">
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

            <button
                @click.stop="toggleSize"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="切换大小"
            >
              <Maximize class="w-4 h-4 text-gray-600"/>
            </button>

            <button
                @click.stop="resetPosition"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="重置位置"
            >
              <RotateCcw class="w-4 h-4 text-gray-600"/>
            </button>

            <!-- 根据是否为自动对比模式显示不同的关闭行为 -->
            <button
                v-if="isAutoComparisonMode"
                @click.stop="handleClose"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="关闭"
            >
              <X class="w-4 h-4 text-gray-600"/>
            </button>
            <button
                v-else
                @click.stop="exitComparisonMode"
                class="p-1 hover:bg-gray-200 rounded transition-colors"
                title="退出对比模式"
            >
              <X class="w-4 h-4 text-gray-600"/>
            </button>
          </div>
        </div>

        <div class="floating-content">
          <img
              :src="src"
              alt="对比图片"
              class="floating-image"
              :style="{ transform: `scale(${zoomLevel})` }"
              @wheel.prevent="handleFloatingWheel"
              @mousedown.stop
              draggable="false"
          />

          <div class="floating-zoom-controls">
            <button
                @click.stop="zoomOut"
                class="zoom-btn"
                :disabled="zoomLevel <= 0.3"
                title="缩小"
            >
              <ZoomOut class="w-3 h-3"/>
            </button>
            <div class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</div>
            <button
                @click.stop="zoomIn"
                class="zoom-btn"
                :disabled="zoomLevel >= 3"
                title="放大"
            >
              <ZoomIn class="w-3 h-3"/>
            </button>
          </div>
        </div>

        <div
            class="resize-handle"
            @mousedown.stop="startResize"
            @touchstart.stop="startResize"
        >
          <CornerDownRight class="w-3 h-3 text-gray-400"/>
        </div>
      </div>

      <div class="comparison-hints">
        <div class="hint-bubble">
          <Mouse class="w-4 h-4"/>
          <span>拖拽移动窗口</span>
        </div>
        <div class="hint-bubble">
          <MousePointer class="w-4 h-4"/>
          <span>调节透明度可透视</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {
  AlertTriangle,
  CornerDownRight,
  Image,
  Loader2,
  Maximize,
  Maximize2,
  Mouse,
  MousePointer,
  Move,
  RotateCcw,
  Square,
  X,
  ZoomIn,
  ZoomOut
} from 'lucide-vue-next'

const props = defineProps<{
  visible: boolean
  src: string
  autoComparison?: boolean
}>()
const emit = defineEmits(['close'])

const imageRef = ref<HTMLImageElement>()
const loading = ref(true)
const error = ref(false)
const imageLoaded = ref(false)
const fitMode = ref<'contain' | 'original' | 'zoom'>('contain')
const zoomLevel = ref(1)

// 根据 autoComparison 设置初始状态，避免闪烁
const comparisonMode = ref(!!props.autoComparison)
// 记录是否为自动对比模式（用于区分关闭行为）
const isAutoComparisonMode = ref(!!props.autoComparison)
const containerOpacity = ref(0.8)
const isDragging = ref(false)
const isResizing = ref(false)
const position = reactive({x: 100, y: 100})
const containerSize = reactive({
  width: props.autoComparison ? 270 : 350,
  height: props.autoComparison ? 600 : 850
})
const sizeMode = ref<'small' | 'medium' | 'large'>(props.autoComparison ? 'small' : 'medium')

const dragState = reactive({isDragging: false, startX: 0, startY: 0, startPosX: 0, startPosY: 0})
const resizeState = reactive({isResizing: false, startX: 0, startY: 0, startWidth: 0, startHeight: 0})

const sizePresets = {
  small: {width: 270, height: 600},
  medium: {width: 355, height: 800},
  large: {width: 560, height: 750}
}

const imageClass = computed(() => {
  const base = 'rounded-lg shadow-2xl transition-all duration-300'
  return fitMode.value === 'contain'
      ? `${base} max-w-full max-h-full object-contain`
      : fitMode.value === 'original'
          ? `${base} object-none max-w-none max-h-none`
          : `${base} object-contain transform transition-transform duration-300`
})

function onImageLoad() {
  loading.value = false;
  error.value = false;
  imageLoaded.value = true
}

function onImageError() {
  loading.value = false;
  error.value = true;
  imageLoaded.value = false
}

function retryLoad() {
  if (imageRef.value) {
    loading.value = true
    error.value = false
    imageRef.value.src = props.src + '?t=' + Date.now()
  }
}

function fitToWindow() {
  fitMode.value = 'contain';
  zoomLevel.value = 1;
  updateImageTransform()
}

function resetSize() {
  fitMode.value = 'original';
  zoomLevel.value = 1;
  updateImageTransform()
}

function zoomIn() {
  if (comparisonMode.value) {
    zoomLevel.value = Math.min(zoomLevel.value * 1.2, 3)
  } else {
    fitMode.value = 'zoom'
    zoomLevel.value = Math.min(zoomLevel.value * 1.2, 5)
    updateImageTransform()
  }
}

function zoomOut() {
  if (comparisonMode.value) {
    zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.3)
  } else {
    fitMode.value = 'zoom'
    zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.1)
    updateImageTransform()
  }
}

function updateImageTransform() {
  if (imageRef.value && fitMode.value === 'zoom' && !comparisonMode.value)
    imageRef.value.style.transform = `scale(${zoomLevel.value})`
  else if (imageRef.value) imageRef.value.style.transform = ''
}

function toggleSize() {
  const modes = ['small', 'medium', 'large'] as const
  const next = modes[(modes.indexOf(sizeMode.value) + 1) % modes.length]
  sizeMode.value = next
  Object.assign(containerSize, sizePresets[next])
}

function resetPosition() {
  position.x = 100;
  position.y = 100;
  zoomLevel.value = 1
  containerOpacity.value = 0.8
  Object.assign(containerSize, sizePresets.medium)
  sizeMode.value = 'medium'
}

function openComparisonMode() {
  comparisonMode.value = true
  isAutoComparisonMode.value = false // 手动打开的对比模式
  resetPosition()
  zoomLevel.value = 1
  Object.assign(containerSize, sizePresets.small)
  sizeMode.value = 'small'
}

// 退出对比模式（回到普通模式）
function exitComparisonMode() {
  comparisonMode.value = false
  isAutoComparisonMode.value = false
  resetPosition()
  zoomLevel.value = 1
  fitMode.value = 'contain'
  Object.assign(containerSize, sizePresets.medium)
  sizeMode.value = 'medium'
}

function initializeMode() {
  // 重置基础状态
  loading.value = true
  error.value = false
  imageLoaded.value = false
  fitMode.value = 'contain'
  zoomLevel.value = 1
  containerOpacity.value = 0.8
  position.x = 100
  position.y = 100

  // 根据 autoComparison 设置模式和尺寸
  if (props.autoComparison) {
    comparisonMode.value = true
    isAutoComparisonMode.value = true // 自动对比模式
    Object.assign(containerSize, sizePresets.small)
    sizeMode.value = 'small'
  } else {
    comparisonMode.value = false
    isAutoComparisonMode.value = false
    Object.assign(containerSize, sizePresets.medium)
    sizeMode.value = 'medium'
  }
}

function resetAllStates() {
  comparisonMode.value = false
  isAutoComparisonMode.value = false
  loading.value = true
  error.value = false
  imageLoaded.value = false
  fitMode.value = 'contain'
  zoomLevel.value = 1
  containerOpacity.value = 0.8
  position.x = 100
  position.y = 100
  Object.assign(containerSize, sizePresets.medium)
  sizeMode.value = 'medium'
}

function startDrag(e: MouseEvent | TouchEvent) {
  const x = 'touches' in e ? e.touches[0].clientX : e.clientX
  const y = 'touches' in e ? e.touches[0].clientY : e.clientY
  Object.assign(dragState, {isDragging: true, startX: x, startY: y, startPosX: position.x, startPosY: position.y})
  isDragging.value = true
  document.addEventListener('mousemove', handleDrag, {passive: false})
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', handleDrag, {passive: false})
  document.addEventListener('touchend', stopDrag)
}

function handleDrag(e: MouseEvent | TouchEvent) {
  if (!dragState.isDragging) return
  const x = 'touches' in e ? e.touches[0].clientX : e.clientX
  const y = 'touches' in e ? e.touches[0].clientY : e.clientY
  position.x = dragState.startPosX + (x - dragState.startX)
  position.y = dragState.startPosY + (y - dragState.startY)
}

function stopDrag() {
  dragState.isDragging = false
  isDragging.value = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', handleDrag)
  document.removeEventListener('touchend', stopDrag)
}

function startResize(e: MouseEvent | TouchEvent) {
  const x = 'touches' in e ? e.touches[0].clientX : e.clientX
  const y = 'touches' in e ? e.touches[0].clientY : e.clientY
  Object.assign(resizeState, {
    isResizing: true,
    startX: x,
    startY: y,
    startWidth: containerSize.width,
    startHeight: containerSize.height
  })
  isResizing.value = true
  document.addEventListener('mousemove', handleResize, {passive: false})
  document.addEventListener('mouseup', stopResize)
  document.addEventListener('touchmove', handleResize, {passive: false})
  document.addEventListener('touchend', stopResize)
}

function handleResize(e: MouseEvent | TouchEvent) {
  if (!resizeState.isResizing) return
  const x = 'touches' in e ? e.touches[0].clientX : e.clientX
  const y = 'touches' in e ? e.touches[0].clientY : e.clientY
  containerSize.width = Math.max(200, resizeState.startWidth + (x - resizeState.startX))
  containerSize.height = Math.max(150, resizeState.startHeight + (y - resizeState.startY))
}

function stopResize() {
  resizeState.isResizing = false
  isResizing.value = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResize)
  document.removeEventListener('touchend', stopResize)
}

function handleFloatingWheel(e: WheelEvent) {
  e.preventDefault()
  e.deltaY < 0 ? zoomIn() : zoomOut()
}

// 关闭函数 - 只负责通知父组件，不重置状态
function handleClose() {
  emit('close')
}

// 监听 visible 变化
watch(() => props.visible, (v) => {
  if (v) {
    initializeMode()
  } else {
    // visible 变成 false 时重置所有状态
    resetAllStates()
  }
})

// 监听 autoComparison 变化
watch(() => props.autoComparison, (autoComparison) => {
  if (props.visible) {
    initializeMode()
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

onMounted(() => {
  document.addEventListener('keydown', handleKeyDown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) return
  switch (event.key) {
    case 'Escape':
      // 如果是自动对比模式或普通模式，直接关闭
      // 如果是手动进入的对比模式，先退出对比模式
      if (isAutoComparisonMode.value || !comparisonMode.value) {
        handleClose()
      } else {
        exitComparisonMode()
      }
      break
    case '1':
      resetSize();
      break
    case '0':
      fitToWindow();
      break
    case '+':
    case '=':
      zoomIn();
      break
    case '-':
      zoomOut();
      break
    case 'c':
    case 'C':
      if (!comparisonMode.value) {
        openComparisonMode();
      }
      break
    case 'r':
    case 'R':
      if (comparisonMode.value) resetPosition();
      break
  }
}
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
  border-radius: var(--radius-xl);
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
  align-items: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: var(--radius-xl);
  padding: 4px 8px;
  height: 32px;
}

.zoom-btn {
  width: 24px;
  height: 24px;
  background: transparent;
  color: white;
  border-radius: var(--radius-xl);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  border: none;
  outline: none;
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
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 4px;
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
  border-radius: var(--radius-full);
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
  border-radius: var(--radius-xl);
  background: #000;
  cursor: pointer;
  border: none;
}

.mini-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: var(--radius-xl);
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
  border-radius: var(--radius-xl);
  font-size: 0.875rem;
  backdrop-filter: blur(8px);
  opacity: 0;
  animation: fadeInOut 6s ease-in-out;
}

@keyframes fadeInOut {
  0%, 100% {
    opacity: 0;
    transform: translateY(10px);
  }
  15%, 85% {
    opacity: 1;
    transform: translateY(0);
  }
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
  bborder-radius: var(--radius-xl);
}

:deep(.modal-body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: var(--radius-xl);

}

:deep(.modal-body)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>