<!--src/components/common/ImageViewer.vue-->
<template>
  <BaseModal
      :visible="visible"
      :hide-header="true"
      :draggable="true"
      :resizable="true"
      :z-index="3000"
      width="800px"
      :min-width="400"
      :min-height="300"
      :max-width="1200"
      :max-height="800"
      @update:visible="$emit('close')"
  >
    <template #default>
      <div class="relative flex items-center justify-center min-h-[60vh] -mx-6 -my-4">
        <!-- 浮动关闭按钮 -->
        <button
            @click="$emit('close')"
            class="absolute top-4 right-4 z-10 bg-black bg-opacity-50 text-white hover:bg-opacity-70 rounded-full p-2 transition-all duration-200"
            aria-label="关闭"
            type="button"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>

        <!-- 图片 -->
        <img
            :src="src"
            alt="查看大图"
            class="max-w-full max-h-full object-contain rounded-lg shadow-2xl"
            @click.stop
        />
      </div>
    </template>

    <template #footer>
      <div class="text-center text-gray-500 text-sm">
        <p>拖拽顶部移动 • 拖拽边缘调整大小 • 点击空白区域或按 ESC 键关闭</p>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean
  src: string
}>()

defineEmits(['close'])
</script>