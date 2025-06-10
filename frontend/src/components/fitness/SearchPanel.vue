<template>
  <div class="search-panel" style="background-color: var(--bg-secondary)">
    <div class="flex justify-between gap-4">
      <div class="flex-grow">
        <div class="form-group">
          <div class="control-group">
            <select v-model="localQuery.typeId" class="form-select">
              <option value="">全部类型</option>
              <option v-for="type in types" :key="type.id" :value="type.id">
                {{ type.value1 }}
              </option>
            </select>

            <div class="date-range">
              <input type="date" v-model="localQuery.startDate" class="input" />
              <span class="date-separator">至</span>
              <input type="date" v-model="localQuery.endDate" class="input" />
            </div>

            <input 
              class="input control-input"
              v-model="localQuery.remark" 
              placeholder="备注关键词" 
              type="text" 
            />
          </div>
        </div>
      </div>

      <div class="search-actions flex items-center gap-2">
        <button class="btn btn-icon btn-primary" title="查询" @click="search">
          <LucideSearch />
        </button>
        <button class="btn btn-icon btn-text" title="重置" @click="reset">
          <LucideRotateCcw />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, reactive } from 'vue';
import { LucideSearch, LucideRotateCcw } from 'lucide-vue-next';

const props = defineProps({
  query: {
    type: Object,
    required: true
  },
  types: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['update:query', 'search', 'reset']);

// 本地查询对象，用于实现双向绑定
const localQuery = reactive({
  typeId: props.query.typeId || '',
  startDate: props.query.startDate || '',
  endDate: props.query.endDate || '',
  remark: props.query.remark || ''
});

// 监听外部查询对象变化
watch(() => props.query, (newQuery) => {
  localQuery.typeId = newQuery.typeId || '';
  localQuery.startDate = newQuery.startDate || '';
  localQuery.endDate = newQuery.endDate || '';
  localQuery.remark = newQuery.remark || '';
}, { deep: true });

// 监听本地查询对象变化
watch(localQuery, (newQuery) => {
  console.log('SearchPanel - 本地查询条件变化：', newQuery);
  emit('update:query', {
    typeId: newQuery.typeId,
    startDate: newQuery.startDate,
    endDate: newQuery.endDate,
    remark: newQuery.remark
  });
}, { deep: true });

// 搜索和重置功能
function search() {
  emit('search');
}

function reset() {
  emit('reset');
}
</script>
