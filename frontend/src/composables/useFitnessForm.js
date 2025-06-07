import { reactive, ref } from 'vue';

export function useFitnessForm(types, units) {
  // 工具函数
  function getTodayDate() {
    return new Date().toISOString().slice(0, 10);
  }

  // 初始表单状态
  const initialFormState = {
    typeId: null,
    count: 1,
    unitId: null,
    finishTime: getTodayDate(),
    remark: ''
  };

  // 表单数据
  const form = reactive({ ...initialFormState });
  const editForm = reactive({ ...initialFormState });
  
  // 表单状态
  const adding = ref(false);
  const editingIdx = ref(null);
  const showAddModal = ref(false);

  // 重置表单
  function resetForm(formData) {
    Object.assign(formData, initialFormState);
    if (types.value.length > 0) formData.typeId = types.value[0].id;
    if (units.value.length > 0) formData.unitId = units.value[0].id;
    formData.finishTime = getTodayDate();
  }

  // 处理编辑开始
  function startEdit(idx, record) {
    editingIdx.value = idx;
    Object.assign(editForm, {
      typeId: record.typeId,
      count: record.count,
      unitId: record.unitId,
      finishTime: record.finishTime?.slice(0, 10) || getTodayDate(),
      remark: record.remark || ''
    });
  }

  // 取消编辑
  function cancelEdit() {
    editingIdx.value = null;
    resetForm(editForm);
  }

  // 关闭添加模态框
  function closeAddModal() {
    showAddModal.value = false;
    resetForm(form);
  }

  return {
    form,
    editForm,
    adding,
    editingIdx,
    showAddModal,
    resetForm,
    startEdit,
    cancelEdit,
    closeAddModal,
    getTodayDate
  };
}
