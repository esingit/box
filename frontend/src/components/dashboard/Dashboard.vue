

<template>
  <div class="min-h-screen max-w-6xl mx-auto bg-gray-50 rounded-xl flex flex-col space-y-8">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="py-12 flex justify-center items-center text-gray-500 gap-3">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-gray-900"></div>
      正在加载统计数据...
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="py-12 text-center">
      <AlertCircle class="mx-auto w-16 h-16 mb-4" />
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
          :unit-options="unitOptions"
          :fitness-type-options="fitnessTypeOptions"
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
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { AlertCircle } from 'lucide-vue-next'
import { useMetaStore, type CommonMetaVO } from '@/store/metaStore'
import { useAssetNameStore } from '@/store/assetNameStore'

import FitnessStats from './components/FitnessStats.vue'
import AssetStats from './components/AssetStats.vue'

const route = useRoute()
const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

const isLoading = ref(true)
const loadError = ref('')

// 通用映射函数，返回 CommonMetaVO[]
function mapToCommonMetaVO(list: any[] | undefined, typeCode = '', typeName = ''): CommonMetaVO[] {
  if (!list || !Array.isArray(list)) return []
  return list
      .filter(item => item && item.id && (item.value1 || item.name))
      .map(item => ({
        id: item.id,
        typeCode: item.typeCode || typeCode,
        typeName: item.typeName || typeName,
        value: item.id,
        label: item.value1 || item.name || '',
        key1: item.key1,
        key2: item.key2,
        key3: item.key3,
        key4: item.key4,
        value1: item.value1,
        value2: item.value2,
        value3: item.value3,
        value4: item.value4,
      }))
}

// 各类选项（typeMap 是 Record<string, CommonMetaVO[]>）
const unitOptions = computed<CommonMetaVO[]>(() => metaStore.getOptions('UNIT'))
const fitnessTypeOptions = computed<CommonMetaVO[]>(() => metaStore.getOptions('FITNESS_TYPE'))
const assetTypeOptions = computed<CommonMetaVO[]>(() => metaStore.getOptions('ASSET_TYPE'))
const assetLocationOptions = computed<CommonMetaVO[]>(() => metaStore.getOptions('ASSET_LOCATION'))

// 资产名称选项（来自 assetNameStore 里 assetName）
const { assetName } = storeToRefs(assetNameStore)
const assetNameOptions = computed<CommonMetaVO[]>(() =>
    mapToCommonMetaVO(assetName.value, 'ASSET_NAME', '资产名称')
)

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