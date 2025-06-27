<!-- RecognizedAssetsTable.vue -->
<template>
  <div class="w-full">
    <BaseTable
        ref="tableRef"
        :columns="columns"
        :data="data"
        :loading="loading"
        @delete="handleDelete"
    >
      <!-- 扫描结果列 -->
      <template #cell-assetName="{ record }">
        <div class="flex flex-col gap-1">
          <span class="text-sm text-gray-900 dark:text-gray-100 block truncate">
            {{ record.assetName || '暂无扫描结果' }}
          </span>
          <!-- 显示匹配信息 -->
          <span v-if="record.matchScore" class="text-xs text-gray-500">
            匹配度: {{ (record.matchScore * 100).toFixed(0) }}%
          </span>
        </div>
      </template>

      <!-- 资产名称列 -->
      <template #cell-assetNameId="{ record, index, setActiveRow, clearActiveRow }">
        <BaseSelect
            :model-value="record.assetNameId"
            :title="'资产名称'"
            :options="assetNameOptions"
            clearable
            searchable
            placeholder="请选择资产名称"
            class="w-full"
            @update:model-value="handleFieldChange(index, 'assetNameId', $event)"
            @dropdown-open="setActiveRow?.(index)"
            @dropdown-close="clearActiveRow?.()"
        />
      </template>

      <!-- 金额列 -->
      <template #cell-amount="{ record, index }">
        <input
            :value="record.amount"
            type="number"
            step="0.01"
            class="w-full px-2 py-1 border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
            placeholder="0.00"
            @input="handleAmountChange(index, ($event.target as HTMLInputElement).value)"
        />
      </template>

      <!-- 备注列 -->
      <template #cell-remark="{ record, index }">
        <input
            :value="record.remark"
            type="text"
            class="w-full px-2 py-1 border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
            placeholder="备注信息"
            @input="handleFieldChange(index, 'remark', ($event.target as HTMLInputElement).value)"
        />
      </template>

      <template #actions="{ index }">
        <BaseButton
            type="button"
            title="删除"
            color="danger"
            :icon="Trash2"
            @click="handleRemoveItem(index)"
        >
          <Trash2 class="w-4 h-4"/>
        </BaseButton>
      </template>
    </BaseTable>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, ref} from 'vue'
import {Trash2} from 'lucide-vue-next'
import {useAssetNameStore} from '@/store/assetNameStore'
import BaseTable from '@/components/base/BaseTable.vue'
import BaseButton from "@/components/base/BaseButton.vue"
import BaseSelect from "@/components/base/BaseSelect.vue"
import {RecognizedAssetItem} from "@/types/asset"

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

// 安全处理大数ID的函数
function safeParseId(id: any): string | null {
  if (id === null || id === undefined || id === '') {
    return null
  }

  // 保持为字符串类型，避免精度丢失
  return String(id)
}

// 改进的资产名称选项计算 - 确保ID保持为字符串
const assetNameOptions = computed(() => {
  // 优先使用store提供的计算属性，但确保value保持为字符串
  if (assetNameStore.assetNameOptions && assetNameStore.assetNameOptions.length > 0) {
    return assetNameStore.assetNameOptions.map(option => ({
      label: String(option.label || ''),
      value: String(option.value || '') // 保持为字符串
    }))
  }

  // 备选：从assetName数组构建选项
  if (assetNameStore.assetName && assetNameStore.assetName.length > 0) {
    return assetNameStore.assetName.map(item => ({
      label: String(item.name || item.label || '未命名'),
      value: String(item.id || '') // 保持为字符串
    }))
  }

  return []
})

const columns = [
  {
    key: 'assetName',
    label: '扫描结果',
    resizable: true,
    defaultWidth: 300,
    type: 'test',
    headerAlign: 'left',
    align: 'left',
    sortable: true
  },
  {
    key: 'assetNameId',
    label: '资产名称',
    resizable: true,
    defaultWidth: 400,
    type: 'custom',
    headerAlign: 'left',
    align: 'left',
    sortable: true
  },
  {
    key: 'amount',
    label: '金额',
    resizable: true,
    defaultWidth: 150,
    type: 'custom',
    headerAlign: 'right',
    align: 'right',
    sortable: true
  },
  {
    key: 'remark',
    label: '备注',
    resizable: true,
    defaultWidth: 150,
    type: 'custom',
    headerAlign: 'left',
    align: 'left',
    sortable: true
  },
  {
    key: 'actions',
    label: '操作',
    resizable: false,
    defaultWidth: 100,
    actions: true,
    headerAlign: 'center',
    align: 'center'
  }
]

// 强制加载资产名称数据
async function forceLoadAssetNames() {
  try {
    if (assetNameStore.fetchAssetName) {
      await assetNameStore.fetchAssetName(true)
    }

    if (assetNameStore.loadList && (!assetNameStore.assetName || assetNameStore.assetName.length === 0)) {
      await assetNameStore.loadList(true)
    }

    await nextTick()
  } catch (error) {
    console.error('加载资产名称数据失败:', error)
  }
}

onMounted(async () => {
  await forceLoadAssetNames()

  // 处理数据中的 assetNameId - 保持为字符串类型
  if (props.data && props.data.length > 0) {
    props.data.forEach((item) => {
      item.assetNameId = safeParseId(item.assetNameId)
    })
  }
})

const handleFieldChange = (index: number, field: string, value: any) => {
  const item = props.data[index]
  if (item) {
    if (field === 'assetNameId') {
      // 保持为字符串类型
      value = safeParseId(value)
    }
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

// 暴露方法供父组件调用
defineExpose({
  forceLoadAssetNames
})
</script>