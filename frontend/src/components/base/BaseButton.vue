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
  variant?: 'default' | 'action' | 'search'
}>()

const slots = useSlots()

const baseClasses = computed(() => {
  // 主色类
  const colorClass = {
    primary: 'btn-primary',
    outline: 'btn-outline',
    text: 'btn-text',
    danger: 'btn-danger',
  }[props.color ?? 'primary']

  // variant 类（宽高控制）
  const variantClass = {
    default: '',
    action: 'btn-action',
    search: 'btn-search',
  }[props.variant ?? 'default']

  // 尺寸类
  const sizeClass = props.size === 'sm' ? 'btn-sm' : ''

  // 宽度全占
  const blockClass = props.block ? 'w-full' : ''

  // 有无文字判断，用于控制 icon-only 状态
  const hasText = !!props.title || !!slots.default
  const gapClass = hasText ? 'gap-2' : ''
  const iconOnlyClass = hasText ? '' : 'p-2'

  return [
    colorClass,
    variantClass,
    sizeClass,
    blockClass,
    gapClass,
    iconOnlyClass,
  ].join(' ')
})
</script>

<template>
  <button
      :type="type ?? 'button'"
      :disabled="disabled || loading"
      :class="['inline-flex items-center justify-center whitespace-nowrap', baseClasses]"
  >
    <!-- 图标（左） -->
    <component
        v-if="icon && !loading && iconPosition !== 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />

    <!-- 加载图标 -->
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

    <!-- 文字内容 -->
    <span v-if="title">{{ title }}</span>
    <slot v-else />

    <!-- 图标（右） -->
    <component
        v-if="icon && !loading && iconPosition === 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />
  </button>
</template>
