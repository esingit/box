<template>
  <div class="flex items-center space-x-3 text-gray-700 select-none justify-end">
    <!-- 上一页 -->
    <BaseButton
        type="button"
        title="上一页"
        color="outline"
        :disabled="pageNo <= 1"
        @click="changePage(pageNo - 1)"
    />

    <!-- 页码按钮与省略号 -->
    <template v-for="(page, idx) in pagesToShow" :key="idx">
      <BaseButton
          v-if="page !== '...'"
          :color="page === pageNo ? 'primary' : 'outline'"
          @click="changePage(page as number)"
      >
        {{ page }}
      </BaseButton>
      <span v-else class="px-2 select-none">...</span>
    </template>

    <!-- 下一页 -->
    <BaseButton
        type="button"
        title="下一页"
        color="outline"
        :disabled="pageNo >= totalPages || totalPages === 0"
        @click="changePage(pageNo + 1)"
    >
      下一页
    </BaseButton>

    <!-- 每页条数选择 -->
    <div class="w-[170px]">
      <Field name="pageSize" v-slot="{ value, setValue }">
        <BaseSelect
            title="每页条数"
            :modelValue="props.pageSize"
            direction="up"
            :options="pageSizeOptions.map(n => ({ label: `每页${n}条`, value: n }))"
            placeholder="每页条数"
            searchable
            @update:modelValue="val => emit('page-size-change', val as number)"
        />
      </Field>
    </div>

    <!-- 总条数 -->
    <span class="text-gray-500 text-sm whitespace-nowrap">
      共 {{ total }} 条
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Field } from 'vee-validate'
import BaseSelect from './BaseSelect.vue'

const props = defineProps<{
  pageNo: number
  total: number
  pageSize: number
}>()

const emit = defineEmits<{
  (e: 'page-change', page: number): void
  (e: 'page-size-change', size: number): void
}>()

const pageSizeOptions = [10, 20, 50, 100, 300, 900]

const totalPages = computed(() =>
    props.total && props.pageSize ? Math.ceil(props.total / props.pageSize) : 0
)

const pagesToShow = computed((): (number | '...')[] => {
  const total = totalPages.value
  const pageNo = props.pageNo
  if (total === 0) return []

  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const sideCount = 3
  const range = 1
  const result: (number | '...')[] = []

  const firstPages = [1, 2, 3]
  const lastPages = [total - 2, total - 1, total].filter(n => n > sideCount)

  const middle: number[] = []
  const frontThresh = sideCount + range
  const tailThresh = total - sideCount - range + 1

  if (pageNo <= frontThresh) {
    for (let i = sideCount + 1; i <= sideCount + range * 2 + 1; i++) {
      middle.push(i)
    }
  } else if (pageNo >= tailThresh) {
    for (let i = total - sideCount - range * 2; i <= total - sideCount; i++) {
      middle.push(i)
    }
  } else {
    for (let i = pageNo - range; i <= pageNo + range; i++) {
      if (i > sideCount && i < total - sideCount + 1) {
        middle.push(i)
      }
    }
  }

  const merged = [...firstPages, ...middle, ...lastPages]
      .filter(p => p >= 1 && p <= total)
  const sorted = Array.from(new Set(merged)).sort((a, b) => a - b)

  for (let i = 0; i < sorted.length; i++) {
    result.push(sorted[i])
    const nxt = sorted[i + 1]
    if (nxt && nxt - sorted[i] > 1) {
      result.push('...')
    }
  }

  return result
})

function changePage(page: number) {
  if (page < 1 || page > totalPages.value || page === props.pageNo) return
  emit('page-change', page)
}
</script>
