<!--src/components/asset/AssetScanAddModal.vue-->
<template>
  <BaseModal
      title="扫图批量添加"
      :visible="visible"
      :width="modalWidth"
      @update:visible="handleClose"
  >
    <!-- 主内容区域 -->
    <div class="space-y-6">
      <!-- 上传操作区 -->
      <div class="space-y-4">
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
              :disabled="isRecognizing"
              color="outline"
          >
            <LucideScanText v-if="!isRecognizing" class="w-5 h-5"/>
            <Loader2 v-else class="w-5 h-5 animate-spin"/>
            <span>{{ isRecognizing ? '识别中...' : '开始识别' }}</span>
          </BaseButton>
        </div>
        <img
            v-if="imagePreview"
            :src="imagePreview"
            alt="预览"
            class="max-h-48 rounded border"
        />
      </div>

      <!-- 统一属性设置 -->
      <div v-if="recognizedData.length" class="section-card">
        <h4 class="text-sm font-medium text-gray-700 mb-3 block">设置共同信息</h4>
        <div class="grid grid-cols-5 gap-4">
          <BaseSelect
              title="资产类型"
              v-model="commonAttributes.assetTypeId"
              :options="assetTypeOptions"
              required
              clearable
              searchable
              @update:model-value="onAssetTypeChange"
          />
          <BaseSelect
              title="资产位置"
              v-model="commonAttributes.assetLocationId"
              :options="assetLocationOptions"
              required
              clearable
              searchable
          />
          <BaseDateInput
              title="登记日期"
              v-model="commonAttributes.acquireTime"
              type="date"
              :max="today"
              required
              clearable
          />
          <BaseSelect
              title="货币单位"
              v-model="commonAttributes.unitId"
              :options="unitOptions"
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

      <!-- 识别结果展示 -->
      <div v-if="recognizedData.length" class="section-card">
        <h4 class="text-sm font-medium text-gray-700 mb-3 block">
          识别结果 ({{ recognizedData.length }} 条)
          <span v-if="validItemsCount < recognizedData.length" class="text-amber-600">
            - {{ validItemsCount }} 条有效
          </span>
        </h4>
        <RecognizedAssetsTable
            ref="recognizedAssetsTableRef"
            :data="recognizedData"
            @remove-item="removeItem"
            @update-item="updateItem"
        />
      </div>

      <!-- 校验提示 -->
      <div v-if="recognizedData.length && validationErrors.length"
           class="bg-amber-50 border border-amber-200 rounded-lg p-4">
        <h5 class="text-sm font-medium text-amber-800 mb-2">请完善以下信息：</h5>
        <ul class="text-sm text-amber-700 space-y-1">
          <li v-for="error in validationErrors" :key="error" class="flex items-center gap-2">
            <span class="w-1.5 h-1.5 bg-amber-400 rounded-full"></span>
            {{ error }}
          </li>
        </ul>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer v-if="recognizedData.length > 0">
      <div class="flex justify-end gap-3">
        <BaseButton type="button" title="取消" color="outline" @click="handleClose"/>
        <BaseButton
            type="button"
            title="批量添加"
            color="primary"
            :disabled="!canSubmit || isSubmitting"
            @click="handleSubmit"
        >
          <Loader2 v-if="isSubmitting" class="w-4 h-4 animate-spin"/>
          <span>{{ isSubmitting ? '处理中...' : `批量添加 (${validItemsCount} 条)` }}</span>
        </BaseButton>
      </div>
    </template>
  </BaseModal>

  <AssetName ref="assetNameRef" @refresh="refreshAssetNames"/>
</template>

<script setup lang="ts">
import {ref, computed, onMounted, watch, nextTick} from 'vue'
import {Loader2, LucideScanText, LucideSettings} from 'lucide-vue-next'
import {useAssetStore} from '@/store/assetStore'
import {useMetaStore} from '@/store/metaStore'
import {useAssetNameStore} from '@/store/assetNameStore'
import emitter from '@/utils/eventBus'
import {RawAssetRecord, RecognizedAssetItem, BatchAddResult} from '@/types/asset'

import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import RecognizedAssetsTable from '@/components/asset/RecognizedAssetsTable.vue'
import BaseDateInput from "@/components/base/BaseDateInput.vue"
import AssetName from "@/components/asset/assetName/AssetName.vue"

const props = defineProps<{
  visible: boolean
  form?: any
  title?: string
  confirmText?: string
}>()

const emit = defineEmits(['close', 'submit', 'update:visible'])

// Stores
const assetStore = useAssetStore()
const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

