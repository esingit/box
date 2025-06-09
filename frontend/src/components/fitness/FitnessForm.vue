<template>
  <form>
    <div class="form-group">
      <label class="input-label">
        类型
        <span class="required">*</span>
      </label>
      <select v-model="form.typeId" class="select" required>
        <option value="">请选择类型</option>
        <option v-for="type in types" 
                :key="type.id" 
                :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">
        数量
        <span class="required">*</span>
      </label>
      <input type="number" 
             v-model.number="form.count" 
             min="1" 
             class="input" 
             required
             placeholder="请输入数量" />
    </div>

    <div class="form-group">
      <label class="input-label">
        单位
        <span class="required">*</span>
      </label>
      <select v-model="form.unitId" class="select" required>
        <option value="">请选择单位</option>
        <option v-for="unit in units" 
                :key="unit.id" 
                :value="unit.id">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">
        完成日期
        <span class="required">*</span>
      </label>
      <input type="date" 
             v-model="form.finishTime" 
             class="input" 
             required />
    </div>

    <div class="form-group">
      <label class="input-label">备注</label>
      <input type="text" 
             v-model="form.remark" 
             class="input" 
             :placeholder="remarkPlaceholder" />
    </div>
  </form>
</template>

<script setup>
import { computed } from 'vue';

// 定义表单项的类型
const props = defineProps({
  form: {
    type: Object,
    required: true,
    validator: (value) => {
      return value && typeof value === 'object';
    }
  },
  types: {
    type: Array,
    default: () => [],
    validator: (value) => {
      return value.every(item => 
        item && typeof item.id !== 'undefined' && typeof item.value1 === 'string'
      );
    }
  },
  units: {
    type: Array,
    default: () => [],
    validator: (value) => {
      return value.every(item => 
        item && typeof item.id !== 'undefined' && typeof item.value1 === 'string'
      );
    }
  },
  remarkPlaceholder: {
    type: String,
    default: '请输入备注（可选）'
  }
});

// 表单字段验证
const fieldValidation = computed(() => ({
  typeId: Boolean(props.form.typeId),
  count: props.form.count > 0,
  unitId: Boolean(props.form.unitId),
  finishTime: Boolean(props.form.finishTime)
}));

// 表单整体验证
const isValid = computed(() => {
  if (!props.form) return false;
  
  return Object.values(fieldValidation.value).every(valid => valid);
});

// 对外暴露的接口
defineExpose({
  isValid,
  fieldValidation
});
</script>