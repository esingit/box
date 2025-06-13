<template>
  <form>
    <div class="form-group">
      <label class="input-label">类型<span class="required">*</span></label>
      <select
          v-model="form.typeId"
          class="select form-select"
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

    <div class="form-group">
      <label class="input-label">数量<span class="required">*</span></label>
      <input
          type="number"
          v-model.number="form.count"
          min="1"
          class="input"
          required
          placeholder="请输入数量"
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>

    <div class="form-group">
      <label class="input-label">单位<span class="required">*</span></label>
      <select
          v-model="form.unitId"
          class="select form-select"
          required
          @keydown.enter.prevent="moveCursorToNextLine"
      >
        <option value="">请选择单位</option>
        <option v-for="unit in filteredUnits" :key="unit.id" :value="String(unit.id)">
          {{ unit.value1 }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label class="input-label">完成日期<span class="required">*</span></label>
      <input
          type="date"
          v-model="form.finishTime"
          class="input"
          required
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>

    <div class="form-group">
      <label class="input-label">备注</label>
      <input
          type="text"
          v-model="form.remark"
          class="input"
          :placeholder="remarkPlaceholder"
          @keydown.enter.prevent="moveCursorToNextLine"
      />
    </div>
  </form>
</template>

<script setup>
import { computed, onMounted, nextTick } from 'vue'

const props = defineProps({
  form: { type: Object, required: true },
  types: { type: Array, default: () => [] },
  units: { type: Array, default: () => [] },
  remarkPlaceholder: { type: String, default: '请输入备注（可选）' }
})

// 根据所选类型动态筛选单位
const filteredUnits = computed(() => {
  const selected = props.types.find(t => t.id === props.form.typeId)
  return selected?.key3
      ? props.units.filter(unit => unit.key1 === selected.key3)
      : props.units
})

// 类型改变时自动设置默认单位
const onTypeChange = () => {
  const selected = props.types.find(t => t.id === props.form.typeId)
  const match = selected?.key3
      ? props.units.find(unit => unit.key1 === selected.key3)
      : null
  props.form.unitId = match?.id || ''
}

onMounted(onTypeChange)

const isValid = computed(() =>
    props.form.typeId &&
    props.form.unitId &&
    props.form.count > 0 &&
    !!props.form.finishTime
)

defineExpose({ isValid })

// 回车后让光标跳到下一个可编辑控件
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
