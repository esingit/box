<template>
  <form class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-gray-700">
        类型 <span class="text-red-500">*</span>
      </label>
      <select
          v-model="form.typeId"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white py-2 px-3
               shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-300 focus:ring-opacity-50"
          required
          @change="onTypeChange"
          @keydown.enter.prevent="moveCursorToNextLine"
      >
        <option value="">请选择类型</option>
        <option v-for="type in types" :key="type.id" :value="type.id">
          {{ type.value1 }}
        </option>
      </select>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700">
        数量 <span class="text-red-500">*</span>
      </label>
      <input
          type="number"
          v-model.number="form.count"
          min="1"
          class="mt-1 block w-full rounded-md border border-gray-300 py-2 px-3
               shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-300 focus:ring-opacity-50"
          required
          placeholder="请输入数量"
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700">
        单位 <span class="text-red-500">*</span>
      </label>
      <select
          v-model="form.unitId"
          class="mt-1 block w-full rounded-md border border-gray-300 bg-white py-2 px-3
               shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-300 focus:ring-opacity-50"
          required
          @keydown.enter.prevent="moveCursorToNextLine"
      >
        <option value="">请选择单位</option>
        <option v-for="unit in filteredUnits" :key="unit.id" :value="String(unit.id)">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700">
        完成日期 <span class="text-red-500">*</span>
      </label>
      <input
          type="date"
          v-model="form.finishTime"
          class="mt-1 block w-full rounded-md border border-gray-300 py-2 px-3
               shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-300 focus:ring-opacity-50"
          required
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700">
        备注
      </label>
      <input
          type="text"
          v-model="form.remark"
          :placeholder="remarkPlaceholder"
          class="mt-1 block w-full rounded-md border border-gray-300 py-2 px-3
               shadow-sm focus:border-blue-500 focus:ring focus:ring-blue-300 focus:ring-opacity-50"
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>
  </form>
</template>

<script setup>
import { reactive, computed, onMounted, nextTick } from 'vue'

const props = defineProps({
  // 如果你需要传入form，打开下面这行
  // form: { type: Object, default: null },
  types: { type: Array, default: () => [] },
  units: { type: Array, default: () => [] },
  remarkPlaceholder: { type: String, default: '请输入备注（可选）' }
})

// 本地初始化form，避免undefined报错
const form = reactive({
  typeId: '',
  count: 1,
  unitId: '',
  finishTime: '',
  remark: ''
})

// 如果你需要传入form，请用下面代码替换上面初始化
// const form = reactive(props.form || {
//   typeId: '',
//   count: 1,
//   unitId: '',
//   finishTime: '',
//   remark: ''
// })

const filteredUnits = computed(() => {
  const selected = props.types.find(t => String(t.id) === String(form.typeId))
  return selected?.key3
      ? props.units.filter(unit => unit.key1 === selected.key3)
      : props.units
})

const onTypeChange = () => {
  const selected = props.types.find(t => String(t.id) === String(form.typeId))
  const match = selected?.key3
      ? props.units.find(unit => unit.key1 === selected.key3)
      : null
  form.unitId = match?.id || ''
}

onMounted(onTypeChange)

const isValid = computed(() =>
    form.typeId &&
    form.unitId &&
    form.count > 0 &&
    !!form.finishTime
)

defineExpose({ isValid })

const moveCursorToNextLine = (event) => {
  event.preventDefault()
  nextTick(() => {
    const formElements = event.target.form.elements
    const index = Array.from(formElements).indexOf(event.target)
    for (let i = index + 1; i < formElements.length; i++) {
      const el = formElements[i]
      if (
          ['INPUT', 'SELECT', 'TEXTAREA'].includes(el.tagName) &&
          !el.disabled &&
          el.type !== 'hidden'
      ) {
        el.focus()
        if (el.select) el.select()
        break
      }
    }
  })
}
</script>
