<!-- src/components/base/BaseModal.vue -->
<template>
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="visible"
          class="fixed inset-0 flex items-center justify-center bg-black/50"
          :style="{ zIndex: computedZIndex }"
          @click.self="close"
      >
        <div
            class="bg-white rounded-2xl shadow-xl animate-fade-in dark:bg-gray-800"
            :class="widthClass"
            :style="[computedStyle, { zIndex: computedZIndex + 10 }]"
            @click.stop
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-gray-100">{{ title }}</h2>
            <button
                @click="close"
                class="text-gray-500 hover:text-gray-900 dark:hover:text-white text-xl transition-colors duration-200"
                aria-label="关闭弹窗"
                type="button"
            >
              ✕
            </button>
          </div>

          <!-- 内容插槽 -->
          <div class="p-6 text-gray-800 dark:text-gray-200">
            <slot />
          </div>

          <!-- 底部插槽 -->
          <div
              v-if="$slots.footer"
              class="px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-right rounded-b-2xl"
          >
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps<{
  visible: boolean
  title?: string
  width?: string
  widthClass?: string
  zIndex?: number // 支持传入弹窗层级，默认1050
}>()

const emit = defineEmits(['update:visible'])

const close = () => {
  emit('update:visible', false)
}

const computedStyle = computed(() => {
  return props.width ? { width: props.width } : {}
})

const computedZIndex = computed(() => {
  return props.zIndex ?? 2000
})

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') close()
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