// Refs
const recognizedAssetsTableRef = ref()
const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const assetNameRef = ref()
const isRecognizing = ref(false)
const isSubmitting = ref(false)
const today = new Date().toISOString().slice(0, 10)

// 识别数据
const recognizedData = ref<RecognizedAssetItem[]>([])

// 公共属性
const commonAttributes = ref({
  assetTypeId: null as string | null,
  assetLocationId: null as string | null,
  acquireTime: today,
  unitId: null as string | null
})

// 动态计算弹窗宽度
const modalWidth = computed(() => {
  if (recognizedData.value.length === 0) {
    return '600px'
  }
  return '1350px'
})

// 计算属性 - 选项数据
const assetTypeOptions = computed(() =>
    metaStore.typeMap?.ASSET_TYPE?.map(i => ({label: String(i.value1), value: i.id})) || []
)

const assetLocationOptions = computed(() =>
    metaStore.typeMap?.ASSET_LOCATION?.map(i => ({label: String(i.value1), value: i.id})) || []
)

const unitOptions = computed(() =>
    metaStore.typeMap?.UNIT?.map(i => ({
      label: String(i.value1),
      value: i.id
    })) || []
)

// 计算属性 - 校验逻辑
const validationErrors = computed(() => {
  const errors: string[] = []

  if (!commonAttributes.value.assetTypeId) {
    errors.push('请选择资产类型')
  }

  if (!commonAttributes.value.assetLocationId) {
    errors.push('请选择资产位置')
  }

  if (!commonAttributes.value.acquireTime) {
    errors.push('请选择登记日期')
  }

  if (!commonAttributes.value.unitId) {
    errors.push('请选择货币单位')
  }

  // 检查识别数据
  const invalidItems = recognizedData.value.filter(item =>
      !item.assetNameId || !item.amount || item.amount <= 0
  )

  if (invalidItems.length > 0) {
    errors.push(`有 ${invalidItems.length} 条记录缺少资产名称或金额无效`)
  }

  return errors
})

const validItemsCount = computed(() => {
  return recognizedData.value.filter(item =>
      item.assetNameId && item.amount && item.amount > 0
  ).length
})

const canSubmit = computed(() => {
  return recognizedData.value.length > 0 &&
      validationErrors.value.length === 0 &&
      validItemsCount.value > 0
})

// 安全处理ID的函数
function safeParseId(id: any): string | null {
  if (id === null || id === undefined || id === '') {
    return null
  }
  return String(id)
}

// 强制加载资产名称数据的方法
async function forceLoadAssetNames() {
  try {
    if (assetNameStore.fetchAssetName) {
      await assetNameStore.fetchAssetName(true)
    }

    if ((!assetNameStore.assetName || assetNameStore.assetName.length === 0) && assetNameStore.loadList) {
      await assetNameStore.loadList(true)
    }

    await nextTick()
  } catch (error) {
    console.error('加载资产名称数据失败:', error)
  }
}

// 方法 - 图片处理
function handleImageUpload(file: File) {
  imageFile.value = file
  imagePreview.value = ''          // 👉 清空，先不显示
  recognizedData.value = []
}

async function recognizeImage() {
  if (!imageFile.value) return

  isRecognizing.value = true
  try {
    await forceLoadAssetNames()

    if (recognizedAssetsTableRef.value?.forceLoadAssetNames) {
      await recognizedAssetsTableRef.value.forceLoadAssetNames()
    }

    const formData = new FormData()
    formData.append('file', imageFile.value)

    const result = await assetStore.recognizeAssetImage(formData)

    recognizedData.value = (result || []).map((item: any) => ({
      ...item,
      assetNameId: safeParseId(item.assetNameId),
      amount: item.amount || null,
      remark: item.remark || '',
      matchScore: item.matchScore || 0,
      matchedAssetName: item.matchedAssetName || '',
      originalAssetName: item.originalAssetName || item.assetName || ''
    })) as RecognizedAssetItem[]

    emitter.emit('notify', {
      type: recognizedData.value.length ? 'success' : 'warning',
      message: recognizedData.value.length
          ? `成功识别 ${recognizedData.value.length} 条数据`
          : '未识别到有效数据'
    })
  } catch (err) {
    emitter.emit('notify', {
      message: '图片识别失败，请重试或检查网络',
      type: 'error'
    })
    console.error('识别失败:', err)
  } finally {
    isRecognizing.value = false
    // 👉 此处才设置预览，避免图片加载与内容刷新叠加造成闪现
    if (imageFile.value) {
      imagePreview.value = URL.createObjectURL(imageFile.value)
    }
  }
}

