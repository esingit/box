<template>
  <div class="w-full bg-white border rounded-xl p-4 shadow-sm">
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <!-- 左侧筛选项 -->
      <div class="flex flex-wrap items-center gap-4 flex-1">
        <!-- 类型下拉 -->
        <select v-model="localQuery.typeId"
                class="h-10 rounded-lg border border-gray-300 text-sm px-3 focus:outline-none focus:ring-2 focus:ring-black bg-white">
          <option value="">全部类型</option>
          <option v-for="type in types" :key="type.id" :value="type.id">
            {{ type.value1 }}
          </option>
        </select>

        <!-- 日期区间 -->
        <div class="flex items-center gap-2">
          <input type="date" v-model="localQuery.startDate"
                 class="h-10 rounded-lg border border-gray-300 text-sm px-3 focus:outline-none focus:ring-2 focus:ring-black" />
          <span class="text-gray-400">至</span>
          <input type="date" v-model="localQuery.endDate"
                 class="h-10 rounded-lg border border-gray-300 text-sm px-3 focus:outline-none focus:ring-2 focus:ring-black" />
        </div>

        <!-- 备注关键词 -->
        <input type="text" v-model="localQuery.remark" placeholder="备注关键词"
               class="h-10 w-48 rounded-lg border border-gray-300 text-sm px-3 focus:outline-none focus:ring-2 focus:ring-black" />
      </div>

      <!-- 操作按钮 -->
      <div class="flex items-center gap-2 shrink-0">
        <button @click="search" title="查询"
                class="h-10 w-10 flex items-center justify-center rounded-lg bg-black text-white hover:bg-gray-800 transition">
          <LucideSearch class="w-4 h-4" />
        </button>
        <button @click="reset" title="重置"
                class="h-10 w-10 flex items-center justify-center rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-100 transition">
          <LucideRotateCcw class="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, reactive } from 'vue';
import { LucideSearch, LucideRotateCcw } from 'lucide-vue-next';

const props = defineProps({
  query: Object,
  types: Array
});
const emit = defineEmits(['update:query', 'search', 'reset']);

const localQuery = reactive({
  typeId: props.query.typeId || '',
  startDate: props.query.startDate || '',
  endDate: props.query.endDate || '',
  remark: props.query.remark || ''
});

watch(() => props.query, (newQuery) => {
  localQuery.typeId = newQuery.typeId || '';
  localQuery.startDate = newQuery.startDate || '';
  localQuery.endDate = newQuery.endDate || '';
  localQuery.remark = newQuery.remark || '';
}, { deep: true });

watch(localQuery, (newQuery) => {
  emit('update:query', { ...newQuery });
}, { deep: true });

function search() {
  emit('search');
}
function reset() {
  emit('reset');
}
</script>
