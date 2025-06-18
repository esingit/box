<!-- src/components/base/BaseModal.vue -->
<template>
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="visible"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          @click.self="close"
      >
        <div
            class="bg-white rounded-2xl shadow-xl animate-fade-in"
            :class="widthClass"
            :style="computedStyle"
            @click.stop
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 class="text-lg font-semibold text-gray-900">{{ title }}</h2>
            <button
                @click="close"
                class="text-gray-500 hover:text-gray-900 text-xl transition-colors duration-200"
                aria-label="关闭弹窗"
            >
              ✕
            </button>
          </div>

          <!-- 内容插槽 -->
          <div class="p-6">
            <slot />
          </div>

          <!-- 底部插槽 -->
          <div v-if="$slots.footer" class="px-6 py-4 border-t border-gray-200 bg-white text-right rounded-b-2xl">
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
  width?: string     // 支持内联宽度字符串，如 '500px', '80vw'
  widthClass?: string  // 支持Tailwind宽度类，如 'max-w-lg', 'w-96'
}>()

const emit = defineEmits(['update:visible'])

const close = () => {
  emit('update:visible', false)
}

// 如果传了 width（内联样式宽度），优先使用
const computedStyle = computed(() => {
  return props.width ? { width: props.width } : {}
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