function updateItem(index: number, field: string, value: any) {
  if (recognizedData.value[index]) {
    if (field === 'assetNameId') {
      value = safeParseId(value)
    }
    ;(recognizedData.value[index] as any)[field] = value
  }
}

function removeItem(index: number) {
  recognizedData.value.splice(index, 1)
}

function validateForm(): boolean {
  if (validationErrors.value.length > 0) {
    emitter.emit('notify', {
      type: 'warning',
      message: `请完善信息：${validationErrors.value.join('；')}`
    })
    return false
  }

  if (validItemsCount.value === 0) {
    emitter.emit('notify', {
      type: 'warning',
      message: '没有有效的数据可提交'
    })
    return false
  }

  return true
}

// 🔥 修复 executeBatchAdd 方法
async function executeBatchAdd(
    records: RawAssetRecord[],
    forceOverwrite: boolean,
    copyLast: boolean = false
): Promise<boolean> {
  try {
    console.log('=== executeBatchAdd 开始 ===')
    console.log('参数检查:', {
      records: records,
      recordsType: typeof records,
      isArray: Array.isArray(records),
      length: records?.length,
      forceOverwrite,
      copyLast
    })

    if (!records || !Array.isArray(records)) {
      console.error('records 参数错误:', records)
      throw new Error('记录数据格式错误')
    }

    if (records.length === 0) {
      throw new Error('没有要处理的记录')
    }

    // 🔥 直接调用 smartBatchAddRecords，不要经过其他方法
    const result = await assetStore.smartBatchAddRecords(records, forceOverwrite, copyLast)

    if (result) {
      let message = result.message || `批量操作完成：成功处理 ${result.successCount} 条记录`
      const details: string[] = []

      if (result.copied) {
        details.push('已复制历史记录')
      }
      if (result.overwrote) {
        details.push('已覆盖今日记录')
      }
      if (result.updateCount && result.updateCount > 0) {
        details.push(`更新${result.updateCount}条`)
      }
      if (result.addCount && result.addCount > 0) {
        details.push(`新增${result.addCount}条`)
      }

      if (details.length > 0) {
        message = `${message}（${details.join('，')}）`
      }

      emitter.emit('notify', {
        type: 'success',
        message
      })

      // 🔥 直接触发 submit 事件，不要包装数据
      emit('submit', records) // 直接传递 records 数组

      return true
    }

    return false
  } catch (error: any) {
    console.error('executeBatchAdd 错误:', error)
    emitter.emit('notify', {
      type: 'error',
      message: `批量添加失败：${error.message || '未知错误'}`
    })
    return false
  }
}

// 🔥 修复 handleSubmit 方法，增加详细的调试日志
async function handleSubmit() {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    console.log('=== 开始 handleSubmit ===')
    console.log('recognizedData:', recognizedData.value)
    console.log('commonAttributes:', commonAttributes.value)

    // 准备有效记录
    const validItems = recognizedData.value.filter(item => {
      const isValid = item.assetNameId && item.amount && item.amount > 0
      console.log('校验记录:', item, '有效:', isValid)
      return isValid
    })

    console.log('有效记录数:', validItems.length)

    if (validItems.length === 0) {
      throw new Error('没有有效的记录可提交')
    }

    // 🔥 修复：确保所有字段都正确设置
    const records: RawAssetRecord[] = validItems.map((item, index) => {
      const record = {
        id: String(Date.now() + index),
        assetNameId: String(item.assetNameId!),
        assetLocationId: String(commonAttributes.value.assetLocationId!),
        assetTypeId: String(commonAttributes.value.assetTypeId!),
        unitId: String(commonAttributes.value.unitId!),
        amount: Number(item.amount!),
        acquireTime: commonAttributes.value.acquireTime,
        remark: item.remark || ''
      }

      console.log(`构建记录 ${index}:`, record)
      return record
    })

    console.log('最终构建的 records 数组:', records)
    console.log('records 类型检查:', typeof records, Array.isArray(records))

    // 检查今日是否已有记录
    const hasRecordsToday = await assetStore.checkTodayRecords()
    console.log('今日是否有记录:', hasRecordsToday)

    if (hasRecordsToday) {
      // 今日已有记录的处理逻辑
      emitter.emit('confirm', {
        title: '今日已有记录',
        message: `检测到今日已有记录，请选择处理方式：

• 智能合并：保留现有记录，更新相同资产名称的金额，添加新资产
• 完全覆盖：删除今日所有记录后重新添加

将处理 ${records.length} 条记录`,
        type: 'primary',
        confirmText: '智能合并',
        cancelText: '完全覆盖',
        onConfirm: async () => {
          console.log('用户选择：智能合并')
          const success = await executeBatchAdd(records, false, false)
          if (success) handleClose()
        },
        onCancel: async () => {
          console.log('用户选择：完全覆盖')
          emitter.emit('confirm', {
            title: '确认覆盖',
            message: '⚠️ 此操作将删除今日所有现有记录，是否确认？',
            type: 'danger',
            confirmText: '确认覆盖',
            cancelText: '取消',
            onConfirm: async () => {
              console.log('用户确认覆盖')
              const success = await executeBatchAdd(records, true, false)
              if (success) handleClose()
            }
          })
        }
      })
    } else {
      // 今日无记录的处理逻辑
      emitter.emit('confirm', {
        title: '是否复制历史记录',
        message: `今日暂无记录，请选择操作方式：

• 复制并添加：先复制上次记录作为基础，再添加 ${records.length} 条新记录
• 仅添加新记录：直接添加 ${records.length} 条新记录`,
        type: 'primary',
        confirmText: '复制并添加',
        cancelText: '仅添加新记录',
        onConfirm: async () => {
          console.log('用户选择：复制并添加')
          const success = await executeBatchAdd(records, false, true)
          if (success) handleClose()
        },
        onCancel: async () => {
          console.log('用户选择：仅添加新记录')
          const success = await executeBatchAdd(records, false, false)
          if (success) handleClose()
        }
      })
    }
  } catch (error: any) {
    console.error('handleSubmit 错误:', error)
    emitter.emit('notify', {
      type: 'error',
      message: `操作失败：${error.message || '未知错误'}`
    })
  } finally {
    isSubmitting.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
  emit('close')
  resetForm()
}

