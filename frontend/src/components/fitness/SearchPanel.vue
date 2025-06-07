<template>
  <div class="fitness-query-bar">
    <div class="query-fields query-bar-fields">
      <select v-model="localQuery.typeId">
        <option value="">全部类型</option>
        <option v-for="type in types" :key="type.id" :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
      
      <input type="date" v-model="localQuery.startDate" />
      <span class="to-text">至</span>
      <input type="date" v-model="localQuery.endDate" />
      
      <input 
        v-model="localQuery.remark" 
        placeholder="备注关键词" 
        type="text" 
      />
    </div>
    
    <div class="query-btns">
      <button class="btn btn-primary" title="查询" @click="search">
        <LucideSearch size="18" style="vertical-align: middle;" />
      </button>
      <button class="btn btn-outline" title="重置" @click="reset">
        <LucideRotateCcw size="18" style="vertical-align: middle;" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
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
const localQuery = ref({ ...props.query });

// 监听外部查询对象变化
watch(() => props.query, (newQuery) => {
  localQuery.value = { ...newQuery };
}, { deep: true });

// 监听本地查询对象变化
watch(localQuery, (newQuery) => {
  emit('update:query', { ...newQuery });
}, { deep: true });

// 搜索和重置功能
function search() {
  emit('search');
}

function reset() {
  emit('reset');
}
</script>
