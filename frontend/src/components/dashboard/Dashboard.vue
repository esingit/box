<!--src/components/dashboard/Dashboard.vue-->
<template>
  <div class="min-h-screen max-w-6xl mx-auto bg-gray-50 rounded-xl flex flex-col space-y-8">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="py-12 flex justify-center items-center text-gray-500 gap-3">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-gray-900"></div>
      正在加载统计数据...
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="py-12 text-center">
      <AlertCircle class="mx-auto w-16 h-16 mb-4"/>
      <h3 class="mb-2 text-lg font-semibold text-gray-800">数据加载失败</h3>
      <p class="mb-4 text-gray-600">{{ loadError }}</p>
      <BaseButton
          type="button"
          title="重新加载"
          color="primary"
          @click="initializeData"
      />
    </div>

    <!-- 主内容 -->
    <div v-else class="grid gap-6">
      <FitnessStats
          :key="`fitness-${route.fullPath}`"
          :unitOptions="unitOptions"
          :fitnessTypeOptions="fitnessTypeOptions"
      />

      <AssetStats
          :key="`asset-${route.fullPath}`"
          :asset-name-options="assetNameOptions"
          :asset-type-options="assetTypeOptions"
          :asset-location-options="assetLocationOptions"
          :unit-options="unitOptions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {storeToRefs} from 'pinia'
import {AlertCircle} from 'lucide-vue-next'
import {useMetaStore} from '@/store/metaStore'
import {useAssetNameStore} from '@/store/assetNameStore'

import FitnessStats from './components/FitnessStats.vue'
import AssetStats from './components/AssetStats.vue'

// 类型定义，仅传入组件用 label/value
interface Option {
  label: string
  value: string | number
}

const route = useRoute()
const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

const isLoading = ref(true)
const loadError = ref('')

/** 公共映射函数：元数据 => Option[] */
function mapToOption(arr: any[] | undefined): Option[] {
  if (!arr || !Array.isArray(arr)) return []
  return arr
      .filter(item => item && item.id && item.value1)
      .map(item => ({
        label: item.value1,
        value: item.id,
      }))
}

// 基础选项：单位、健身类型、资产类型、资产位置（都用 Option[]）
const unitOptions = computed<Option[]>(() => mapToOption(metaStore.typeMap?.UNIT))
const fitnessTypeOptions = computed<Option[]>(() => mapToOption(metaStore.typeMap?.FITNESS_TYPE))
const assetTypeOptions = computed<Option[]>(() => mapToOption(metaStore.typeMap?.ASSET_TYPE))
const assetLocationOptions = computed<Option[]>(() => mapToOption(metaStore.typeMap?.ASSET_LOCATION))

// 资产名称选项（来自 store 的 assetName 数据）
const {assetName} = storeToRefs(assetNameStore)
const assetNameOptions = computed<Option[]>(() => {
  if (!assetName.value || !Array.isArray(assetName.value)) return []
  return assetName.value
      .filter(item => item && item.id && item.name)
      .map(item => ({
        label: item.name,
        value: item.id,
      }))
})

// 初始化函数
async function initializeData() {
  isLoading.value = true
  loadError.value = ''
  try {
    await metaStore.initAll()
    await assetNameStore.fetchAssetName()
  } catch (e: any) {
    loadError.value = e?.message || '数据加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  initializeData()
})
</script>
