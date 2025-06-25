<script setup lang="ts">
import { computed, ref, useSlots } from 'vue'

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
  upload?: boolean
  accept?: string
  capture?: boolean | 'user' | 'environment'
}>()

const emit = defineEmits<{
  (e: 'change', file: File): void
}>()

const slots = useSlots()
const inputRef = ref<HTMLInputElement>()

const baseClasses = computed(() => {
  const colorClass = {
    primary: 'btn-primary',
    outline: 'btn-outline',
    text: 'btn-text',
    danger: 'btn-danger',
  }[props.color ?? 'primary']

  const variantClass = {
    default: '',
    action: 'btn-action',
    search: 'btn-search',
  }[props.variant ?? 'default']

  const sizeClass = props.size === 'sm' ? 'btn-sm' : ''
  const blockClass = props.block ? 'w-full' : ''
  const hasText = !!props.title || !!slots.default
  const gapClass = hasText ? 'gap-2' : ''
  const iconOnlyClass = hasText ? '' : 'p-2'
  const alignClass = props.block ? 'justify-start text-left' : 'justify-center text-center'

  return [
    'inline-flex items-center relative',
    alignClass,
    colorClass,
    variantClass,
    sizeClass,
    blockClass,
    gapClass,
    iconOnlyClass,
    'whitespace-nowrap',
  ].join(' ')
})

function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (file) emit('change', file)
}
</script>
<template>
  <label v-if="upload" :class="baseClasses">
    <!-- 隐藏的上传 input -->
    <input
        ref="inputRef"
        type="file"
        class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
        :accept="accept"
        :capture="capture"
        :disabled="disabled"
        @change="handleFileChange"
    />

    <!-- 左图标 -->
    <component
        v-if="icon && !loading && iconPosition !== 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />

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

    <span v-if="title">{{ title }}</span>
    <slot v-else />

    <!-- 右图标 -->
    <component
        v-if="icon && !loading && iconPosition === 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />
  </label>

  <button
      v-else
      :type="type ?? 'button'"
      :disabled="disabled || loading"
      :class="baseClasses"
  >
    <component
        v-if="icon && !loading && iconPosition !== 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />
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
    <span v-if="title">{{ title }}</span>
    <slot v-else />
    <component
        v-if="icon && !loading && iconPosition === 'right'"
        :is="icon"
        :class="iconSize ?? 'w-4 h-4'"
    />
  </button>
</template>
