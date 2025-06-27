<template>
  <BaseModal
      title="扫图批量添加"
      :visible="visible"
      :width="modalSize.width"
      :height="modalSize.height"
      class="scan-batch-modal"
      @update:visible="handleClose"
  >
    <!-- 主内容区域 - 固定最小高度防止抖动 -->
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

      <!-- 内容区域 - 使用v-show避免DOM重建 -->
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
                  class="w-60"
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
import { computed, ref, reactive, onMounted, nextTick } from 'vue'
import { Loader2, LucideScanText, LucideSettings } from 'lucide-vue-next'
import { useAssetStore } from '@/store/assetStore'
import { useMetaStore } from '@/store/metaStore'
import { useAssetNameStore } from '@/store/assetNameStore'
import emitter from '@/utils/eventBus'
import { RawAssetRecord, RecognizedAssetItem } from '@/types/asset'

// Props & Emits
const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['close', 'submit', 'update:visible'])

// Constants
const TODAY = new Date().toISOString().slice(0, 10)

// Stores
const assetStore = useAssetStore()
const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

// Refs
const assetNameRef = ref()
const recognizedAssetsTableRef = ref()

// State
const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const showImageViewer = ref(false)
const isProcessing = ref(false)
const isSubmitting = ref(false)
const recognizedItems = ref<RecognizedAssetItem[]>([])

// Form data
const formData = reactive({
  assetTypeId: null as string | null,
  assetLocationId: null as string | null,
  acquireTime: TODAY,
  unitId: null as string | null
})

// 计算属性
const modalSize = computed(() => {
  // 使用图片上传作为触发点，避免频繁变化
  const isExpanded = !!imagePreview.value
  return {
    width: isExpanded ? '1400px' : '900px',
    height: isExpanded ? '900px' : '600px'
  }
})

const hasData = computed(() => recognizedItems.value.length > 0)

const tableHeight = computed(() =>
    recognizedItems.value.length > 8 ? '400px' : 'none'
)

const options = computed(() => ({
  assetTypes: metaStore.typeMap?.ASSET_TYPE?.map(i => ({
    label: String(i.value1),
    value: i.id
  })) || [],
  assetLocations: metaStore.typeMap?.ASSET_LOCATION?.map(i => ({
    label: String(i.value1),
    value: i.id
  })) || [],
  units: metaStore.typeMap?.UNIT?.map(i => ({
    label: String(i.value1),
    value: i.id
  })) || []
}))

const validationErrors = computed(() => {
  const errors: string[] = []

  if (!formData.assetTypeId) errors.push('请选择资产类型')
  if (!formData.assetLocationId) errors.push('请选择资产位置')
  if (!formData.acquireTime) errors.push('请选择登记日期')
  if (!formData.unitId) errors.push('请选择货币单位')

  const invalidItems = recognizedItems.value.filter(item =>
      !item.assetNameId || !item.amount || item.amount <= 0
  )

  if (invalidItems.length > 0) {
    errors.push(`有 ${invalidItems.length} 条记录缺少资产名称或金额无效`)
  }

  return errors
})

const validItemsCount = computed(() => {
  return recognizedItems.value.filter(item =>
      item.assetNameId && item.amount && item.amount > 0
  ).length
})

const canSubmit = computed(() =>
    hasData.value && validationErrors.value.length === 0 && validItemsCount.value > 0
)

