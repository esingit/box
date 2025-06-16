<template>
  <div v-if="visible" class="modal-overlay" @click="close">
    <div class="modal-container animate-fade-in w-[500px]" @click.stop>
      <!-- 标题和关闭按钮 -->
      <div class="modal-header">
        <h2 class="modal-title">{{ title }}</h2>
        <button @click="close" class="btn-text text-xl">✕</button>
      </div>

      <div class="modal-divider" />

      <slot />
      <slot name="footer" />
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

