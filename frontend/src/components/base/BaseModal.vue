<!-- components/base/BaseModal.vue -->
<template>
  <div
      v-if="visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
  >
    <div
        class="bg-white rounded-xl shadow-xl w-full max-w-md p-6 relative animate-fade-in"
        @click.stop
    >
      <!-- 标题 -->
      <h2 class="text-xl font-semibold mb-2">{{ title }}</h2>
      <!-- 分割线，负margin让它撑满modal宽度 -->
      <div class="h-px bg-gray-200 mb-4 -mx-6" />

      <!-- 插槽内容 -->
      <slot />
      <slot name="footer" />

      <!-- 关闭按钮 -->
      <button
          @click="close"
          class="absolute top-2 right-3 text-gray-400 hover:text-gray-600"
      >
        ✕
      </button>
    </div>
  </div>
</template>


<script setup lang="ts">
const props = defineProps<{
  visible: boolean
  title?: string
}>()

const emit = defineEmits(['update:visible'])

function close() {
  emit('update:visible', false)
}
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
.animate-fade-in {
  animation: fade-in 0.2s ease-out;
}
</style>