function resetForm() {
  if (imagePreview.value && imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
  }
  imageFile.value = null
  imagePreview.value = ''
  recognizedData.value = []
  commonAttributes.value = {
    assetTypeId: null,
    assetLocationId: null,
    acquireTime: today,
    unitId: null
  }
  isSubmitting.value = false
}

function setFieldValue(field: string, value: any) {
  if (field in commonAttributes.value) {
    (commonAttributes.value as any)[field] = value
  }
}

async function setDefaultUnit(
    typeId: string,
    setFieldValue?: (field: string, value: any) => void,
    values?: { unitId?: string | number | null }
) {
  const fitnessTypes = metaStore.typeMap?.FITNESS_TYPE || []
  const assetTypes = metaStore.typeMap?.ASSET_TYPE || []
  const unitList = metaStore.typeMap?.UNIT || []

  const types = [...fitnessTypes, ...assetTypes]
  const selectedType = types.find(type => String(type.id) === String(typeId))

  if (!selectedType?.key3) {
    setFieldValue?.('unitId', null)
    return
  }

  const defaultUnit = unitList.find(unit => unit.key1 === selectedType.key3)

  if (!defaultUnit) {
    return
  }

  const currentUnitId = values?.unitId
  if (!currentUnitId || String(currentUnitId) !== String(defaultUnit.id)) {
    setFieldValue?.('unitId', defaultUnit.id)

    emitter.emit('notify', {
      type: 'info',
      message: `已自动设置默认单位为：${defaultUnit.value1}`
    })
  }
}

function onAssetTypeChange(value: string | number | (string | number)[] | null) {
  const assetTypeId = Array.isArray(value) ? value[0] : value

  if (!assetTypeId) {
    commonAttributes.value.unitId = null
    return
  }

  setDefaultUnit(String(assetTypeId), setFieldValue, {
    unitId: commonAttributes.value.unitId
  })
}

onMounted(async () => {
  await forceLoadAssetNames()

  // 设置默认资产类型为“理财”
  const defaultAssetType = metaStore.typeMap?.ASSET_TYPE?.find(
      (item) => item.value1 === '理财'
  )
  if (defaultAssetType) {
    commonAttributes.value.assetTypeId = String(defaultAssetType.id)
    await setDefaultUnit(String(defaultAssetType.id), setFieldValue, {
      unitId: commonAttributes.value.unitId
    })
  }

  // 设置默认资产位置为“兴业银行”
  const defaultAssetLocation = metaStore.typeMap?.ASSET_LOCATION?.find(
      (item) => item.value1 === '兴业银行'
  )
  if (defaultAssetLocation) {
    commonAttributes.value.assetLocationId = String(defaultAssetLocation.id)
  }
})

function refreshAssetNames() {
  forceLoadAssetNames()
}
</script>