<!-- /components/fitness/BaseActions.vue -->
<template>
  <div class="flex items-center gap-2">
    <BaseButton
        @click="$emit('edit')"
        color="outline"
        variant="action"
        :icon="LucidePencil"
    />
    <BaseButton
        @click="$emit('delete')"
        color="danger"
        variant="action"
        :icon="LucideTrash2"
    />
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next'
import BaseButton from "@/components/base/BaseButton.vue";

defineEmits(['edit', 'delete'])
</script>