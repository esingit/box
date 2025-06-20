<template>
  <div class="flex items-center space-x-3 text-gray-700 select-none justify-end">
    <!-- 上一页 -->
    <button
        class="btn-outline"
        :disabled="current <= 1"
        @click="changePage(current - 1)"
    >
      上一页
    </button>

    <!-- 页码按钮与省略号 -->
    <template v-for="(page, idx) in pagesToShow" :key="idx">
      <button
          v-if="page !== '...'"
          class="px-4 py-4 rounded-full border cursor-pointer transition"
          :class="page === current ? 'btn-primary' : 'btn-outline'"
          @click="changePage(page as number)"
      >
        {{ page }}
      </button>
      <span v-else class="px-2 select-none">...</span>
    </template>

    <!-- 下一页 -->
    <button
        class="btn-outline"
        :disabled="current >= totalPages || totalPages === 0"
        @click="changePage(current + 1)"
    >
      下一页
    </button>

    <!-- 每页条数选择 -->
    <div class="w-[130px]">
      <Field name="pageSize" v-slot="{ value, setValue }">
        <BaseSelect
            :modelValue="value"
            direction="up"
            :options="pageSizeOptions.map(n => ({ label: `每页${n}条`, value: n }))"
            placeholder="每页条数"
            clearable
            @update:modelValue="val => {
              setValue(val)
              emit('page-size-change', val)
            }"
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
  current: number
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
  const current = props.current
  if (total === 0) return []

  // 如果总页数小于等于7，显示全部页码，避免省略号
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const sideCount = 3    // 前后固定页数
  const range = 1        // 当前页前后范围
  const result: (number | '...')[] = []

  const firstPages = [1, 2, 3]
  const lastPages = [total - 2, total - 1, total].filter(n => n > sideCount)

  const middle: number[] = []
  const frontThresh = sideCount + range       // 4
  const tailThresh  = total - sideCount - range + 1  // e.g. 8

  if (current <= frontThresh) {
    // 靠前：显示 4,5,6
    for (let i = sideCount + 1; i <= sideCount + range * 2 + 1; i++) {
      middle.push(i)
    }
  } else if (current >= tailThresh) {
    // 靠后：显示 total-5,total-4,total-3
    for (let i = total - sideCount - range * 2; i <= total - sideCount; i++) {
      middle.push(i)
    }
  } else {
    // 中间：显示 current-1, current, current+1
    for (let i = current - range; i <= current + range; i++) {
      if (i > sideCount && i < total - sideCount + 1) {
        middle.push(i)
      }
    }
  }

  // 合并去重并排序
  const merged = [...firstPages, ...middle, ...lastPages]
      .filter(p => p >= 1 && p <= total)
  const sorted = Array.from(new Set(merged)).sort((a, b) => a - b)

  // 插入省略号
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
  if (page < 1 || page > totalPages.value || page === props.current) return
  emit('page-change', page)
}
</script>
