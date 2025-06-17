<template>
  <div class="bg-white border border-gray-300 rounded-md p-4">
    <div class="flex space-x-4">
      <div class="flex-grow space-y-4">
        <div class="flex space-x-4">
          <select
              v-model="localQuery.assetNameId"
              class="flex-1 border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">全部资产名称</option>
            <option v-for="name in assetNames" :key="name.id" :value="name.id">{{ name.name }}</option>
          </select>

          <select
              v-model="localQuery.typeId"
              class="flex-1 border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">全部资产类型</option>
            <option v-for="type in types" :key="type.id" :value="type.id">{{ type.value1 }}</option>
          </select>

          <select
              v-model="localQuery.locationId"
              class="flex-1 border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">全部资产位置</option>
            <option v-for="location in locations" :key="location.id" :value="location.id">{{ location.value1 }}</option>
          </select>
        </div>

        <transition
            enter-active-class="transition-opacity duration-300"
            leave-active-class="transition-opacity duration-300"
        >
          <div v-if="isExpanded" class="flex space-x-4 items-center">
            <div class="flex space-x-2 items-center">
              <input
                  type="date"
                  v-model="localQuery.startDate"
                  class="border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  aria-label="开始日期"
              />
              <span class="text-gray-500 select-none">至</span>
              <input
                  type="date"
                  v-model="localQuery.endDate"
                  class="border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  aria-label="结束日期"
              />
            </div>

            <input
                type="text"
                v-model="localQuery.remark"
                placeholder="备注关键词"
                class="flex-1 border border-gray-300 rounded-md px-3 py-2 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                aria-label="备注关键词"
            />
          </div>
        </transition>
      </div>

      <div class="flex flex-col space-y-2 justify-center">
        <button
            @click="isExpanded = !isExpanded"
            :title="isExpanded ? '收起更多查询' : '更多查询'"
            class="p-2 rounded-md text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label="切换更多查询"
            type="button"
        >
          <component :is="isExpanded ? LucideChevronUp : LucideChevronDown" class="w-5 h-5" />
        </button>

        <button
            @click="search"
            title="查询"
            class="p-2 rounded-md bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label="执行查询"
            type="button"
        >
          <LucideSearch class="w-5 h-5" />
        </button>

        <button
            @click="reset"
            title="重置"
            class="p-2 rounded-md text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label="重置查询条件"
            type="button"
        >
          <LucideRotateCcw class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue';
import { LucideSearch, LucideRotateCcw, LucideChevronDown, LucideChevronUp } from 'lucide-vue-next';

const props = defineProps({
  query: {
    type: Object,
    default: () => ({}),
  },
  types: {
    type: Array,
    default: () => [],
  },
  locations: {
    type: Array,
    default: () => [],
  },
  assetNames: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['updateQuery', 'search', 'reset']);
const isExpanded = ref(false);

const localQuery = reactive({
  assetNameId: '',
  typeId: '',
  locationId: '',
  startDate: '',
  endDate: '',
  remark: '',
});

function updateLocalQuery(source) {
  Object.assign(localQuery, {
    assetNameId: source.assetNameId || '',
    typeId: source.typeId || '',
    locationId: source.locationId || '',
    startDate: source.startDate || '',
    endDate: source.endDate || '',
    remark: source.remark || '',
  });
}

updateLocalQuery(props.query);

watch(
    () => props.query,
    (newQuery) => updateLocalQuery(newQuery),
    { deep: true }
);

watch(
    localQuery,
    (newQuery) => {
      emit('updateQuery', { ...newQuery });
    },
    { deep: true }
);

function search() {
  emit('search');
}

function reset() {
  emit('reset');
}
</script>
