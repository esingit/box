<template>
  <div class="flex space-x-2 mt-2 justify-center text-gray-900 select-none">
    <select v-model.number="hours" class="select-clean">
      <option
          v-for="h in 24"
          :key="h"
          :value="h - 1"
          :disabled="isDisabled(h - 1, minutes, seconds)"
      >
        {{ (h - 1).toString().padStart(2, '0') }}
      </option>
    </select>
    <span class="select-none font-semibold text-gray-600 flex items-center">:</span>
    <select v-model.number="minutes" class="select-clean">
      <option
          v-for="m in 60"
          :key="m"
          :value="m - 1"
          :disabled="isDisabled(hours, m - 1, seconds)"
      >
        {{ (m - 1).toString().padStart(2, '0') }}
      </option>
    </select>
    <span class="select-none font-semibold text-gray-600 flex items-center">:</span>
    <select v-model.number="seconds" class="select-clean">
      <option
          v-for="s in 60"
          :key="s"
          :value="s - 1"
          :disabled="isDisabled(hours, minutes, s - 1)"
      >
        {{ (s - 1).toString().padStart(2, '0') }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import { watch, ref } from 'vue'

const props = defineProps<{
  modelValue: { h: number; m: number; s: number }
  minTime?: { h: number; m: number; s: number }
  maxTime?: { h: number; m: number; s: number }
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: { h: number; m: number; s: number }): void
}>()

const hours = ref(props.modelValue.h)
const minutes = ref(props.modelValue.m)
const seconds = ref(props.modelValue.s)

watch([hours, minutes, seconds], () => {
  emit('update:modelValue', { h: hours.value, m: minutes.value, s: seconds.value })
})

watch(() => props.modelValue, (val) => {
  hours.value = val.h
  minutes.value = val.m
  seconds.value = val.s
})

function isDisabled(h: number, m: number, s: number): boolean {
  const current = h * 3600 + m * 60 + s
  const min = props.minTime ? props.minTime.h * 3600 + props.minTime.m * 60 + props.minTime.s : null
  const max = props.maxTime ? props.maxTime.h * 3600 + props.maxTime.m * 60 + props.maxTime.s : null
  if (min !== null && current < min) return true
  return max !== null && current > max;

}
</script>

<style scoped>
.select-clean {
  @apply bg-white border border-gray-300 rounded-full px-3 py-1 text-sm text-black shadow-sm transition duration-150 focus:outline-none focus:ring-0 focus:border-gray-500 hover:border-gray-400 cursor-pointer appearance-none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 20 20' fill='gray' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath fill-rule='evenodd' d='M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0L5.21 8.27a.75.75 0 01.02-1.06z' clip-rule='evenodd'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.5rem center;
  background-size: 1rem 1rem;
}
</style>