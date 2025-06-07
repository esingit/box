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
          <td class="operations">
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
  { key: 'name', label: '资产名称' },
  { key: 'description', label: '描述' },
  { key: 'actions', label: '操作' }
]
</script>
