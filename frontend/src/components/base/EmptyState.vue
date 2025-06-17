<template>
  <div
      class="flex flex-col items-center justify-center text-center space-y-2 py-12
           text-gray-500 dark:text-gray-400"
  >
    <component
        :is="iconComponent"
        size="48"
        class="text-gray-400 dark:text-gray-600"
        aria-hidden="true"
    />
    <div class="text-lg font-medium text-gray-700 dark:text-gray-300">
      {{ message }}
    </div>
    <div v-if="description" class="text-sm max-w-xs text-gray-500 dark:text-gray-400">
      {{ description }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { LucideInbox, LucideWallet, LucideDumbbell } from 'lucide-vue-next'

const props = defineProps({
  icon: {
    type: String,
    default: '',
    validator: v => ['Inbox', 'Wallet', 'Dumbbell', ''].includes(v)
  },
  message: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: ''
  }
})

const iconComponent = computed(() => {
  const icons = {
    Inbox: LucideInbox,
    Wallet: LucideWallet,
    Dumbbell: LucideDumbbell
  }
  return icons[props.icon] || LucideInbox
})
</script>
