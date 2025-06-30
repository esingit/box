<template>
  <BaseModal
      :title="title"
      :visible="visible"
      :width="modalSize.width"
      :height="modalSize.height"
      @update:visible="handleClose"
  >
    <!-- 主内容区域 -->
    <div class="modal-body">
      <!-- 上传区域 -->
      <div class="upload-section">
        <div class="flex items-center gap-4">
          <BaseButton
              id="image-upload"
              type="button"
              upload
              title="上传图片"
              accept="image/*"
              capture="environment"
              @change="handleImageUpload"
          />
          <BaseButton
              v-if="imageFile"
              type="button"
              @click="recognizeImage"
              :disabled="isProcessing"
              color="outline"
          >
            <component :is="isProcessing ? Loader2 : LucideScanText" class="w-5 h-5" :class="{'animate-spin': isProcessing}"/>
            <span>{{ isProcessing ? '识别中...' : '开始识别' }}</span>
          </BaseButton>
        </div>

        <!-- 图片预览 -->
        <div v-if="imagePreview" class="image-preview-wrapper">
          <img
              :src="imagePreview"
              alt="预览"
              class="preview-image"
              @click="showImageViewer = true"
          />
          <div class="preview-hint">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7"/>
            </svg>
            点击查看大图
          </div>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="content-wrapper">
        <!-- 加载状态 -->
        <div v-show="isProcessing" class="loading-skeleton">
          <div class="skeleton-section">
            <div class="skeleton-title"></div>
            <div class="skeleton-grid">
              <div class="skeleton-item" v-for="i in 5" :key="`sk1-${i}`"></div>
            </div>
          </div>
          <div class="skeleton-section mt-6">
            <div class="skeleton-title"></div>
            <div class="skeleton-list">
              <div class="skeleton-row" v-for="i in 3" :key="`sk2-${i}`"></div>
            </div>
          </div>
        </div>

        <!-- 有数据时的内容 -->
        <div v-show="!isProcessing && hasData" class="data-content">
          <!-- 公共属性设置 -->
          <div class="section-card">
            <h4 class="section-title">设置共同信息</h4>
            <div class="grid grid-cols-5 gap-4">
              <BaseSelect
                  title="资产类型"
                  v-model="formData.assetTypeId"
                  :options="options.assetTypes"
                  required
                  clearable
                  searchable
                  @update:model-value="handleAssetTypeChange"
              />
              <BaseSelect
                  title="资产位置"
                  v-model="formData.assetLocationId"
                  :options="options.assetLocations"
                  required
                  clearable
                  searchable
              />
              <BaseDateInput
                  title="登记日期"
                  v-model="formData.acquireTime"
                  type="date"
                  :max="TODAY"
                  required
                  clearable
              />
              <BaseSelect
                  title="货币单位"
                  v-model="formData.unitId"
                  :options="options.units"
                  required
                  clearable
                  searchable
              />
              <BaseButton
                  type="button"
                  title="名称管理"
                  color="outline"
                  @click="assetNameRef?.open()"
                  :icon="LucideSettings"
                  variant="search"
                  class="w-52"
              />
            </div>
          </div>

          <!-- 识别结果 -->
          <div class="section-card">
            <h4 class="section-title">
              识别结果 ({{ recognizedItems.length }} 条)
              <span v-if="validItemsCount < recognizedItems.length" class="text-amber-600">
                - {{ validItemsCount }} 条有效
              </span>
            </h4>
            <div class="table-wrapper" :style="{ maxHeight: tableHeight }">
              <RecognizedAssetsTable
                  ref="recognizedAssetsTableRef"
                  :data="recognizedItems"
                  @remove-item="removeItem"
                  @update-item="updateItem"
              />
            </div>
          </div>

          <!-- 验证提示 -->
          <div v-if="validationErrors.length" class="validation-alert">
            <h5 class="validation-title">请完善以下信息：</h5>
            <ul class="validation-list">
              <li v-for="error in validationErrors" :key="error">
                <span class="validation-dot"></span>
                {{ error }}
              </li>
            </ul>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-show="!isProcessing && !hasData && !imagePreview" class="empty-state">
          <LucideScanText class="w-12 h-12 mx-auto mb-4 opacity-50"/>
          <p>请上传图片并开始识别</p>
        </div>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <div v-if="hasData" class="footer-actions">
        <BaseButton type="button" title="取消" color="outline" @click="handleClose"/>
        <BaseButton
            type="button"
            :title="`批量添加 (${validItemsCount} 条)`"
            color="primary"
            :disabled="!canSubmit || isSubmitting"
            @click="handleBatchAdd"
        >
          <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin"/>
          <span>{{ isSubmitting ? '处理中...' : `批量添加 (${validItemsCount} 条)` }}</span>
        </BaseButton>
      </div>
    </template>
  </BaseModal>

  <!-- 辅助组件 -->
  <AssetName ref="assetNameRef" @refresh="loadAssetNames"/>
  <ImageViewer
      :visible="showImageViewer"
      :src="imagePreview"
      @close="showImageViewer = false"
  />
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Loader2, LucideScanText, LucideSettings } from 'lucide-vue-next'
import { useAssetScanModal } from '@/composables/asset/useAssetScanModal'
import { useImageRecognition } from '@/composables/asset/useImageRecognition'
import { useBatchSubmit } from '@/composables/asset/useBatchSubmit'
import { useFormValidation } from '@/composables/asset/useFormValidation'

