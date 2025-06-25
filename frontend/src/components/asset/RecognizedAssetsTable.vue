<template>
  <div class="overflow-x-auto border rounded-lg border-gray-200 dark:border-gray-700">
    <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
      <thead class="bg-gray-50 dark:bg-gray-900">
      <tr>
        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">资产名称</th>
        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">金额</th>
        <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
      </tr>
      </thead>
      <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
      <tr v-for="(item, index) in data" :key="index">
        <td class="px-3 py-2">
          <input
              v-model="item.assetName"
              type="text"
              class="w-full px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
          />
        </td>
        <td class="px-3 py-2">
          <input
              v-model.number="item.amount"
              type="number"
              step="0.01"
              class="w-full px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
          />
        </td>
        <td class="px-3 py-2">
          <BaseButton
              type="button"
              title="删除"
              color="danger"
              :icon="LucideTrash2"
              @click="$emit('removeItem', index)"
          >
            <Trash2 class="w-4 h-4" />
          </BaseButton>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { Trash2, LucideTrash2 } from 'lucide-vue-next'
import BaseButton from "@/components/base/BaseButton.vue";

const props = defineProps<{
  data: Array<{
    assetName: string
    amount: number
  }>
}>();

const emit = defineEmits<{
  (e: 'removeItem', index: number): void;
}>();
</script>
