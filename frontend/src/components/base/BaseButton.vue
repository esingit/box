<script setup lang="ts">
import { computed, useSlots } from 'vue'

const props = defineProps<{
  type?: 'button' | 'submit' | 'reset'
  color?: 'primary' | 'outline' | 'text' | 'danger'
  title?: string
  icon?: any
  iconPosition?: 'left' | 'right'
  iconSize?: string
  loading?: boolean
  disabled?: boolean
  size?: 'sm' | 'md'
  block?: boolean
}>()

const slots = useSlots()

const baseClasses = computed(() => {
  const colorClass = {
    primary: 'btn-primary',
    outline: 'btn-outline',
    text: 'btn-text',
    danger: 'btn-danger',
  }[props.color ?? 'primary']

  const sizeClass = props.size === 'sm' ? 'btn-sm' : ''
  const blockClass = props.block ? 'w-full' : ''

  // 没有文字也没 slot，自动加 padding
  const isIconOnly = !props.title && !slots.default
  const iconOnlyClass = isIconOnly ? 'p-2' : ''

  return [colorClass, sizeClass, blockClass, iconOnlyClass].join(' ')
})
</script>

<template>
  <button
      :type="type ?? 'button'"
      :disabled="disabled || loading"
      :class="baseClasses"
  >
    <!-- 左图标 -->
    <component
        v-if="icon && !loading && iconPosition !== 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />

    <!-- loading 替代图标 -->
    <svg
        v-if="loading"
        :class="iconSize ?? 'w-4 h-4'"
        class="animate-spin"
        viewBox="0 0 24 24"
        fill="none"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor"
            d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 100 16z" />
    </svg>

    <!-- 显示 title 或 slot -->
    <span v-if="title">{{ title }}</span>
    <slot v-else />

    <!-- 右图标 -->
    <component
        v-if="icon && !loading && iconPosition === 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />
  </button>
</template>
