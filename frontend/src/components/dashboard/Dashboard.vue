<template>
  <div class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-8 rounded-xl">
    <div class="grid gap-12">
      <!-- 加上 :key 强制刷新组件 -->
      <FitnessStats :key="route.fullPath" :fitnessTypeOptions="fitnessTypeOptions" />
      <AssetStats
          :key="route.fullPath"
          :assetNameOptions="assetNameOptions"
          :assetTypeOptions="assetTypeOptions"
          :assetLocationOptions="assetLocationOptions"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
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

// 健身类型下拉框选项
const fitnessTypeOptions = computed(() =>
    (metaStore.typeMap?.FITNESS_TYPE || []).map(item => ({
      label: item.value1 || '',
      value: item.id,
      value1: item.value1
    }))
)

// 资产相关下拉选项
const assetTypeOptions = computed(() =>
    (metaStore.typeMap?.ASSET_TYPE || []).map(i => ({
      label: i.value1 || '',
      value: i.id
    }))
)
const assetLocationOptions = computed(() =>
    (metaStore.typeMap?.ASSET_LOCATION || []).map(i => ({
      label: i.value1 || '',
      value: i.id
    }))
)
const { assetNameOptions } = storeToRefs(assetNameStore)

onMounted(async () => {
  await Promise.all([metaStore.initAll()])
  await assetNameStore.fetchAssetName()
})
</script>
