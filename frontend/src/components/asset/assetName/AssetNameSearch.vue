<template>
  <div class="flex items-center justify-between mb-4 space-x-3">
    <div class="relative flex-1">
      <BaseInput
          v-model="localSearchTerm"
          type="text"
          clearable
          placeholder="搜索资产名称"
          autocomplete="off"
          @clear="onClear"
      />
    </div>
    <BaseButton
        v-if="!loading"
        @click="onAddNew"
        color="primary"
        title="添加资产名称"
        type="button"
        :icon="LucidePlus"
    />
  </div>
</template>

<script setup lang="ts">
import { LucidePlus } from 'lucide-vue-next'

const props = defineProps({
  searchTerm: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:searchTerm', 'addNew'])

const localSearchTerm = ref(props.searchTerm)

// 监听父组件传入searchTerm变化，更新本地变量
watch(
    () => props.searchTerm,
    (val) => {
      if (val !== localSearchTerm.value) {
        localSearchTerm.value = val
      }
    }
)

// 本地搜索词变化同步给父组件，触发筛选
watch(localSearchTerm, (val) => {
  emit('update:searchTerm', val)
})

function onClear() {
  localSearchTerm.value = ''
  // 这里emit自动触发watch同步，无需额外emit
}

function onAddNew() {
  emit('addNew')  // 触发新增事件
}
</script>
