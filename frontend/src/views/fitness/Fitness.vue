<template>
  <div class="page-container">
    <div class="page-header">
      <!-- 页面标题 -->
      <PageHeader title="健身" :icon="LucideDumbbell" />
    </div>
    
    <!-- 查询区域 -->
    <SearchPanel
      :query="query"
      :types="types"
      @update:query="val => { 
        Object.assign(query, val);
        console.log('Fitness - 查询条件已更新：', query);
      }"
      @search="handleQuery"
      @reset="resetQuery"
    />
    
    <!-- 操作按钮区域 -->
    <div class="action-bar">
      <button class="btn btn-primary" @click="handleAdd">
        添加记录
      </button>
    </div>

    <!-- 记录列表 -->
    <FitnessList
      :records="records"
      :current="current"
      :total="total"
      :page-size="pageSize"
      @edit="editRecord"
      @delete="handleDelete"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />

    <!-- 添加记录弹窗 -->
    <FitnessModal
      v-if="showAddModal"
      :show="showAddModal"
      :form="form"
      :types="types"
      :units="units"
      :loading="adding"
      title="添加记录"
      confirm-text="确定"
      remark-placeholder="备注（可选）"
      @submit="handleAddRecord"
      @cancel="closeAddModal"
    />

    <!-- 编辑记录弹窗 -->
    <FitnessModal
      v-if="editingIdx !== null"
      :show="editingIdx !== null"
      :form="editForm"
      :types="types"
      :units="units"
      :loading="false"
      title="编辑记录"
      confirm-text="保存"
      remark-placeholder="备注"
      @submit="saveEdit"
      @cancel="cancelEdit"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { LucideDumbbell } from 'lucide-vue-next';
import { useAuth } from '@/composables/useAuth';
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus.js';
import { useMetaData } from '@/composables/useMetaData';
import { useFitnessRecords } from '@/composables/useFitnessRecords';
import { useFitnessForm } from '@/composables/useFitnessForm';
import FitnessModal from '@/components/fitness/FitnessModal.vue';
import FitnessList from '@/components/fitness/FitnessList.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import SearchPanel from '@/components/fitness/SearchPanel.vue';

// Store
const userStore = useUserStore();
const isLoggedIn = computed(() => userStore.isLoggedIn);

// 组合式函数
const { types, units, fetchMetaData } = useMetaData();
const {
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
} = useFitnessRecords();

const {
  form,
  editForm,
  adding,
  editingIdx,
  showAddModal,
  resetForm,
  startEdit,
  cancelEdit,
  closeAddModal
} = useFitnessForm(types, units);

// 监听元数据变化，设置默认值
watch([types, units], ([newTypes, newUnits]) => {
  if (newTypes?.length > 0) {
    form.typeId = newTypes[0].id;
    editForm.typeId = newTypes[0].id;
  }
  if (newUnits?.length > 0) {
    form.unitId = newUnits[0].id;
    editForm.unitId = newUnits[0].id;
  }
}, { immediate: true });

// 生命周期钩子
onMounted(async () => {
  if (!isLoggedIn.value) {
    const { showLogin } = useAuth()
    showLogin('请先登录')
    return
  }

  try {
    // 初始化数据
    await Promise.all([
      fetchMetaData(),
      fetchRecords()
    ])

    // 监听全局刷新事件
    emitter.on('refresh-data', () => {
      Promise.all([
        fetchMetaData(),
        fetchRecords()
      ])
    })
  } catch (error) {
    console.error('初始化数据失败:', error)
    emitter.emit('notify', '加载数据失败：' + (error.message || '请刷新页面重试'), 'error')
  }
});

// 在组件销毁时移除事件监听
onUnmounted(() => {
  emitter.off('refresh-data');
});

// 事件处理函数  // 检查登录状态并执行操作
  function checkAuthAndExecute(action) {
    if (!isLoggedIn.value) {
      const { showLogin } = useAuth();
      showLogin('请先登录', () => action());
      return false;
    }
    return true;
  }

function handleAdd() {
  if (!checkAuthAndExecute(() => { showAddModal.value = true; })) return;
  showAddModal.value = true;
}

async function handleAddRecord() {
  if (!checkAuthAndExecute(async () => {
    try {
      adding.value = true;
      if (await addRecord(form)) {
        closeAddModal();
        emitter.emit('notify', '添加成功', 'success');
      }
    } finally {
      adding.value = false;
    }
  })) return;
  
  try {
    adding.value = true;
    if (await addRecord(form)) {
      closeAddModal();
      emitter.emit('notify', '添加成功', 'success');
    }
  } finally {
    adding.value = false;
  }
}

function editRecord(idx) {
  const record = records.value[idx];
  if (!record) return;
  
  if (!checkAuthAndExecute(() => startEdit(idx, record))) return;
  startEdit(idx, record);
}

async function saveEdit() {
  if (!editingIdx.value || !editForm.finishTime) return;
  
  const record = records.value[editingIdx.value];
  if (!record) return;
  
  if (!checkAuthAndExecute(async () => {
    if (await updateRecord({ ...editForm, id: record.id })) {
      cancelEdit();
    }
  })) return;
  
  if (await updateRecord({ ...editForm, id: record.id })) {
    cancelEdit();
  }
}

async function handleDelete(idx) {
  const record = records.value[idx];
  if (!record) return;
  
  // 先检查登录状态，如果未登录就显示登录弹窗
  if (!isLoggedIn.value) {
    const { showLogin } = useAuth();
    showLogin('请先登录');
    return;
  }

  try {
    const success = await deleteRecord(record.id);
    if (success) {
      emitter.emit('notify', '删除成功', 'success');
      await fetchRecords(); // 刷新列表
    }
  } catch (error) {
    console.error('删除记录失败：', error);
  }
}

function handleQuery() {
  // 确保使用最新的查询条件
  const currentQuery = { ...query };
  console.log('执行查询，当前查询条件：', currentQuery);
  if (Object.values(currentQuery).some(val => val !== '')) {
    fetchRecords(1); // 重置到第一页并使用当前查询条件进行搜索
  }
}
</script>
