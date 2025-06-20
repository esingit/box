<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px]">
        <BaseSelect
            v-model="query.assetNameIdList"
            :options="localAssetNameOptions"
            placeholder="全部资产名称"
            multiple
            clearable
            class="w-full"
        />
      </div>

      <!-- 资产类型 -->
      <BaseSelect
          v-model="query.assetTypeIdList"
          :options="assetTypeOptions"
          placeholder="全部资产类型"
          multiple
          clearable
          class="w-full max-w-[240px]"
      />

      <!-- 资产位置 -->
      <BaseSelect
          v-model="query.assetLocationIdList"
          :options="assetLocationOptions"
          placeholder="全部资产位置"
          multiple
          clearable
          class="w-full max-w-[240px]"
      />

      <!-- 按钮组 -->
      <div class="flex items-center gap-2">
        <BaseButton type="button" @click="onSearch" color="outline" :icon="LucideSearch" variant="search"/>
        <BaseButton type="button" @click="onReset" color="outline" :icon="LucideRotateCcw" variant="search"/>
        <BaseButton type="button" @click="toggleMore" color="outline" :icon="showMore ? LucideChevronUp : LucideChevronDown" variant="search"/>
      </div>
    </div>

    <!-- 更多条件 -->
    <div v-if="showMore" class="flex flex-col md:flex-row md:items-center md:gap-3 gap-2">
      <!-- 日期范围 -->
      <div class="flex items-center gap-2">
        <input type="date" v-model="query.startDate" class="input-base" />
        <span class="text-gray-400">至</span>
        <input type="date" v-model="query.endDate" class="input-base" />
      </div>

      <!-- 备注关键词 -->
     <BaseInput
          type="text"
          v-model="query.remark"
          placeholder="备注关键词"
          clearable
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch,
} from 'lucide-vue-next'

const props = defineProps<{
  query: {
    assetNameIdList: (string | number)[]
    assetTypeIdList: (string | number)[]
    assetLocationIdList: (string | number)[]
    startDate: string
    endDate: string
    remark: string
  }
  assetNameOptions: Array<{ label: string; value: string | number }>
  assetTypeOptions: Array<{ label: string; value: string | number }>
  assetLocationOptions: Array<{ label: string; value: string | number }>
  resultCount: number | null
}>()

const emit = defineEmits(['search', 'reset'])

// 本地响应式变量，解决首次为空的问题
const localAssetNameOptions = ref<Array<{ label: string; value: string | number }>>([])

// 监听 props.assetNameOptions 变化
watch(
    () => props.assetNameOptions,
    (val) => {
      localAssetNameOptions.value = val || []
    },
    { immediate: true }
)

const showMore = ref(false)
const toggleMore = () => {
  showMore.value = !showMore.value
}

function onSearch() {
  emit('search', { ...props.query })
}

function onReset() {
  emit('reset')
}
</script>

