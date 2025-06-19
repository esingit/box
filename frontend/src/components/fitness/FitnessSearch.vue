<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px]">
        <BaseSelect
            v-model="query.typeIdList"
            :options="fitnessTypeOptions"
            multiple
            clearable
            placeholder="全部类型"
            class="w-full"
        />
      </div>

      <!-- 日期范围 -->
      <div class="flex gap-2 items-center flex-shrink-0">
        <input type="date" v-model="query.startDate" class="input-base w-[140px]" />
        <span class="text-gray-400">至</span>
        <input type="date" v-model="query.endDate" class="input-base w-[140px]" />
      </div>

      <!-- 按钮组 -->
      <div class="flex gap-2 flex-shrink-0 ml-auto">
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

    <!-- 第二行 -->
    <div v-if="showMore" class="flex flex-wrap gap-3">
      <input
          v-model="query.remark"
          placeholder="备注关键词"
          type="text"
          class="input-base w-full sm:w-[300px]"
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
    typeIdList: (string | number)[]
    startDate: string
    endDate: string
    remark: string
  }
  fitnessTypeOptions: Array<{ label: string; value: string | number }>
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
