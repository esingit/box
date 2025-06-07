<template>
  <form class="fitness-form">
    <div class="form-group">
      <label>类型：</label>
      <select v-model="form.typeId" class="form-control">
        <option v-for="type in types" 
                :key="type.id" 
                :value="type.id">
          {{ type.name }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label>数量：</label>
      <input type="number" 
             v-model.number="form.count" 
             min="1" 
             class="form-control" 
             placeholder="请输入数量" />
    </div>

    <div class="form-group">
      <label>单位：</label>
      <select v-model="form.unitId" class="form-control">
        <option v-for="unit in units" 
                :key="unit.id" 
                :value="unit.id">
          {{ unit.name }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label>完成日期：</label>
      <input type="date" 
             v-model="form.finishTime" 
             class="form-control" 
             required />
    </div>

    <div class="form-group">
      <label>备注：</label>
      <input type="text" 
             v-model="form.remark" 
             class="form-control" 
             :placeholder="remarkPlaceholder" />
    </div>
  </form>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  form: {
    type: Object,
    required: true
  },
  types: {
    type: Array,
    default: () => []
  },
  units: {
    type: Array,
    default: () => []
  },
  remarkPlaceholder: {
    type: String,
    default: '请输入备注（可选）'
  }
});

const isValid = computed(() => {
  return props.form && 
         props.form.typeId && 
         props.form.count > 0 && 
         props.form.unitId && 
         props.form.finishTime;
});

defineExpose({
  isValid
});
</script>

<style scoped>
.fitness-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: #333;
}

.form-control {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-control:focus {
  outline: none;
  border-color: #0066cc;
  box-shadow: 0 0 0 2px rgba(0,102,204,0.2);
}

select.form-control {
  background-color: white;
}

input[type="number"].form-control {
  width: 100%;
}

input[type="date"].form-control {
  width: 100%;
}
</style>
