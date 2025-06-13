<template>
  <form @submit.prevent="onSubmit">
    <div class="form-group">
      <label class="input-label">资产名称<span class="required">*</span></label>
      <div class="control-group">
        <select
            v-model="form.assetNameId"
            class="select form-select"
            required
            @keydown.enter.prevent="handleEnter"
        >
          <option value="">请选择资产名称</option>
          <option v-for="name in assetNames" :key="name.id" :value="name.id">
            {{ name.name }}
          </option>
        </select>
        <button
            type="button"
            class="btn btn-outline control-action"
            @click.prevent="emitMaintain"
        >
          <LucideSettings size="16" class="btn-icon" />
          维护
        </button>
      </div>
    </div>

    <div class="form-group">
      <label class="input-label">资产分类<span class="required">*</span></label>
      <select
          v-model="form.assetTypeId"
          class="select form-select"
          required
          @change="onAssetTypeChange"
          @keydown.enter.prevent="handleEnter"
      >
        <option value="">请选择资产分类</option>
        <option v-for="type in types" :key="type.id" :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">资产位置<span class="required">*</span></label>
      <select
          v-model="form.assetLocationId"
          class="select form-select"
          required
          @keydown.enter.prevent="handleEnter"
      >
        <option value="">请选择资产位置</option>
        <option v-for="location in locations" :key="location.id" :value="location.id">
          {{ location.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">金额<span class="required">*</span></label>
      <input
          v-model.number="form.amount"
          class="amount-input input"
          type="number"
          step="0.01"
          min="0"
          required
          placeholder="请输入金额"
          @keydown.enter.prevent="handleEnter"
      />
    </div>

    <div class="form-group">
      <label class="input-label">货币单位<span class="required">*</span></label>
      <select
          v-model="form.unitId"
          class="form-select select"
          required
          @keydown.enter.prevent="handleEnter"
      >
        <option value="">请选择货币单位</option>
        <option v-for="unit in filteredUnits" :key="unit.id" :value="String(unit.id)">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">日期<span class="required">*</span></label>
      <input
          v-model="form.acquireTime"
          type="date"
          class="date-input input"
          required
          @keydown.enter.prevent="handleEnter"
      />
    </div>

    <div class="form-group">
      <label class="input-label">备注</label>
      <input
          v-model="form.remark"
          type="text"
          class="remark-input input"
          :placeholder="remarkPlaceholder"
          @keydown.enter.prevent="handleEnter"
      />
    </div>
  </form>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { LucideSettings } from 'lucide-vue-next'

const props = defineProps({
  form: { type: Object, required: true },
  assetNames: { type: Array, required: true },
  types: { type: Array, required: true },
  units: { type: Array, required: true },
  locations: { type: Array, required: true },
  remarkPlaceholder: { type: String, default: '备注' }
})

const emit = defineEmits(['maintain', 'confirm'])

const emitConfirm = () => emit('confirm')
const emitMaintain = () => emit('maintain')

const onSubmit = () => {
  emitConfirm()
}

onMounted(() => {
  onAssetTypeChange()
})

const filteredUnits = computed(() => {
  const selected = props.types.find(t => t.id === props.form.assetTypeId)
  return selected?.key3
      ? props.units.filter(unit => unit.key1 === selected.key3)
      : props.units
})

const onAssetTypeChange = () => {
  const selected = props.types.find(t => t.id === props.form.assetTypeId)
  const match = selected?.key3
      ? props.units.find(unit => unit.key1 === selected.key3)
      : null
  props.form.unitId = match?.id || ''
}

const handleEnter = (e) => {
  e.preventDefault()
  const formElements = Array.from(e.target.form.querySelectorAll('select, input'))
      .filter(el => !el.disabled && el.offsetParent !== null)
  const index = formElements.indexOf(e.target)
  if (index > -1 && index < formElements.length - 1) {
    formElements[index + 1].focus()
  } else {
    // 如果是最后一个元素，尝试找到提交按钮
    const submitButton = e.target.form.querySelector('.modal-footer .btn-primary')
    if (submitButton) {
      submitButton.focus()
    } else {
      // 如果没有找到提交按钮，触发 confirm 事件
      emitConfirm()
      e.target.blur()
    }
  }
}

const fieldValidation = computed(() => ({
  assetNameId: !!props.form.assetNameId,
  assetTypeId: !!props.form.assetTypeId,
  assetLocationId: !!props.form.assetLocationId,
  amount: props.form.amount > 0,
  unitId: !!props.form.unitId,
  acquireTime: !!props.form.acquireTime
}))

const isValid = computed(() =>
    Object.values(fieldValidation.value).every(Boolean)
)

defineExpose({ isValid, fieldValidation })
</script>
