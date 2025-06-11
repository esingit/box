<template>
  <form>
    <div class="form-group">
      <label class="input-label">
        资产名称
        <span class="required">*</span>
      </label>
      <div class="control-group">
        <select id="assetName" v-model="form.assetNameId" class="select form-select" required>
          <option value="">请选择资产名称</option>
          <option v-for="name in assetNames" :key="name.id" :value="name.id">
            {{ name.name }}
          </option>
        </select>
        <button class="btn btn-outline control-action" @click.prevent="$emit('maintain')">
          <LucideSettings size="16" class="btn-icon" />
          维护
        </button>
      </div>
    </div>

    <div class="form-group">
      <label class="input-label">
        资产分类
        <span class="required">*</span>
      </label>
      <select id="assetType" v-model="form.assetTypeId" class="select form-select" required>
        <option value="">请选择资产分类</option>
        <option v-for="type in types" :key="type.id" :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">
        资产位置
        <span class="required">*</span>
      </label>
      <select id="location" v-model="form.assetLocationId" class="select form-select" required>
        <option value="">请选择资产位置</option>
        <option v-for="location in locations" :key="location.id" :value="location.id">
          {{ location.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">
        金额
        <span class="required">*</span>
      </label>
      <input
        id="amount"
        v-model="form.amount"
        class="amount-input input"
        type="number"
        step="0.01"
        min="0"
        required
        placeholder="请输入金额"
      />
    </div>

    <div class="form-group">
      <label class="input-label">
        货币单位
        <span class="required">*</span>
      </label>
      <select id="unit" v-model="form.unitId" class="form-select select" required>
        <option value="">请选择货币单位</option>
        <option v-for="unit in units" :key="unit.id" :value="unit.id">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">
        日期
        <span class="required">*</span>
      </label>
      <input id="date" v-model="form.acquireTime" type="date" class="date-input input" required />
    </div>

    <div class="form-group">
      <label class="input-label">备注</label>
      <input 
        id="remark" 
        v-model="form.remark" 
        type="text" 
        class="remark-input input" 
        :placeholder="remarkPlaceholder" 
      />
    </div>
  </form>
</template>

<script setup>
import { computed } from 'vue'
import { LucideSettings } from 'lucide-vue-next'

const props = defineProps({
  form: {
    type: Object,
    required: true,
    validator: (value) => {
      return value && typeof value === 'object'
    }
  },
  assetNames: {
    type: Array,
    required: true
  },
  types: {
    type: Array,
    required: true
  },
  units: {
    type: Array,
    required: true
  },
  locations: {
    type: Array,
    required: true
  },
  remarkPlaceholder: {
    type: String,
    default: '备注'
  }
})

defineEmits(['maintain'])

// 表单字段验证
const fieldValidation = computed(() => ({
  assetNameId: Boolean(props.form.assetNameId),
  assetTypeId: Boolean(props.form.assetTypeId),
  assetLocationId: Boolean(props.form.assetLocationId),
  amount: props.form.amount > 0,
  unitId: Boolean(props.form.unitId),
  acquireTime: Boolean(props.form.acquireTime)
}))

// 表单整体验证
const isValid = computed(() => 
  Object.values(fieldValidation.value).every(valid => valid)
)

defineExpose({
  isValid,
  fieldValidation
})
</script>
