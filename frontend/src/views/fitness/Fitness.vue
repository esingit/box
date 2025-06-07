<template>
  <div class="page-container">
    <!-- 页面标题 -->
    <PageHeader title="记录" :icon="ClipboardListIcon" />
    
    <!-- 查询区域 -->
    <SearchPanel
      v-model:query="query"
      :types="types"
      @search="handleQuery"
      @reset="resetQuery"
    />
    
    <!-- 操作按钮区域 -->
    <div class="action-bar">
      <button class="btn btn-danger" @click="handleAdd">
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
import { LucideClipboardList } from 'lucide-vue-next';
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

// 常量和组件属性
const ClipboardListIcon = LucideClipboardList;

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
  // 初始化数据
  await Promise.all([
    fetchMetaData(),
    fetchRecords()
  ]);

  // 监听全局刷新事件
  emitter.on('refresh-data', () => {
    Promise.all([
      fetchMetaData(),
      fetchRecords()
    ]);
  });
});

// 在组件销毁时移除事件监听
onUnmounted(() => {
  emitter.off('refresh-data');
});

// 事件处理函数
// 保存操作类型和参数
const pendingAction = ref(null);
const pendingActionParams = ref(null);

// 检查登录状态并执行操作
function checkAuthAndExecute(action, params = null) {
  if (!isLoggedIn.value) {
    pendingAction.value = action;
    pendingActionParams.value = params;
    const { showLogin } = useAuth();
    showLogin('请先登录', () => {
      if (pendingAction.value) {
        const actionToExecute = pendingAction.value;
        const actionParams = pendingActionParams.value;
        pendingAction.value = null;
        pendingActionParams.value = null;
        actionToExecute(actionParams);
      }
    });
    return false;
  }
  return true;
}

function handleAdd() {
  if (checkAuthAndExecute(() => showAddModal.value = true)) {
    showAddModal.value = true;
  }
}

async function handleAddRecord() {
  if (!checkAuthAndExecute(handleAddRecord)) return;
  
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
  if (!checkAuthAndExecute(() => editRecord(idx))) return;
  
  const record = records.value[idx];
  if (record) {
    startEdit(idx, record);
  }
}

async function saveEdit() {
  if (!checkAuthAndExecute(saveEdit)) return;
  
  if (editingIdx.value !== null && editForm.finishTime) {
    const record = records.value[editingIdx.value];
    if (await updateRecord({ ...editForm, id: record.id })) {
      cancelEdit();
    }
  }
}

async function handleDelete(idx) {
  const record = records.value[idx];
  if (!record) return;
  
  if (!checkAuthAndExecute(() => handleDelete(idx))) return;

  emitter.emit('confirm', {
    title: '删除确认',
    message: '确定要删除该记录吗？',
    onConfirm: () => deleteRecord(record.id)
  });
}

function handleQuery() {
  fetchRecords();
}
</script>
