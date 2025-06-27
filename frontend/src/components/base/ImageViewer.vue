<!--src/components/common/ImageViewer.vue-->
<template>
  <BaseModal
      :visible="visible"
      :hide-header="false"
      :draggable="true"
      :resizable="true"
      :z-index="3000"
      width="450px"
      height="900px"
      :min-width="300"
      :min-height="400"
      :max-width="1400"
      :max-height="1000"
      @update:visible="$emit('close')"
  >
    <!-- 简化的头部 - 只保留标题和关闭按钮 -->
    <template #header="{ close }">
      <div class="flex items-center justify-between w-full">
        <h3 class="text-lg font-medium text-gray-900 dark:text-gray-100 pointer-events-none"/>
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

        <!-- 浮动工具栏 - 右下角 -->
        <div class="absolute bottom-4 right-4 z-10 flex items-center gap-2">
          <!-- 适应窗口按钮 -->
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

          <!-- 原始尺寸按钮 -->
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

          <!-- 放大按钮 -->
          <button
              @click="zoomIn"
              class="bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
              aria-label="放大"
              type="button"
              title="放大"
          >
            <ZoomIn class="w-5 h-5" />
          </button>

          <!-- 缩小按钮 -->
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import {
  X,
  Maximize2,
  Square,
  ZoomIn,
  ZoomOut,
  Loader2,
  AlertTriangle,
  RotateCcw
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
    // 强制重新加载图片
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
  fitMode.value = 'zoom'
  zoomLevel.value = Math.min(zoomLevel.value * 1.2, 5)
  updateImageTransform()
}

// 缩小
function zoomOut() {
  fitMode.value = 'zoom'
  zoomLevel.value = Math.max(zoomLevel.value / 1.2, 0.1)
  updateImageTransform()
}

// 更新图片变换
function updateImageTransform() {
  if (imageRef.value && fitMode.value === 'zoom') {
    imageRef.value.style.transform = `scale(${zoomLevel.value})`
  } else if (imageRef.value) {
    imageRef.value.style.transform = ''
  }
}

// 键盘事件处理
function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) return

  switch (event.key) {
    case 'Escape':
      emit('close')
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
  }
}

// 监听可见性变化，重置状态
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    loading.value = true
    error.value = false
    imageLoaded.value = false
    fitMode.value = 'contain'
    zoomLevel.value = 1
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
</script>

<style scoped>
/* 确保模态框内容撑满 */
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

/* 自定义滚动条 */
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