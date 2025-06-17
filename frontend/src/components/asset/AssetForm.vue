<template>
  <form @submit.prevent="onSubmit" class="space-y-6">
    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        资产名称<span class="text-red-500">*</span>
      </label>
      <div class="flex items-center space-x-2 mt-1">
        <select
            v-model="form.assetNameId"
            required
            @keydown.enter.prevent="handleEnter"
            class="flex-grow block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
                 py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
                 text-gray-900 dark:text-gray-100"
        >
          <option value="">请选择资产名称</option>
          <option v-for="name in assetNames" :key="name.id" :value="name.id">
            {{ name.name }}
          </option>
        </select>
        <button
            type="button"
            class="inline-flex items-center px-3 py-2 border border-indigo-600 text-indigo-600 rounded-md
                 hover:bg-indigo-50 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            @click.prevent="emitMaintain"
        >
          <LucideSettings size="16" class="mr-1" />
          名称管理
        </button>
      </div>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        资产分类<span class="text-red-500">*</span>
      </label>
      <select
          v-model="form.assetTypeId"
          required
          @change="onAssetTypeChange"
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
      >
        <option value="">请选择资产分类</option>
        <option v-for="type in types" :key="type.id" :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        资产位置<span class="text-red-500">*</span>
      </label>
      <select
          v-model="form.assetLocationId"
          required
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
      >
        <option value="">请选择资产位置</option>
        <option v-for="location in locations" :key="location.id" :value="location.id">
          {{ location.value1 }}
        </option>
      </select>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        金额<span class="text-red-500">*</span>
      </label>
      <input
          v-model.number="form.amount"
          type="number"
          min="0"
          step="0.01"
          required
          placeholder="请输入金额"
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
      />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        货币单位<span class="text-red-500">*</span>
      </label>
      <select
          v-model="form.unitId"
          required
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
      >
        <option value="">请选择货币单位</option>
        <option v-for="unit in filteredUnits" :key="unit.id" :value="String(unit.id)">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">
        日期<span class="text-red-500">*</span>
      </label>
      <input
          v-model="form.acquireTime"
          type="date"
          required
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
      />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 dark:text-gray-300">备注</label>
      <input
          v-model="form.remark"
          type="text"
          :placeholder="remarkPlaceholder"
          @keydown.enter.prevent="handleEnter"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white dark:bg-gray-800 dark:border-gray-600
               py-2 px-3 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500
               text-gray-900 dark:text-gray-100"
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
