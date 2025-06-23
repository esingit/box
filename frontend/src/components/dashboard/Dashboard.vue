<template>
  <div class="min-h-screen bg-gray-50 max-w-6xl mx-auto flex flex-col space-y-8 rounded-xl">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="flex items-center justify-center py-12">
      <div class="flex items-center gap-3 text-gray-500">
        <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-gray-900"></div>
        <span>正在加载统计数据...</span>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="loadError" class="flex items-center justify-center py-12">
      <div class="text-center">
        <div class="text-red-500 mb-4">
          <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-800 mb-2">数据加载失败</h3>
        <p class="text-gray-600 mb-4">{{ loadError }}</p>
        <button
            @click="initializeData"
            class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
        >
          重新加载
        </button>
      </div>
    </div>

    <!-- 主要内容 -->
    <div v-else class="grid gap-6">
      <!-- 健身统计组件 -->
      <FitnessStats
          :key="`fitness-${route.fullPath}`"
          :unitOptions="unitOptions"
          :fitnessTypeOptions="fitnessTypeOptions"
      />

      <!-- 资产统计组件 -->
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
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useMetaStore } from '@/store/metaStore'
import { useAssetNameStore } from '@/store/assetNameStore'

// 引入子组件
import FitnessStats from './components/FitnessStats.vue'
import AssetStats from './components/AssetStats.vue'

const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()
const route = useRoute()

// 状态管理
const isLoading = ref(true)
const loadError = ref('')

// 单位下拉框选项 - 包含完整的字段信息
const unitOptions = computed(() => {
  const units = metaStore.typeMap?.UNIT
  if (!units || !Array.isArray(units)) {
    return []
  }

  return units.map(item => ({
    id: item.id,
    label: item.value1 || '',
    value: item.id,
    key1: item.key1,
    value1: item.value1,
    key2: item.key2,
    value2: item.value2,
    key3: item.key3,
    value3: item.value3,
    key4: item.key4,
    value4: item.value4
  })).filter(item => item.label && item.value)
})

// 健身类型下拉框选项 - 包含完整的字段信息
const fitnessTypeOptions = computed(() => {
  const types = metaStore.typeMap?.FITNESS_TYPE
  if (!types || !Array.isArray(types)) {
    return []
  }

  return types.map(item => ({
    id: item.id,
    label: item.value1 || '',
    value: item.id,
    key1: item.key1,
    value1: item.value1,
    key2: item.key2,
    value2: item.value2,
    key3: item.key3,
    value3: item.value3,
    key4: item.key4,
    value4: item.value4
  })).filter(item => item.label && item.value)
})

// 资产类型下拉框选项 - 包含完整的字段信息
const assetTypeOptions = computed(() => {
  const types = metaStore.typeMap?.ASSET_TYPE
  if (!types || !Array.isArray(types)) {
    return []
  }

  return types.map(item => ({
    id: item.id,
    label: item.value1 || '',
    value: item.id,
    key1: item.key1,
    value1: item.value1,
    key2: item.key2,
    value2: item.value2,
    key3: item.key3,
    value3: item.value3,
    key4: item.key4,
    value4: item.value4
  })).filter(item => item.label && item.value)
})

// 资产位置下拉框选项
const assetLocationOptions = computed(() => {
  const locations = metaStore.typeMap?.ASSET_LOCATION
  if (!locations || !Array.isArray(locations)) {
    return []
  }

  return locations.map(item => ({
    id: item.id,
    label: item.value1 || '',
    value: item.id,
    key1: item.key1,
    value1: item.value1
  })).filter(item => item.label && item.value)
})

// 从 store 中获取资产名称选项
const { assetNameOptions } = storeToRefs(assetNameStore)

// 安全的资产名称选项
const safeAssetNameOptions = computed(() => {
  if (!assetNameOptions.value || !Array.isArray(assetNameOptions.value)) {
    return []
  }
  return assetNameOptions.value.filter(item => item && item.id && item.name)
})

// 初始化数据
async function initializeData() {
  isLoading.value = true
  loadError.value = ''

  try {
    console.log('Starting data initialization...')

    // 并行加载基础元数据
    await Promise.all([
      metaStore.initAll(),
    ])

    console.log('Meta data loaded, loading asset names...')

    // 加载资产名称数据
    await assetNameStore.fetchAssetName()

    console.log('All data loaded successfully')
    console.log('Unit options:', unitOptions.value.length)
    console.log('Fitness type options:', fitnessTypeOptions.value.length)
    console.log('Asset type options:', assetTypeOptions.value.length)
    console.log('Asset location options:', assetLocationOptions.value.length)
    console.log('Asset name options:', safeAssetNameOptions.value.length)

  } catch (error) {
    console.error('Failed to initialize data:', error)
    loadError.value = error instanceof Error ? error.message : '数据加载失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时初始化
onMounted(async () => {
  console.log('=== Dashboard mounted ===')
  await initializeData()
})
</script>

<style scoped>
/* 加载动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.grid {
  animation: fadeIn 0.6s ease-out;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .max-w-6xl {
    max-width: 100%;
    padding: 1rem;
  }
}
</style>