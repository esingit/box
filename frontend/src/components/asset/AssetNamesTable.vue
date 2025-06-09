<template>
  <div class="data-table">
    <table class="table">
      <thead>
        <tr>
          <th v-for="header in tableHeaders" 
              :key="header.key"
              :class="header.class">
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
        <tr v-for="name in names" :key="name.id" class="table-row">
          <td class="cell-text">{{ name.name }}</td>
          <td class="cell-remark">
            <span v-if="name.description" class="remark-text" :title="name.description">
              {{ name.description }}
            </span>
            <span v-else class="text-muted">-</span>
          </td>
          <td class="cell-actions">
            <RecordActions 
              :record="name"
              type="asset-name"
              @edit="$emit('edit', name)"
              @delete="$emit('delete', name)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import EmptyState from '@/components/common/EmptyState.vue'
import RecordActions from '@/components/common/RecordActions.vue'

defineProps({
  names: {
    type: Array,
    required: true
  }
})

defineEmits(['edit', 'delete'])

const tableHeaders = [
  { key: 'name', label: '资产名称', class: 'cell-text' },
  { key: 'description', label: '描述', class: 'cell-remark' },
  { key: 'actions', label: '操作', class: 'cell-actions' }
]
</script>
