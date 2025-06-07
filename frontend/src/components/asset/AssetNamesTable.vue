<template>
  <div>
    <table class="table">
      <thead>
        <tr>
          <th v-for="header in tableHeaders" :key="header.key">
            {{ header.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="names.length === 0">
          <td :colspan="tableHeaders.length">
            <EmptyState
              icon="Wallet"
              message="暂无资产名称"
              description="点击上方的新增按钮添加资产名称"
            />
          </td>
        </tr>
        <tr v-for="name in names" :key="name.id">
          <td>{{ name.name }}</td>
          <td class="remark-cell">
            <span :title="name.description">{{ name.description || '-' }}</span>
          </td>
          <td>
            <div class="operations">
              <button class="action-btn" title="编辑" @click="$emit('edit', name)">
                <LucidePencil :size="16" />
              </button>
              <button class="action-btn delete" title="删除" @click="$emit('delete', name)">
                <LucideTrash2 :size="16" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { LucidePencil, LucideTrash2 } from 'lucide-vue-next'
import EmptyState from '@/components/common/EmptyState.vue'

defineProps({
  names: {
    type: Array,
    required: true
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'name', label: '资产名称' },
  { key: 'description', label: '描述' },
  { key: 'actions', label: '操作' }
]
</script>
