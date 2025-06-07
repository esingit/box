import { ref } from 'vue';
import axios from '@/utils/axios.js';
import emitter from '@/utils/eventBus.js';
import { clearCommonMetaCache } from '@/utils/commonMeta';

export function useMetaData() {
  const types = ref([]);
  const units = ref([]);

  async function fetchMetaData() {
    try {
      const [typeRes, unitRes] = await Promise.all([
        axios.get('/api/common-meta/by-type', { params: { typeCode: 'FITNESS_TYPE' } }),
        axios.get('/api/common-meta/by-type', { params: { typeCode: 'UNIT' } })
      ]);

      if (typeRes.data?.success) {
        types.value = typeRes.data.data || [];
      }
      
      if (unitRes.data?.success) {
        units.value = unitRes.data.data || [];
      }
    } catch (err) {
      console.error('获取元数据失败:', err);
      emitter.emit('notify', '获取元数据失败，请稍后重试', 'error');
    }
  }

  async function refreshMetaData() {
    clearCommonMetaCache();
    await fetchMetaData();
  }

  return {
    types,
    units,
    fetchMetaData,
    refreshMetaData
  };
}
