<template>
  <div class="empty-state">
    <component 
      :is="icon ? iconComponent : LucideInbox"
      :size="48"
      class="empty-icon"
    />
    <div class="empty-text">{{ message }}</div>
    <div v-if="description" class="empty-description">
      {{ description }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { 
  LucideInbox,
  LucideWallet,
  LucideDumbbell
} from 'lucide-vue-next'

const props = defineProps({
  icon: {
    type: String,
    default: ''
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
    'Inbox': LucideInbox,
    'Wallet': LucideWallet,
    'Dumbbell': LucideDumbbell
  }
  return icons[props.icon] || LucideInbox
})
</script>
