<template>
  <div class="relative w-full bg-white border rounded-xl p-4 space-y-4 transition">
    <!-- 搜索行 -->
    <div class="flex flex-wrap items-center gap-3">
      <div class="flex-1 min-w-[200px]">
        <BaseSelect
            title="健身类型"
            v-model="query.typeIdList"
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
            class="w-[300px]"
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
      <BaseInput v-model="query.remark" placeholder="备注关键词" type="text" clearable />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
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

const rangeValue = ref('')

// 监听 props.query.startDate、endDate，自动拼接成 rangeValue 字符串
watch(
    () => [props.query.startDate, props.query.endDate],
    ([start, end]) => {
      rangeValue.value = joinRangeDates(start, end)
    },
    { immediate: true }
)

// 监听 rangeValue，拆分后赋值给 props.query.startDate 和 endDate
watch(rangeValue, (val) => {
  const { start, end } = splitRangeDates(val)
  props.query.startDate = start
  props.query.endDate = end
})

function onSearch() {
  emit('search', { ...props.query })
}

function onReset() {
  emit('reset')
}
</script>
