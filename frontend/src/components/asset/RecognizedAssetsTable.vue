<!-- src/components/asset/RecognizedAssetsTable.vue -->
<template>
  <BaseTable
      ref="tableRef"
      :columns="columns"
      :data="data"
      :loading="loading"
      @delete="handleDelete"
  >
    <!-- 扫描结果列 -->
    <template #cell-assetName="{ record }">
      <span
          :class="textClass"
          :title="record.assetName || '暂无扫描结果'"
      >
        {{ record.assetName || '暂无扫描结果' }}
      </span>
    </template>

    <!-- 资产名称列 -->
    <template #cell-assetNameId="{ record, index, setActiveRow, clearActiveRow }">
      <BaseSelect
          :model-value="record.assetNameId"
          :title="'资产名称'"
          :options="assetNameOptions"
          :multiple="false"
          :clearable="true"
          placeholder="请选择资产名称"
          class="w-full"
          @update:model-value="handleFieldChange(index, 'assetNameId', $event)"
          @dropdown-open="setActiveRow(index)"
          @dropdown-close="clearActiveRow"
      />
    </template>

    <!-- 其他列保持不变 -->
    <template #cell-amount="{ record, index }">
      <input
          :value="record.amount"
          type="number"
          step="0.01"
          :class="inputClass"
          placeholder="0.00"
          @input="handleAmountChange(index, ($event.target as HTMLInputElement).value)"
      />
    </template>

    <template #cell-remark="{ record, index }">
      <input
          :value="record.remark"
          type="text"
          :class="inputClass"
          placeholder="备注信息"
          @input="handleFieldChange(index, 'remark', ($event.target as HTMLInputElement).value)"
      />
    </template>

    <template #actions="{ record, index }">
      <BaseButton
          type="button"
          title="删除"
          color="danger"
          :icon="Trash2"
          @click="handleRemoveItem(index)"
      >
        <Trash2 class="w-4 h-4" />
      </BaseButton>
    </template>
  </BaseTable>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Trash2 } from 'lucide-vue-next'
import { useAssetNameStore } from '@/store/assetNameStore'
import BaseTable from '@/components/base/BaseTable.vue'
import BaseButton from "@/components/base/BaseButton.vue"
import BaseSelect from "@/components/base/BaseSelect.vue"
import { RecognizedAssetItem } from "@/types/asset"

// 其余代码保持不变...
const props = defineProps<{
  data: RecognizedAssetItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  removeItem: [index: number]
  updateItem: [index: number, field: string, value: any]
}>()

const assetNameStore = useAssetNameStore()
const tableRef = ref()

const textClass = computed(() =>
    'text-sm text-gray-900 dark:text-gray-100 truncate block max-w-xs'
)

const inputClass = computed(() =>
    'w-full px-2 py-1 border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent'
)

const assetNameOptions = computed(() => {
  return assetNameStore.assetNameOptions || []
})

const columns = [
  {
    key: 'assetName',
    label: '扫描结果',
    resizable: true,
    defaultWidth: 350,
    type: 'custom'
  },
  {
    key: 'assetNameId',
    label: '资产名称',
    resizable: true,
    defaultWidth: 350,
    type: 'custom'
  },
  {
    key: 'amount',
    label: '金额',
    resizable: true,
    defaultWidth: 150,
    type: 'custom'
  },
  {
    key: 'remark',
    label: '备注',
    resizable: true,
    defaultWidth: 150,
    type: 'custom'
  },
  {
    key: 'actions',
    label: '操作',
    resizable: false,
    defaultWidth: 100,
    actions: true
  }
]

onMounted(async () => {
  props.data.forEach(item => {
    if (item.assetNameId === '' || item.assetNameId === undefined) {
      item.assetNameId = null
    } else if (typeof item.assetNameId === 'string' && item.assetNameId) {
      item.assetNameId = Number(item.assetNameId)
    }
  })

  if (!assetNameStore.assetNameOptions || assetNameStore.assetNameOptions.length === 0) {
    try {
      await assetNameStore.fetchAssetNameOptions?.()
    } catch (error) {
      console.error('获取资产名称选项失败:', error)
    }
  }
})

const handleFieldChange = (index: number, field: string, value: any) => {
  const item = props.data[index]
  if (item) {
    ;(item as any)[field] = value
    emit('updateItem', index, field, value)
  }
}

const handleAmountChange = (index: number, value: string) => {
  const numValue = value === '' ? null : Number(value)
  handleFieldChange(index, 'amount', numValue)
}

const handleDelete = (record: RecognizedAssetItem, index: number) => {
  handleRemoveItem(index)
}

const handleRemoveItem = (index: number) => {
  emit('removeItem', index)
}
</script>