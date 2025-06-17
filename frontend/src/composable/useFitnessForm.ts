import { ref, reactive, Ref } from 'vue';

interface FitnessType {
  id: number | string;
  name?: string;
}

interface FitnessUnit {
  id: number | string;
  name?: string;
}

interface FitnessForm {
  typeId: number | string | null;
  count: number;
  unitId: number | string | null;
  finishTime: string;
  remark: string;
}

export function useFitnessForm(
    types: Ref<FitnessType[]>,
    units: Ref<FitnessUnit[]>
) {
  function getTodayDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  const initialFormState: FitnessForm = {
    typeId: null,
    count: 1,
    unitId: null,
    finishTime: getTodayDate(),
    remark: ''
  };

  const form = reactive<FitnessForm>({ ...initialFormState });
  const editForm = reactive<FitnessForm>({ ...initialFormState });

  const adding = ref(false);
  const editingIdx = ref<number | null>(null);
  const showAddModal = ref(false);

  function resetForm(formData: FitnessForm) {
    if (!formData) {
      console.warn('重置表单时收到空的表单对象');
      return;
    }
    Object.assign(formData, { ...initialFormState });
    if (types.value.length > 0) formData.typeId = types.value[0].id;
    if (units.value.length > 0) formData.unitId = units.value[0].id;
    formData.finishTime = getTodayDate();
  }

  function startEdit(idx: number, record: Partial<FitnessForm>) {
    editingIdx.value = idx;
    Object.assign(editForm, {
      typeId: record.typeId ?? null,
      count: record.count ?? 1,
      unitId: record.unitId ?? null,
      finishTime: record.finishTime?.slice(0, 10) || getTodayDate(),
      remark: record.remark || ''
    });
  }

  function cancelEdit() {
    editingIdx.value = null;
    resetForm(editForm);
  }

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
    closeAddModal
  };
}
