<template>
  <div class="relative w-full bg-white border rounded-xl p-4 transition">
    <!-- 主行 -->
    <div class="flex items-center justify-between gap-3 min-w-full flex-wrap">
      <!-- 资产名称 多选 -->
      <BaseSelect
          v-model="localQuery.assetNameIdList"
          :options="assetNameOptions"
          placeholder="全部资产名称"
          class="w-full max-w-[320px]"
          multiple
          clearable
      />

      <!-- 资产类型 多选 -->
      <BaseSelect
          v-model="localQuery.typeIdList"
          :options="typeOptions"
          placeholder="全部资产类型"
          class="w-full max-w-[240px]"
          multiple
          clearable
      />

      <!-- 资产位置 多选 -->
      <BaseSelect
          v-model="localQuery.locationIdList"
          :options="locationOptions"
          placeholder="全部资产位置"
          class="w-full max-w-[240px]"
          multiple
          clearable
      />

      <!-- 按钮组 -->
      <div class="flex items-center gap-2 shrink-0 mt-2 sm:mt-0">
        <button @click="onSearch" title="查询" class="btn-outline p-2">
          <LucideSearch class="w-4 h-4" />
        </button>
        <button @click="onReset" title="重置" class="btn-outline p-2">
          <LucideRotateCcw class="w-4 h-4" />
        </button>
        <button @click="showMore = !showMore" title="更多" class="btn-outline p-2">
          <component :is="showMore ? LucideChevronUp : LucideChevronDown" class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- 更多查询 -->
    <div v-if="showMore" class="mt-4 flex flex-wrap items-center gap-4">
      <!-- 日期范围 -->
      <div class="flex items-center gap-2 min-w-[220px] flex-shrink-0">
        <input
            type="date"
            v-model="localQuery.startDate"
            class="input-base"
            placeholder="开始日期"
        />
        <span class="text-gray-400 select-none">至</span>
        <input
            type="date"
            v-model="localQuery.endDate"
            class="input-base"
            placeholder="结束日期"
        />
      </div>

      <!-- 备注关键词 -->
      <input
          type="text"
          v-model="localQuery.remark"
          placeholder="备注关键词"
          class="base-input flex-grow min-w-[240px]"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import {
  LucideChevronDown,
  LucideChevronUp,
  LucideRotateCcw,
  LucideSearch
} from 'lucide-vue-next'

const props = defineProps({
  query: Object,
  assetNames: Array,
  types: Array,
  locations: Array
})

const emit = defineEmits(['updateQuery', 'search', 'reset'])

const showMore = ref(false)

const localQuery = reactive({
  assetNameIdList: [] as (string | number)[],
  typeIdList: [] as (string | number)[],
  locationIdList: [] as (string | number)[],
  startDate: '',
  endDate: '',
  remark: ''
})

// 同步外部传入 query 到 localQuery
function updateLocalQuery(source: any) {
  Object.assign(localQuery, {
    assetNameIdList: source.assetNameIdList || [],
    typeIdList: source.typeIdList || [],
    locationIdList: source.locationIdList || [],
    startDate: source.startDate || '',
    endDate: source.endDate || '',
    remark: source.remark || ''
  })
}
updateLocalQuery(props.query || {})

watch(
    () => props.query,
    (newQuery) => {
      updateLocalQuery(newQuery || {})
    },
    { deep: true }
)

// 向外同步本地查询条件
watch(
    localQuery,
    (newVal) => {
      emit('updateQuery', { ...newVal })
    },
    { deep: true }
)

// 选项格式化，保证label/value统一
const assetNameOptions = computed(() =>
    (props.assetNames || []).map(item => ({
      label: item.name || '',
      value: item.id
    }))
)

const typeOptions = computed(() =>
    (props.types || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

const locationOptions = computed(() =>
    (props.locations || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

function onSearch() {
  emit('search')
}

function onReset() {
  emit('reset')
}
</script>
