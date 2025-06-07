import { ref, reactive } from 'vue';
import axios from '@/utils/axios.js';
import { formatFitnessRecord } from '@/utils/commonMeta';
import emitter from '@/utils/eventBus.js';

export function useFitnessRecords() {
  const records = ref([]);
  const loading = ref(false);
  const total = ref(0);
  const current = ref(1);
  const pageSize = ref(7);

  const query = reactive({
    typeId: '',
    startDate: '',
    endDate: '',
    remark: ''
  });

  async function fetchRecords(page = current.value, size = pageSize.value) {
    loading.value = true;
    try {
      const params = {
        page,
        pageSize: size,
        ...query
      };

      const res = await axios.get('/api/fitness-record/list', { params });
      
      if (res.data?.success) {
        const rawRecords = res.data.data?.records || [];
        records.value = await Promise.all(rawRecords.map(record => formatFitnessRecord(record)));
        total.value = Number(res.data.data?.total) || 0;
        current.value = Number(res.data.data?.current) || 1;
        pageSize.value = Number(res.data.data?.size) || pageSize.value;
      } else {
        emitter.emit('notify', '获取数据失败: ' + (res.data?.message || '未知错误'), 'error');
      }
    } catch (err) {
      console.error('获取健身记录失败:', err);
      emitter.emit('notify', '获取数据失败: ' + (err.message || '未知错误'), 'error');
    } finally {
      loading.value = false;
    }
  }

  async function addRecord(formData) {
    try {
      const res = await axios.post('/api/fitness-record/add', {
        ...formData,
        finishTime: formData.finishTime + 'T00:00:00'
      });

      if (res.data?.success) {
        await fetchRecords();
        emitter.emit('notify', '添加成功', 'success');
        return true;
      }
      return false;
    } catch (err) {
      emitter.emit('notify', '添加失败: ' + (err.message || '未知错误'), 'error');
      return false;
    }
  }

  async function updateRecord(formData) {
    try {
      const res = await axios.put('/api/fitness-record/update', {
        ...formData,
        finishTime: formData.finishTime + 'T00:00:00'
      });

      if (res.data?.success) {
        await fetchRecords();
        emitter.emit('notify', '更新成功', 'success');
        return true;
      }
      return false;
    } catch (err) {
      emitter.emit('notify', '更新失败: ' + (err.message || '未知错误'), 'error');
      return false;
    }
  }

  async function deleteRecord(id) {
    try {
      const res = await axios.delete(`/api/fitness-record/delete/${id}`);
      if (res.data?.success) {
        await fetchRecords();
        emitter.emit('notify', '删除成功', 'success');
        return true;
      }
      return false;
    } catch (err) {
      emitter.emit('notify', '删除失败: ' + (err.message || '未知错误'), 'error');
      return false;
    }
  }

  function resetQuery() {
    query.typeId = '';
    query.startDate = '';
    query.endDate = '';
    query.remark = '';
    fetchRecords(1);
  }

  function handlePageChange(page) {
    fetchRecords(page, pageSize.value);
  }

  function handlePageSizeChange(size) {
    pageSize.value = size;
    fetchRecords(1, size);
  }

  return {
    records,
    loading,
    total,
    current,
    pageSize,
    query,
    fetchRecords,
    addRecord,
    updateRecord,
    deleteRecord,
    resetQuery,
    handlePageChange,
    handlePageSizeChange
  };
}
