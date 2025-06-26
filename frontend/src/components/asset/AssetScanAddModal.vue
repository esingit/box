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
              @update:model-value="onAssetTypeChange"
          />
          <BaseSelect
              title="资产位置"
              v-model="commonAttributes.assetLocationId"
              :options="assetLocationOptions"
              required
              clearable
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

    <!-- 底部按钮 - 使用 footer 插槽，只有有数据时才显示 -->
    <template #footer v-if="recognizedData.length > 0">
      <div class="flex justify-end gap-3">
        <BaseButton type="button" title="取消" color="outline" @click="handleClose"/>
        <BaseButton
            type="button"
            title="批量添加"
            color="primary"
            :disabled="!canSubmit"
            @click="handleSubmit"
        >
          批量添加 ({{ validItemsCount }} 条)
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
import {RawAssetRecord, RecognizedAssetItem} from '@/types/asset'

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

// 安全处理大数ID的函数
function safeParseId(id: any): string | null {
  if (id === null || id === undefined || id === '') {
    return null
  }

  // 如果已经是字符串，直接返回
  if (typeof id === 'string') {
    return id
  }

  // 如果是数字，转换为字符串（但可能已经丢失精度）
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
  recognizedData.value = []

  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

async function recognizeImage() {
  if (!imageFile.value) return

  isRecognizing.value = true
  try {
    // 确保资产名称数据已加载
    await forceLoadAssetNames()

    if (recognizedAssetsTableRef.value?.forceLoadAssetNames) {
      await recognizedAssetsTableRef.value.forceLoadAssetNames()
    }

    const formData = new FormData()
    formData.append('file', imageFile.value)

    const result = await assetStore.recognizeAssetImage(formData)

    // 处理识别结果 - 保持ID为字符串类型避免精度丢失
    recognizedData.value = (result || []).map((item: any) => ({
      ...item,
      // 关键修改：保持ID为字符串类型，避免精度丢失
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
  }
}

function updateItem(index: number, field: string, value: any) {
  if (recognizedData.value[index]) {
    if (field === 'assetNameId') {
      // 保持为字符串类型
      value = safeParseId(value)
    }
    ;(recognizedData.value[index] as any)[field] = value
  }
}

// 方法 - 数据操作
function removeItem(index: number) {
  recognizedData.value.splice(index, 1)
}

// 方法 - ��单操作
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

function handleSubmit() {
  if (!validateForm()) return

  // 只提交有效的记录
  const validItems = recognizedData.value.filter(item =>
      item.assetNameId && item.amount && item.amount > 0
  )

  // 转换为 RawAssetRecord 格式
  const records: RawAssetRecord[] = validItems.map((item, index) => ({
    id: Date.now() + index,
    assetNameId: item.assetNameId!, // 保持字符串类型
    assetLocationId: commonAttributes.value.assetLocationId!,
    assetTypeId: commonAttributes.value.assetTypeId!,
    unitId: commonAttributes.value.unitId!,
    amount: item.amount!,
    date: commonAttributes.value.acquireTime,
    remark: item.remark || ''
  }))

  emitter.emit('notify', {
    type: 'success',
    message: `准备添加 ${records.length} 条资产记录`
  })

  emit('submit', records)
  handleClose()
}

function handleClose() {
  emit('update:visible', false)
  emit('close')
  resetForm()
}

function resetForm() {
  imageFile.value = null
  imagePreview.value = ''
  recognizedData.value = []
  commonAttributes.value = {
    assetTypeId: null,
    assetLocationId: null,
    acquireTime: today,
    unitId: null
  }
}

// 修改 setFieldValue
function setFieldValue(field: string, value: any) {
  if (field in commonAttributes.value) {
    (commonAttributes.value as any)[field] = value
  }
}

// 修改 setDefaultUnit
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

// 修复类型错误：修改函数签名以匹配期望的类型
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

// 在 mounted 中加载必要数据
onMounted(async () => {
  await forceLoadAssetNames()
})

// 监听可见性变化，确保每次打开时都有最新数据
watch(() => props.visible, async (newVal) => {
  if (newVal) {
    await forceLoadAssetNames()
  }
})

function refreshAssetNames() {
  forceLoadAssetNames()
}
</script>