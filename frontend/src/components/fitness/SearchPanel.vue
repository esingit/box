<!--src/components/fitness/SearchPanel.vue-->
<template>
  <div
      class="relative w-full bg-white border rounded-xl p-4 transition"
  >
    <!-- 第一行 -->
    <div class="flex items-center justify-between gap-3 flex-nowrap min-w-full">
      <!-- 多选类型 -->
      <MultiSelectTeleport
          v-model="query.typeIdList"
          :options="fitnessTypeOptions"
          placeholder="全部类型"
          class="flex-grow max-w-full"
      />

      <!-- 日期 -->
      <div class="flex items-center gap-2 min-w-[220px] flex-shrink-0">
        <input
            type="date"
            v-model="query.startDate"
            class="input-base"
        />
        <span class="text-gray-400 select-none">至</span>
        <input
            type="date"
            v-model="query.endDate"
            class="input-base"
        />
      </div>

      <!-- 按钮组 -->
      <div class="flex items-center gap-2 shrink-0">
        <button
            @click="onSearch"
            title="查询"
            class="btn-outline"
        >
          <LucideSearch class="w-4 h-4" />
        </button>
        <button
            @click="onReset"
            title="重置"
            class="btn-outline"
        >
          <LucideRotateCcw class="w-4 h-4" />
        </button>
        <button
            @click="showMore = !showMore"
            title="更多"
            class="btn-outline"
        >
          <component :is="showMore ? LucideChevronUp : LucideChevronDown" class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- 第二行 -->
    <div v-if="showMore" class="mt-4 flex min-w-full">
      <input
          type="text"
          v-model="query.remark"
          placeholder="备注关键词"
          class="base-input"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import MultiSelectTeleport from '@/components/base/BaseMultiSelect.vue'
import {
  LucideSearch,
  LucideRotateCcw,
  LucideChevronDown,
  LucideChevronUp
} from 'lucide-vue-next'

import { useFitnessStore } from '@/store/fitnessStore'
import { useMetaStore } from '@/store/metaStore'

const showMore = ref(false)
const fitnessStore = useFitnessStore()
const metaStore = useMetaStore()

const { query } = storeToRefs(fitnessStore)

const fitnessTypeOptions = computed(() =>
    (metaStore.typeMap?.FITNESS_TYPE || []).map(item => ({
      label: item.value1,
      value: item.id
    }))
)

const onSearch = () => {
  fitnessStore.loadList()
}
const onReset = () => {
  fitnessStore.resetQuery()
}
</script>
