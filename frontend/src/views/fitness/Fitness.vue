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
            <LucideRefreshCw class="btn-icon-md"/>
          </button>
        </div>
      </div>

      <div class="card-grid">
        <template v-if="loading">
          <SkeletonCard v-for="i in 3" :key="i" class="stat-card"/>
        </template>
        <template v-else>
          <div class="stat-card">
            <h4 class="stat-label">本月运动</h4>
            <div class="stat-value text-primary">{{ stats.monthlyCount || 0 }}次</div>
            <div class="u-flex-center">本周运动{{ stats.weeklyCount || 0 }}次</div>
          </div>
          <div class="stat-card">
            <h4 class="stat-label">上次运动</h4>
            <div :class="['stat-value', {'text-error': isWorkoutOverdue}]">{{ stats.lastWorkoutDays || 0 }}天前</div>
            <div
                class="text-hint"
                :style="{ color: isNextWorkoutOverdue ? 'var(--error)' : '' }"
            >
              下次运动日{{ stats.nextWorkoutDay || '-' }}
            </div>
          </div>
          <div class="stat-card">
            <h4 class="stat-label">今日蛋白</h4>
            <div :class="['stat-value', {'text-success': stats.proteinIntake >= 80}]">
              {{ stats.proteinIntake || 0 }}克
              <span
                  class="text-hint"
                  :style="{ color: stats.proteinIntake < 80 ? 'var(--warning)' : 'var(--success)' }"
              >
                {{ stats.proteinIntake >= 80 ? '✓' : `差${80 - (stats.proteinIntake || 0)}克` }}
              </span>
            </div>
            <div class="text-hint">
              今日碳水{{ stats.carbsIntake || 0 }}克
              <span v-if="stats.carbsIntake < 120" class="text-warning">
                差{{ 120 - (stats.carbsIntake || 0) }}克
              </span>
              <span v-else class="text-success">✓</span>
            </div>
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
          <LucidePlus class="btn-icon-sm"/>
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
        <SkeletonCard v-for="n in pageSize" :key="n"/>
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
import {onMounted, onUnmounted, ref, watch, computed} from 'vue';
import {LucidePlus, LucideRefreshCw} from 'lucide-vue-next';
import {useUserStore} from '@/stores/userStore';
import emitter from '@/utils/eventBus.js';
import {useMetaData} from '@/composables/useMetaData';
import {useFitnessRecords} from '@/composables/useFitnessRecords';
import {useFitnessForm} from '@/composables/useFitnessForm';
import FitnessModal from '@/components/fitness/FitnessModal.vue';
import FitnessList from '@/components/fitness/FitnessList.vue';
import SearchPanel from '@/components/fitness/SearchPanel.vue';
import SkeletonCard from '@/components/common/SkeletonCard.vue';

// Store
const userStore = useUserStore();

// 统计数据
const stats = ref({
  monthlyCount: 0,
  lastWorkoutDays: 0,
  totalCount: 0,
  weeklyCount: 0,
  nextWorkoutDay: '-',
  carbsIntake: 0,
  proteinIntake: 0
});

// 组合式函数
const {types, units, fetchMetaData} = useMetaData();
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
  handlePageSizeChange,
  fetchStats
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
}, {immediate: true});

// 刷新数据
async function refreshData() {
  try {
    loading.value = true;
    const [metaData, recordsData, statsData] = await Promise.all([
      fetchMetaData(),
      fetchRecords(),
      fetchStats()
    ]);

    if (statsData) {
      stats.value = statsData;
    }
  } catch (error) {
    console.error('刷新数据失败:', error);
    emitter.emit('notify', '刷新失败：' + (error.message || '请稍后重试'), 'error');
  } finally {
    loading.value = false;
  }
}

// 生命周期钩子
onMounted(refreshData);

// 事件处理函数
function handleAdd() {
  resetForm();
  showAddModal.value = true;
}

async function handleAddRecord() {
  try {
    adding.value = true;
    if (await addRecord(form)) {
      closeAddModal();
      emitter.emit('notify', '添加成功', 'success');
      // 更新统计数据
      const statsData = await fetchStats();
      if (statsData) {
        stats.value = statsData;
      }
    }
  } finally {
    adding.value = false;
  }
}

function editRecord(idx) {
  const record = records.value[idx];
  if (record) {
    startEdit(idx, record);
  }
}

async function saveEdit() {
  if (editingIdx.value === null || !editForm.finishTime) return;

  const record = records.value[editingIdx.value];
  if (!record) return;

  try {
    if (await updateRecord({...editForm, id: record.id})) {
      cancelEdit();
      emitter.emit('notify', '更新成功', 'success');
      // 更新统计数据
      const statsData = await fetchStats();
      if (statsData) {
        stats.value = statsData;
      }
    }
  } catch (error) {
    emitter.emit('notify', '更新失败：' + (error.message || '请重试'), 'error');
  }
}

async function handleDelete(idx) {
  const record = records.value[idx];
  if (!record) return;

  try {
    const success = await deleteRecord(record.id);
    if (success) {
      emitter.emit('notify', '删除成功', 'success');
      await fetchRecords();
      // 更新统计数据
      const statsData = await fetchStats();
      if (statsData) {
        stats.value = statsData;
      }
    }
  } catch (error) {
    console.error('删除记录失败：', error);
    emitter.emit('notify', '删除失败：' + (error.message || '请重试'), 'error');
  }
}

function handleQuery() {
  // 只要点击查询就给出提示，无论条件是否为空
  fetchRecords(1).then(() => {
    if (Number(total.value) === 0) {
      emitter.emit('notify', {message: '未找到匹配的记录', type: 'info'});
    } else {
      emitter.emit('notify', {message: `查询到 ${total.value} 条记录`, type: 'success'});
    }
  }).catch(error => {
    emitter.emit('notify', {message: '查询失败：' + (error.message || '未知错误'), type: 'error'});
  });
}

// 计算属性：判断是否已超过运动日期但今天没有运动记录
const isWorkoutOverdue = computed(() => {
  if (!stats.value.nextWorkoutDay || stats.value.nextWorkoutDay === '-') return false;
  const today = new Date().toISOString().split('T')[0];
  return stats.value.nextWorkoutDay === today && stats.value.lastWorkoutDays > 0;
});

const isNextWorkoutOverdue = computed(() => {
  if (!stats.value.nextWorkoutDay || stats.value.nextWorkoutDay === '-') return false;
  const today = new Date().toISOString().split('T')[0];
  return stats.value.nextWorkoutDay < today; // 日期已过
});



// 清理工作
onUnmounted(() => {
  emitter.off('refresh-data');
});
</script>