// Props定义 - 解决警告问题
const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  confirmText?: string
  form?: Record<string, any>
}>(), {
  title: '扫图批量添加',
  confirmText: '批量添加'
})

const emit = defineEmits(['close', 'submit', 'update:visible'])

// 组合式函数
const {
  formData,
  options,
  modalSize,
  initializeDefaults,
  loadAssetNames,
  setDefaultUnit,
  resetForm,
  TODAY
} = useAssetScanModal()

const {
  imageFile,
  imagePreview,
  isProcessing,
  recognizedItems,
  handleImageUpload,
  recognizeImage
} = useImageRecognition(loadAssetNames)

const {
  isSubmitting,
  handleBatchAdd: performBatchAdd
} = useBatchSubmit(formData, recognizedItems, emit)

const {
  validationErrors,
  validItemsCount,
  canSubmit
} = useFormValidation(formData, recognizedItems)

// Refs
const assetNameRef = ref()
const recognizedAssetsTableRef = ref()
const showImageViewer = ref(false)

// 计算属性
const hasData = computed(() => recognizedItems.value.length > 0)

const tableHeight = computed(() =>
    recognizedItems.value.length > 8 ? '400px' : 'none'
)

// 方法
const handleAssetTypeChange = async (value: string | number | (string | number)[] | null) => {
  const assetTypeId = Array.isArray(value) ? value[0] : value
  if (assetTypeId) {
    await setDefaultUnit(String(assetTypeId))
  } else {
    formData.unitId = null
  }
}

const updateItem = (index: number, field: string, value: any) => {
  if (recognizedItems.value[index]) {
    if (field === 'assetNameId') {
      value = value === null || value === undefined || value === '' ? null : String(value)
    }
    (recognizedItems.value[index] as any)[field] = value
  }
}

const removeItem = (index: number) => {
  recognizedItems.value.splice(index, 1)
}

const handleBatchAdd = async () => {
  if (!canSubmit.value) return
  await performBatchAdd()
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  resetForm()
  // 重置本地状态
  showImageViewer.value = false
}

// 生命周期
onMounted(async () => {
  await loadAssetNames()
  await initializeDefaults()
})
</script>

<style scoped>
.modal-body {
  min-height: 450px;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.content-wrapper {
  flex: 1;
  min-height: 350px;
  position: relative;
}

/* 上传区域 */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.image-preview-wrapper {
  position: relative;
  display: inline-block;
}

.preview-image {
  max-height: 12rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  cursor: zoom-in;
  transition: opacity 0.2s;
}

.preview-image:hover {
  opacity: 0.8;
}

.preview-hint {
  position: absolute;
  top: 0;
  left: 0;
  background: linear-gradient(to right, rgba(0,0,0,0.7), transparent);
  color: white;
  font-size: 0.75rem;
  padding: 0.375rem 0.75rem;
  border-radius: 0.5rem 0 1rem 0;
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* 加载骨架屏 */
.loading-skeleton {
  animation: fadeIn 0.3s ease-in-out;
}

.skeleton-section {
  @apply bg-white rounded-lg border border-gray-200 p-4 shadow-sm;
}

.skeleton-title {
  @apply h-4 bg-gray-200 rounded w-1/4 mb-3;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-grid {
  @apply grid grid-cols-5 gap-4;
}

.skeleton-item {
  @apply h-10 bg-gray-200 rounded;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-list {
  @apply space-y-2;
}

.skeleton-row {
  @apply h-12 bg-gray-200 rounded;
  animation: pulse 1.5s ease-in-out infinite;
}

/* 数据内容 */
.data-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  animation: fadeIn 0.3s ease-in-out;
}

.section-card {
  @apply bg-white rounded-lg border border-gray-200 p-4 shadow-sm;
}

.section-title {
  @apply text-sm font-medium text-gray-700 mb-3 block;
}

.table-wrapper {
  @apply overflow-auto;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e0 #f7fafc;
}

.table-wrapper::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.table-wrapper::-webkit-scrollbar-track {
  background: #f7fafc;
}

.table-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
}

/* 验证提示 */
.validation-alert {
  @apply bg-amber-50 border border-amber-200 rounded-lg p-4;
  animation: slideDown 0.3s ease-out;
}

.validation-title {
  @apply text-sm font-medium text-amber-800 mb-2;
}

.validation-list {
  @apply text-sm text-amber-700 space-y-1;
}

.validation-list li {
  @apply flex items-center gap-2;
}

.validation-dot {
  @apply w-1.5 h-1.5 bg-amber-400 rounded-full;
}

/* 空状态 */
.empty-state {
  @apply text-center py-12 text-gray-500;
  animation: fadeIn 0.3s ease-in-out;
}

/* 底部操作 */
.footer-actions {
  @apply flex justify-end gap-3;
  animation: slideUp 0.3s ease-out;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>