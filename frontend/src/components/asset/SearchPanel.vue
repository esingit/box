<template>
  <div class="search-panel" style="background-color: var(--bg-secondary)">
    <div class="flex justify-between gap-4">
      <div class="flex-grow">
        <div class="form-group">
          <div class="control-group">
            <select v-model="localQuery.assetName" class="input control-select">
              <option value="">资产名称</option>
              <option v-for="name in assetNames" :key="name" :value="name">
                {{ name }}
              </option>
            </select>

            <select v-model="localQuery.typeId" class="input control-select">
              <option value="">资产类型</option>
              <option v-for="type in types" :key="type.id" :value="type.id">
                {{ type.value1 }}
              </option>
            </select>

            <select v-model="localQuery.location" class="input control-select">
              <option value="">资产位置</option>
              <option v-for="location in locations" :key="location" :value="location">
                {{ location }}
              </option>
            </select>
          </div>
        </div>

        <transition
          enter-active-class="animate__animated animate__fadeIn"
          leave-active-class="animate__animated animate__fadeOut"
        >
          <div v-show="isExpanded" class="form-group">
            <div class="control-group">
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
        </transition>
      </div>

      <div class="search-actions flex items-center gap-2">
        <button class="btn btn-icon btn-text" title="更多查询" @click="isExpanded = !isExpanded">
          <LucideChevronDown v-if="!isExpanded" />
          <LucideChevronUp v-else />
        </button>
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
import { ref, reactive, watch } from 'vue';
import { LucideSearch, LucideRotateCcw, LucideChevronDown, LucideChevronUp } from 'lucide-vue-next';

const props = defineProps({
  query: {
    type: Object,
    required: true
  },
  types: {
    type: Array,
    required: true
  },
  locations: {
    type: Array,
    required: true
  },
  assetNames: {
    type: Array,
    required: true
  }
});

const emit = defineEmits(['update:query', 'search', 'reset']);
const isExpanded = ref(false);

const localQuery = reactive({
  assetName: props.query.assetName || '',
  typeId: props.query.typeId || '',
  location: props.query.location || '',
  startDate: props.query.startDate || '',
  endDate: props.query.endDate || '',
  remark: props.query.remark || ''
});

watch(() => props.query, (newQuery) => {
  localQuery.assetName = newQuery.assetName || '';
  localQuery.typeId = newQuery.typeId || '';
  localQuery.location = newQuery.location || '';
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


