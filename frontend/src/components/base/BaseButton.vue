<script setup lang="ts">
defineProps<{
  type?: 'button' | 'submit' | 'reset'
  color?: 'primary' | 'gray' | 'danger'
  icon?: any
  loading?: boolean
  disabled?: boolean
}>()
</script>

<template>
  <button
      :type="type ?? 'button'"
      :disabled="disabled || loading"
      class="inline-flex items-center gap-2 rounded-2xl px-4 py-2 text-sm font-medium transition
           shadow-sm disabled:opacity-50 disabled:cursor-not-allowed
           focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2"
      :class="{
      'bg-primary text-white hover:bg-primary-dark focus-visible:ring-primary': color === 'primary',
      'bg-gray-200 text-gray-900 hover:bg-gray-300 focus-visible:ring-gray-400': color === 'gray',
      'bg-red-500 text-white hover:bg-red-600 focus-visible:ring-red-500': color === 'danger'
    }"
  >
    <component :is="icon" class="w-4 h-4" v-if="icon && !loading" />
    <svg v-if="loading" class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
      <path class="opacity-75" fill="currentColor"
            d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 100 16z"/>
    </svg>
    <slot />
  </button>
</template>
