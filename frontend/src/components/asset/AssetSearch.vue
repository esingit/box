<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px]">
      <BaseSelect
          v-model="query.assetNameIdList"
          :options="assetNameOptions"
          placeholder="全部资产名称"
          multiple
          clearable
          class="w-full"
      />
      </div>

      <!-- 资产类型 -->
      <BaseSelect
          v-model="query.typeIdList"
          :options="assetTypeOptions"
          placeholder="全部资产类型"
          multiple
          clearable
          class="w-full max-w-[240px]"
      />

      <!-- 资产位置 -->
      <BaseSelect
          v-model="query.locationIdList"
          :options="assetLocationOptions"
          placeholder="全部资产位置"
          multiple
          clearable
          class="w-full max-w-[240px]"
      />

      <!-- 按钮组 -->
      <div class="flex items-center gap-2">
        <button @click="onSearch" title="查询" class="btn-outline">
          <LucideSearch class="w-4 h-4" />
        </button>
        <button @click="onReset" title="重置" class="btn-outline">
          <LucideRotateCcw class="w-4 h-4" />
        </button>
        <button @click="toggleMore" title="更多" class="btn-outline">
          <component :is="showMore ? LucideChevronUp : LucideChevronDown" class="w-4 h-4" />
        </button>
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
      <input
          type="text"
          v-model="query.remark"
          placeholder="备注关键词"
          class="input-base w-full md:w-[300px]"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch,
} from 'lucide-vue-next'

const props = defineProps<{
  query: {
    assetNameIdList: (string | number)[]
    typeIdList: (string | number)[]
    locationIdList: (string | number)[]
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