// 方法
function handleImageUpload(file: File) {
  imageFile.value = file
  recognizedItems.value = []

  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

async function recognizeImage() {
  if (!imageFile.value) return

  isProcessing.value = true
  recognizedItems.value = []

  try {
    await loadAssetNames()

    const formData = new FormData()
    formData.append('file', imageFile.value)

    const result = await assetStore.recognizeAssetImage(formData)

    await nextTick()

    recognizedItems.value = processRecognitionResult(result)

    emitter.emit('notify', {
      type: recognizedItems.value.length ? 'success' : 'warning',
      message: recognizedItems.value.length
          ? `成功识别 ${recognizedItems.value.length} 条数据`
          : '未识别到有效数据'
    })
  } catch (err) {
    emitter.emit('notify', {
      message: '图片识别失败，请重试',
      type: 'error'
    })
  } finally {
    isProcessing.value = false
  }
}

function processRecognitionResult(result: any[]): RecognizedAssetItem[] {
  return (result || []).map(item => ({
    ...item,
    assetNameId: safeParseId(item.assetNameId),
    amount: item.amount || null,
    remark: item.remark || '',
    matchScore: item.matchScore || 0,
    matchedAssetName: item.matchedAssetName || '',
    originalAssetName: item.originalAssetName || item.assetName || ''
  }))
}

function safeParseId(id: any): string | null {
  return id === null || id === undefined || id === '' ? null : String(id)
}

function updateItem(index: number, field: string, value: any) {
  if (recognizedItems.value[index]) {
    if (field === 'assetNameId') {
      value = safeParseId(value)
    }
    (recognizedItems.value[index] as any)[field] = value
  }
}

function removeItem(index: number) {
  recognizedItems.value.splice(index, 1)
}

async function handleAssetTypeChange(value: string | number | (string | number)[] | null) {
  const assetTypeId = Array.isArray(value) ? value[0] : value
  if (!assetTypeId) {
    formData.unitId = null
    return
  }

  await setDefaultUnit(String(assetTypeId))
}

async function setDefaultUnit(typeId: string) {
  const assetTypes = metaStore.typeMap?.ASSET_TYPE || []
  const unitList = metaStore.typeMap?.UNIT || []

  const selectedType = assetTypes.find(type => String(type.id) === typeId)
  if (!selectedType?.key3) return

  const defaultUnit = unitList.find(unit => unit.key1 === selectedType.key3)
  if (!defaultUnit) return

  if (!formData.unitId || String(formData.unitId) !== String(defaultUnit.id)) {
    formData.unitId = defaultUnit.id
    emitter.emit('notify', {
      type: 'info',
      message: `已自动设置默认单位为：${defaultUnit.value1}`
    })
  }
}

async function handleBatchAdd() {
  if (!canSubmit.value) return

  isSubmitting.value = true

  try {
    const validItems = recognizedItems.value.filter(item =>
        item.assetNameId && item.amount && item.amount > 0
    )

    const records: RawAssetRecord[] = validItems.map((item, index) => ({
      id: String(Date.now() + index),
      assetNameId: String(item.assetNameId!),
      assetLocationId: String(formData.assetLocationId!),
      assetTypeId: String(formData.assetTypeId!),
      unitId: String(formData.unitId!),
      amount: Number(item.amount!),
      acquireTime: formData.acquireTime,
      remark: item.remark || ''
    }))

    const hasRecordsToday = await assetStore.checkTodayRecords()

    if (hasRecordsToday) {
      await handleExistingRecords(records)
    } else {
      await handleNoRecords(records)
    }
  } catch (error: any) {
    emitter.emit('notify', {
      type: 'error',
      message: `操作失败：${error.message || '未知错误'}`
    })
  } finally {
    isSubmitting.value = false
  }
}

async function handleExistingRecords(records: RawAssetRecord[]) {
  emitter.emit('confirm', {
    title: '今日已有记录',
    message: `检测到今日已有记录，请选择处理方式：\n\n• 智能合并：保留现有记录，更新相同资产名称的金额，添加新资产\n• 完全覆盖：删除今日所有记录后重新添加\n\n将处理 ${records.length} 条记录`,
    type: 'primary',
    confirmText: '智能合并',
    cancelText: '完全覆盖',
    onConfirm: async () => {
      const success = await executeBatchAdd(records, false, false)
      if (success) handleClose()
    },
    onCancel: async () => {
      emitter.emit('confirm', {
        title: '确认覆盖',
        message: '⚠️ 此操作将删除今日所有现有记录，是否确认？',
        type: 'danger',
        confirmText: '确认覆盖',
        cancelText: '取消',
        onConfirm: async () => {
          const success = await executeBatchAdd(records, true, false)
          if (success) handleClose()
        }
      })
    }
  })
}

async function handleNoRecords(records: RawAssetRecord[]) {
  emitter.emit('confirm', {
    title: '是否复制历史记录',
    message: `今日暂无记录，请选择操作方式：\n\n• 复制并添加：先复制上次记录作为基础，再添加 ${records.length} 条新记录\n• 仅添加新记录：直接添加 ${records.length} 条新记录`,
    type: 'primary',
    confirmText: '复制并添加',
    cancelText: '仅添加新记录',
    onConfirm: async () => {
      const success = await executeBatchAdd(records, false, true)
      if (success) handleClose()
    },
    onCancel: async () => {
      const success = await executeBatchAdd(records, false, false)
      if (success) handleClose()
    }
  })
}

async function executeBatchAdd(
    records: RawAssetRecord[],
    forceOverwrite: boolean,
    copyLast: boolean = false
): Promise<boolean> {
  try {
    const result = await assetStore.smartBatchAddRecords(records, forceOverwrite, copyLast)

    if (result) {
      const details: string[] = []
      if (result.copied) details.push('已复制历史记录')
      if (result.overwrote) details.push('已覆盖今日记录')
      if (result.updateCount > 0) details.push(`更新${result.updateCount}条`)
      if (result.addCount > 0) details.push(`新增${result.addCount}条`)

      emitter.emit('notify', {
        type: 'success',
        message: `批量操作完成：成功处理 ${result.successCount} 条记录${details.length ? `（${details.join('，')}）` : ''}`
      })

      emit('submit', records)
      return true
    }

    return false
  } catch (error: any) {
    emitter.emit('notify', {
      type: 'error',
      message: `批量添加失败：${error.message || '未知错误'}`
    })
    return false
  }
}

function handleClose() {
  emit('update:visible', false)
  emit('close')
  resetForm()
}

function resetForm() {
  imageFile.value = null
  imagePreview.value = ''
  recognizedItems.value = []
  showImageViewer.value = false
  Object.assign(formData, {
    assetTypeId: null,
    assetLocationId: null,
    acquireTime: TODAY,
    unitId: null
  })
  isSubmitting.value = false
}

async function loadAssetNames() {
  try {
    await assetNameStore.fetchAssetName(true)

    if ((!assetNameStore.assetName || assetNameStore.assetName.length === 0) && assetNameStore.loadList) {
      await assetNameStore.loadList(true)
    }

    if (recognizedAssetsTableRef.value?.forceLoadAssetNames) {
      await recognizedAssetsTableRef.value.forceLoadAssetNames()
    }
  } catch (error) {
    console.error('加载资产名称数据失败:', error)
  }
}

async function initializeDefaults() {
  // 设置默认资产类型
  const defaultAssetType = metaStore.typeMap?.ASSET_TYPE?.find(
      item => item.value1 === '理财'
  )
  if (defaultAssetType) {
    formData.assetTypeId = String(defaultAssetType.id)
    await setDefaultUnit(String(defaultAssetType.id))
  }

  // 设置默认资产位置
  const defaultAssetLocation = metaStore.typeMap?.ASSET_LOCATION?.find(
      item => item.value1 === '兴业银行'
  )
  if (defaultAssetLocation) {
    formData.assetLocationId = String(defaultAssetLocation.id)
  }
}

// 生命周期
onMounted(async () => {
  await loadAssetNames()
  await initializeDefaults()
})
</script>

<style scoped>
/* 防闪屏核心样式 */
.scan-batch-modal {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

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