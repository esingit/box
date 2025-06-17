<template>
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="visible"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          @click="close"
      >
        <div
            class="bg-white rounded-2xl shadow-xl animate-fade-in"
            :class="widthClass"
            :style="{ width: width || undefined }"
            @click.stop
        >
          <!-- Header -->
          <div class="flex items-center justify-between px-6 py-4 border-b">
            <h2 class="text-lg font-semibold">{{ title }}</h2>
            <button @click="close" class="text-gray-500 hover:text-black text-xl">✕</button>
          </div>

          <!-- 内容插槽 -->
          <div class="p-6">
            <slot />
          </div>

          <!-- 底部插槽 -->
          <div v-if="$slots.footer" class="px-6 py-4 border-t bg-gray-50 text-right rounded-b-2xl">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount } from 'vue'

const props = defineProps<{
  visible: boolean
  title?: string
  width?: string // 自定义像素宽度，如 "800px"
  widthClass?: string // 自定义类名，如 "w-[800px] max-w-[90vw]"
}>()

const emit = defineEmits(['update:visible'])

function close() {
  emit('update:visible', false)
}

function handleKeyDown(e: KeyboardEvent) {
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
  transition: opacity 0.5s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
