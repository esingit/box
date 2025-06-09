<template>
  <div class="page-container">
    <!-- 健身统计概览 -->
    <div class="card">
      <div class="card-header flex-between">
        <h3 class="card-title">健身记录统计</h3>
        <div class="card-actions">
          <button 
            class="btn btn-icon btn-text" 
            @click="refreshData"
            title="刷新数据"
          >
            <LucideRefreshCw class="btn-icon-md" />
          </button>
        </div>
      </div>

      <div class="card-grid">
        <template v-if="loading">
          <SkeletonCard v-for="i in 3" :key="i" class="stat-card" />
        </template>
        <template v-else>
          <div class="stat-card">
            <h4 class="stat-label">本月运动</h4>
            <div class="stat-value">{{ stats.monthlyCount || 0 }}次</div>
            <div class="stat-info">过去30天</div>
          </div>
          <div class="stat-card">
            <h4 class="stat-label">连续运动</h4>
            <div class="stat-value">{{ stats.streakDays || 0 }}天</div>
            <div class="stat-info">当前连续</div>
          </div>
          <div class="stat-card">
            <h4 class="stat-label">累计运动</h4>
            <div class="stat-value">{{ stats.totalCount || 0 }}次</div>
            <div class="stat-info">总计记录</div>
          </div>
        </template>
      </div>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="content-section">
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
      
      <div class="action-bar content-action">
        <button class="btn btn-primary" @click="handleAdd">
          <LucidePlus class="btn-icon-sm" />
          添加记录
        </button>
      </div>
    </div>

    <!-- 记录列表区域 -->
    <div class="content-section">
      <FitnessList
        v-if="!loading"
        :records="records"
        :current="current"
        :total="total"
        :page-size="pageSize"
        @edit="editRecord"
        @delete="handleDelete"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      />

      <!-- 加载中骨架屏 -->
      <div v-else class="skeleton-list">
        <SkeletonCard v-for="n in pageSize" :key="n" />
      </div>
    </div>

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
import { LucideDumbbell, LucidePlus, LucideRefreshCw } from 'lucide-vue-next';
import { useAuth } from '@/composables/useAuth';
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus.js';
import { useMetaData } from '@/composables/useMetaData';
import { useFitnessRecords } from '@/composables/useFitnessRecords';
import { useFitnessForm } from '@/composables/useFitnessForm';
import FitnessModal from '@/components/fitness/FitnessModal.vue';
import FitnessList from '@/components/fitness/FitnessList.vue';
import SearchPanel from '@/components/fitness/SearchPanel.vue';
import SkeletonCard from '@/components/common/SkeletonCard.vue';

// Store
const userStore = useUserStore();
const isLoggedIn = computed(() => userStore.isLoggedIn);

// 统计数据
const stats = ref({
  monthlyCount: 0,
  streakDays: 0,
  totalCount: 0
});

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

// 检查登录状态并执行操作
async function checkAuthAndExecute(action) {
  console.log('检查登录状态:', isLoggedIn.value);
  
  if (!isLoggedIn.value) {
    console.log('用户未登录，显示登录框');
    const auth = useAuth();
    auth.showLogin('请先登录', async () => {
      console.log('登录成功，准备执行回调操作');
      try {
        await action();
      } catch (error) {
        console.error('执行操作时出错:', error);
        emitter.emit('notify', '操作执行失败，请重试', 'error');
      }
    });
    return false;
  }
  
  console.log('用户已登录，直接执行操作');
  try {
    await action();
  } catch (error) {
    console.error('执行操作时出错:', error);
    emitter.emit('notify', '操作执行失败，请重试', 'error');
  }
  return true;
}

// 刷新数据
async function refreshData() {
  try {
    loading.value = true;
    await Promise.all([
      fetchMetaData(),
      fetchRecords()
    ]);
  } catch (error) {
    console.error('刷新数据失败:', error);
    emitter.emit('notify', '刷新失败：' + (error.message || '请稍后重试'), 'error');
  } finally {
    loading.value = false;
  }
}

// 生命周期钩子
onMounted(async () => {
  if (!isLoggedIn.value) {
    const { showLogin } = useAuth();
    showLogin('请先登录');
    return;
  }
  
  await refreshData();
});

// 事件处理函数
function handleAdd() {
  console.log('点击添加按钮');
  if (!checkAuthAndExecute(async () => { 
    console.log('执行添加操作');
    resetForm();
    showAddModal.value = true;
  })) return;
  console.log('添加操作执行完成');
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
}

function editRecord(idx) {
  const record = records.value[idx];
  if (!record || !checkAuthAndExecute(() => startEdit(idx, record))) return;
}

async function saveEdit() {
  if (editingIdx.value === null || !editForm.finishTime) return;
  
  const record = records.value[editingIdx.value];
  if (!record) return;
  
  if (!checkAuthAndExecute(async () => {
    try {
      if (await updateRecord({ ...editForm, id: record.id })) {
        cancelEdit();
        emitter.emit('notify', '更新成功', 'success');
      }
    } catch (error) {
      emitter.emit('notify', '更新失败：' + (error.message || '请重试'), 'error');
    }
  })) return;
}

async function handleDelete(idx) {
  const record = records.value[idx];
  if (!record || !checkAuthAndExecute(async () => {
    try {
      const success = await deleteRecord(record.id);
      if (success) {
        emitter.emit('notify', '删除成功', 'success');
        await fetchRecords();
      }
    } catch (error) {
      console.error('删除记录失败：', error);
      emitter.emit('notify', '删除失败：' + (error.message || '请重试'), 'error');
    }
  })) return;
}

function handleQuery() {
  const currentQuery = { ...query };
  console.log('执行查询，当前查询条件：', currentQuery);
  if (Object.values(currentQuery).some(val => val !== '')) {
    fetchRecords(1);
  }
}

// 清理工作
onUnmounted(() => {
  emitter.off('refresh-data');
});
</script>
