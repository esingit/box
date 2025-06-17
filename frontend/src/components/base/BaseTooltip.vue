<template>
  <div class="relative group inline-block">
    <slot />
    <div
        v-if="text"
        class="absolute z-50 hidden group-hover:block min-w-max max-w-xs text-sm text-white bg-gray-800 rounded-md px-2 py-1 shadow-md transition-opacity duration-200"
        :class="tooltipPositionClass"
    >
      {{ text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text: string
  position?: 'top' | 'bottom' | 'left' | 'right'
}>()

const tooltipPositionClass = computed(() => {
  switch (props.position) {
    case 'bottom':
      return 'top-full mt-1 left-1/2 -translate-x-1/2'
    case 'left':
      return 'right-full mr-2 top-1/2 -translate-y-1/2'
    case 'right':
      return 'left-full ml-2 top-1/2 -translate-y-1/2'
    default:
      return 'bottom-full mb-1 left-1/2 -translate-x-1/2'
  }
})
</script>
