<template>
  <BaseModal title="扫图批量添加" :visible="visible" width="1350px" @update:visible="handleClose">
    <div class="p-4 space-y-6">
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
              upload
              @click="recognizeImage"
              :disabled="isRecognizing"
              color="outline"
              @change="handleImageUpload"
          >
            <LucideScanText v-if="!isRecognizing" class="w-5 h-5" />
            <Loader2 v-else class="w-5 h-5 animate-spin" />
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
        <h4 class="text-sm font-medium text-gray-700 mb-1 block">设置共同信息</h4>
        <div class="grid grid-cols-5 gap-4">
          <BaseSelect
              title="资产类型"
              v-model="commonAttributes.assetTypeId"
              :options="assetTypeOptions"
              required
              clearable
              @change="onAssetTypeChange"
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
                v-model="commonAttributes.unit"
                :options="units"
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
        <h4 class="text-sm font-medium text-gray-700 mb-1 block">识别结果 ({{ recognizedData.length }} 条)</h4>
        <RecognizedAssetsTable :data="recognizedData" @remove-item="removeItem" />
      </div>

      <!-- 操作按钮 -->
      <div class="flex justify-end gap-3">
        <BaseButton type="button" title="取消" color="outline" @click="handleClose" />
        <BaseButton
            type="button"
            title="批量添加"
            color="primary"
            :disabled="!canSubmit"
            @click="handleSubmit"
        >
          批量添加 ({{ recognizedData.length }} 条)
        </BaseButton>
      </div>
    </div>
  </BaseModal>
  <AssetName ref="assetNameRef" @refresh="refreshAssetNames"/>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Loader2, LucideScanText, LucideSettings } from 'lucide-vue-next'
import { useAssetStore } from '@/store/assetStore'
import { useMetaStore } from '@/store/metaStore'
import emitter from '@/utils/eventBus'
import { RawAssetRecord, RecognizedAssetItem } from '@/types/asset'

import {useAssetNameStore} from '@/store/assetNameStore'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import RecognizedAssetsTable from '@/components/asset/RecognizedAssetsTable.vue'
import BaseDateInput from "@/components/base/BaseDateInput.vue";
import AssetName from "@/components/asset/assetName/AssetName.vue";

const props = defineProps<{
  visible: boolean
  form?: any
  title?: string
  confirmText?: string
}>()
const emit = defineEmits(['close', 'submit', 'update:visible'])
const assetStore = useAssetStore()
const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

const imageFile = ref<File | null>(null)
const imagePreview = ref('')
const assetNameRef = ref()
const isRecognizing = ref(false)
const today = new Date().toISOString().slice(0, 10)

// 修改为识别专用的数据结构
const recognizedData = ref<RecognizedAssetItem[]>([])

const commonAttributes = ref({ assetTypeId: null, assetLocationId: null , acquireTime: today, unit: null })

const assetTypeOptions = computed(() =>
    metaStore.typeMap?.ASSET_TYPE?.map(i => ({ label: String(i.value1), value: i.id })) || []
)
const assetLocationOptions = computed(() =>
    metaStore.typeMap?.ASSET_LOCATION?.map(i => ({ label: String(i.value1), value: i.id })) || []
)
const units = computed(() => metaStore.typeMap?.UNIT?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])

const canSubmit = computed(() =>
    recognizedData.value.length > 0 &&
    commonAttributes.value.assetTypeId !== null &&
    commonAttributes.value.assetLocationId !== null &&
    recognizedData.value.every(item => item.assetName && item.amount > 0)
)

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
    const formData = new FormData()
    formData.append('image', imageFile.value)
    const result = await assetStore.recognizeAssetImage(formData)
    // 确保结果是 RecognizedAssetItem[] 格式
    recognizedData.value = (result || []) as RecognizedAssetItem[]
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
  } finally {
    isRecognizing.value = false
  }
}

function removeItem(index: number) {
  recognizedData.value.splice(index, 1)
}

function handleSubmit() {
  if (!canSubmit.value) {
    emitter.emit('notify', {
      type: 'warning',
      message: '请检查所有数据，确保名称和金额有效，且已选择资产类型和位置'
    })
    return
  }

  // 转换为 RawAssetRecord 格式
  const records: RawAssetRecord[] = recognizedData.value.map((item, index) => ({
    id: Date.now() + index, // 生成临时ID
    assetNameId: item.assetName, // 将 assetName 映射到 assetNameId
    assetLocationId: commonAttributes.value.assetLocationId!,
    assetTypeId: commonAttributes.value.assetTypeId!,
    unitId: item.unit || 'default', // 设置默认单位
    amount: item.amount,
    date: new Date().toISOString().split('T')[0],
    remark: '批量导入'
  }))

  emit('submit', records)
  handleClose()
}

function handleClose() {
  emit('close')
  imageFile.value = null
  imagePreview.value = ''
  recognizedData.value = []
  commonAttributes.value.assetTypeId = null
  commonAttributes.value.assetLocationId = null
}

function onAssetTypeChange() {
}

function refreshAssetNames() {
  assetNameStore.fetchAssetName()
}
</script>