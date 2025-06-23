<!--src/components/fitness/FitnessSearch.vue-->
<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px]">
        <BaseSelect
            title="健身类型"
            v-model="localQuery.typeIdList"
            :options="fitnessTypeOptions"
            multiple
            clearable
            placeholder="全部健身类型"
            class="w-full"
        />
      </div>

      <!-- 日期范围，使用单个range组件 -->
      <div class="flex gap-2 items-center flex-shrink-0">
        <BaseDateInput
            v-model="rangeValue"
            type="date"
            range
            clearable
            placeholder="请选择日期范围"
            class="w-[325px]"
        />
      </div>

      <!-- 按钮组 -->
      <div class="flex gap-2 flex-shrink-0 ml-auto">
        <BaseButton @click="onSearch" color="outline" :icon="LucideSearch" variant="search" />
        <BaseButton @click="onReset" color="outline" :icon="LucideRotateCcw" variant="search" />
        <BaseButton
            @click="toggleMore"
            color="outline"
            :icon="showMore ? LucideChevronUp : LucideChevronDown"
            variant="search"
        />
      </div>
    </div>

    <!-- 第二行 -->
    <div v-if="showMore" class="flex flex-wrap gap-3">
      <BaseInput v-model="localQuery.remark" placeholder="备注关键词" type="text" clearable />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import { LucideChevronDown, LucideChevronUp, LucideRotateCcw, LucideSearch } from 'lucide-vue-next'
import { joinRangeDates, splitRangeDates } from '@/utils/formatters'

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

// 用响应式localQuery复制props.query，避免直接修改props
const localQuery = reactive({
  typeIdList: Array.isArray(props.query.typeIdList) ? [...props.query.typeIdList] : [],
  startDate: props.query.startDate,
  endDate: props.query.endDate,
  remark: props.query.remark
})

// rangeValue 用于绑定日期范围选择组件
const rangeValue = ref(joinRangeDates(localQuery.startDate, localQuery.endDate))

// 监听 localQuery.startDate/endDate，更新 rangeValue
watch(
    () => [localQuery.startDate, localQuery.endDate],
    ([start, end]) => {
      rangeValue.value = joinRangeDates(start, end)
    }
)

// 监听 rangeValue，拆分赋值给 localQuery.startDate/endDate
watch(rangeValue, (val) => {
  const { start, end } = splitRangeDates(val)
  localQuery.startDate = start
  localQuery.endDate = end
})

// 监听 props.query 变化，保持 localQuery 同步，深拷贝typeIdList避免引用问题
watch(
    () => props.query,
    (newVal) => {
      localQuery.typeIdList = Array.isArray(newVal.typeIdList) ? [...newVal.typeIdList] : []
      localQuery.startDate = newVal.startDate
      localQuery.endDate = newVal.endDate
      localQuery.remark = newVal.remark
    },
    { deep: true }
)

function onSearch() {
  emit('search', { ...localQuery })
}

function onReset() {
  emit('reset')
}
</script>
