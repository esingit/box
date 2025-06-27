<!--src/components/asset/AssetSearch.vue-->
<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 - 使用 grid 布局 -->
    <div class="flex items-center gap-3 w-full">
      <!-- 资产类型 - 固定 200px -->
      <div class="w-[200px] flex-shrink-0">
        <BaseSelect
            title="资产类型"
            v-model="query.assetTypeIdList"
            :options="assetTypeOptions"
            placeholder="全部资产类型"
            multiple
            clearable
            searchable
            class="w-full"
        />
      </div>

      <!-- 资产位置 -->
      <BaseSelect
          title="资产位置"
          v-model="query.assetLocationIdList"
          :options="assetLocationOptions"
          placeholder="全部资产位置"
          multiple
          clearable
          searchable
          class="w-full"
      />

      <div class="w-[280px] flex-shrink-0">
        <BaseDateInput
            v-model="rangeValue"
            type="date"
            range
            clearable
            class="w-full"
            placeholder="请选择日期范围"
        />
      </div>

      <!-- 按钮组 -->
      <div class="flex items-center gap-2 flex-shrink-0">
        <BaseButton type="button" @click="onSearch" color="outline" :icon="LucideSearch" variant="search"/>
        <BaseButton type="button" @click="onReset" color="outline" :icon="LucideRotateCcw" variant="search"/>
        <BaseButton type="button" @click="toggleMore" color="outline"
                    :icon="showMore ? LucideChevronUp : LucideChevronDown" variant="search"/>
      </div>
    </div>

    <!-- 更多条件 -->
    <div v-if="showMore" class="flex flex-col md:flex-row md:items-center md:gap-3 gap-2">
      <div class="w-[700px] flex-shrink-0">
        <BaseSelect
            title="资产名称"
            v-model="query.assetNameIdList"
            :options="localAssetNameOptions"
            placeholder="全部资产名称"
            multiple
            clearable
            searchable
            class="w-full"
        />
      </div>
      <!-- 备注关键词 -->
      <BaseInput
          type="text"
          v-model="query.remark"
          placeholder="备注关键词"
          clearable
          class="w-full"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, watch} from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
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

watch(
    () => props.assetNameOptions,
    (val) => {
      localAssetNameOptions.value = val || []
    },
    {immediate: true}
)

const showMore = ref(true)
const toggleMore = () => {
  showMore.value = !showMore.value
}

// 日期范围字符串，格式形如 '2023-01-01 ~ 2023-01-31'
const rangeValue = ref('')

// 工具函数：把start和end拼成range字符串
function joinRangeDates(start: string, end: string) {
  if (!start && !end) return ''
  if (start && end) return `${start} ~ ${end}`
  return start || end || ''
}

// 工具函数：拆分range字符串为start和end
function splitRangeDates(rangeStr: string) {
  if (!rangeStr) return {start: '', end: ''}
  const parts = rangeStr.split('~').map(s => s.trim())
  return {
    start: parts[0] || '',
    end: parts[1] || ''
  }
}

// 监听 props.query.startDate 和 endDate，同步给 rangeValue 显示
watch(
    () => [props.query.startDate, props.query.endDate],
    ([start, end]) => {
      rangeValue.value = joinRangeDates(start, end)
    },
    {immediate: true}
)

// 监听 rangeValue，拆分回 startDate 和 endDate，赋值给 props.query
watch(rangeValue, (val) => {
  const {start, end} = splitRangeDates(val)
  props.query.startDate = start
  props.query.endDate = end
})

function onSearch() {
  emit('search', {...props.query})
}

function onReset() {
  emit('reset')
}
</script>
