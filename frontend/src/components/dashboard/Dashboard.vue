<template>
  <div class="dashboard-wrapper px-4 py-6">
    <div class="grid gap-12">
      <FitnessStats :fitnessTypeOptions="fitnessTypeOptions"/>
      <AssetStats
          :assetNameOptions="assetNameOptions"
          :assetTypeOptions="assetTypeOptions"
          :assetLocationOptions="assetLocationOptions"
      />
    </div>
  </div>
</template>

<script setup="ts">
import {useMetaStore} from '@/store/metaStore'
import {useAssetNameStore} from "@/store/assetNameStore";

const metaStore = useMetaStore()
const assetNameStore = useAssetNameStore()

// 类型选项
const fitnessTypeOptions = computed(() => (metaStore.typeMap?.FITNESS_TYPE || []).map(item => ({
  label: item.value1 || '',
  value: item.id
})))

const assetTypeOptions = computed(() =>
    (metaStore.typeMap?.ASSET_TYPE || []).map(i => ({label: i.value1 || '', value: i.id}))
)
const assetLocationOptions = computed(() =>
    (metaStore.typeMap?.ASSET_LOCATION || []).map(i => ({label: i.value1 || '', value: i.id}))
)
const {assetNameOptions} = storeToRefs(assetNameStore)

onMounted(async () => {
  await Promise.all([
    metaStore.initAll(),
  ])
})
</script>
