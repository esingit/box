<template>
  <Form
      ref="formRef"
      :validation-schema="schema"
      :initial-values="form"
      @submit="handleSubmit"
      class="space-y-4"
      v-slot="{ values, setFieldValue, errors, meta }"
  >
    <!-- 类型 -->
    <div>
      <label class="form-label">类型</label>
      <Field
          as="select"
          name="typeId"
          class="form-select"
          @change="e => onTypeChange(e, setFieldValue, values)"
      >
        <option value="" disabled>请选择</option>
        <option v-for="type in fitnessTypes" :key="type.id" :value="type.id">{{ type.value1 }}</option>
      </Field>
      <ErrorMessage name="typeId" class="select" />
    </div>

    <!-- 次数 -->
    <div>
      <label class="form-label">次数</label>
      <Field name="count" type="number" class="form-input" min="1" />
      <ErrorMessage name="count" class="base-input" />
    </div>

    <!-- 单位 -->
    <div>
      <label class="form-label">单位</label>
      <Field as="select" name="unitId" class="form-select">
        <option value="" disabled>请选择</option>
        <option v-for="unit in units" :key="unit.id" :value="unit.id">{{ unit.value1 }}</option>
      </Field>
      <ErrorMessage name="unitId" class="select" />
    </div>

    <!-- 完成时间 -->
    <div>
      <label class="form-label">完成时间</label>
      <Field name="finishTime" type="date" class="form-input" />
      <ErrorMessage name="finishTime" class="select" />
    </div>

    <!-- 备注 -->
    <div>
      <label class="form-label">备注</label>
      <Field
          as="textarea"
          name="remark"
          :placeholder="remarkPlaceholder"
          class="form-textarea"
          rows="3"
      />
    </div>
  </Form>
</template>

<script setup lang="ts">
import { Form, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { ref, watch, computed, onMounted, defineExpose } from 'vue'
import type { FormContextType } from 'vee-validate'
import { useMetaStore } from '@/store/metaStore'

const props = defineProps<{
  form: {
    typeId?: string
    count?: number
    unitId?: string
    finishTime?: string
    remark?: string
  }
  remarkPlaceholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:form', val: any): void
  (e: 'submit', val: any): void
}>()

const metaStore = useMetaStore()
const fitnessTypes = computed(() => metaStore.typeMap?.FITNESS_TYPE || [])
const units = computed(() => metaStore.typeMap?.UNIT || [])

const form = ref({ ...props.form })
const formRef = ref<FormContextType | null>(null)

const schema = yup.object({
  typeId: yup.string().required('请选择类型'),
  count: yup
      .number()
      .typeError('请输入次数') // 防止为空时类型错误
      .required('请输入次数')
      .min(1, '次数不能小于1'),
  unitId: yup.string().required('请选择单位'),
  finishTime: yup.string().required('请输入完成时间'),
  remark: yup.string().nullable(), // 可选
})


// props.form 改变时：格式化 finishTime 并重置表单
watch(
    () => props.form,
    val => {
      form.value = { ...val }
      if (form.value.finishTime && form.value.finishTime.length > 10) {
        // 截取成 YYYY-MM-DD，保证 date 输入框能识别
        form.value.finishTime = form.value.finishTime.slice(0, 10)
      }
      // resetForm 重新初始化所有字段
      if (formRef.value) {
        formRef.value.resetForm({ values: form.value })
      }
    },
    { immediate: true, deep: true }
)

onMounted(() => {
  if (form.value.typeId) {
    setDefaultUnit(form.value.typeId)
  }
})

function onTypeChange(event: Event, setFieldValue: any, values: any) {
  const newTypeId = (event.target as HTMLSelectElement).value
  setFieldValue('typeId', newTypeId)
  setDefaultUnit(newTypeId, setFieldValue, values)
}

function setDefaultUnit(typeId: string, setFieldValue?: any, values?: any) {
  const selectedType = fitnessTypes.value.find(type => type.id === typeId)
  if (selectedType?.key3) {
    const defaultUnit = units.value.find(unit => unit.key1 === selectedType.key3)
    if (defaultUnit) {
      const currentUnitId = values?.unitId || form.value.unitId
      if (!currentUnitId || currentUnitId !== defaultUnit.id) {
        if (setFieldValue) {
          setFieldValue('unitId', defaultUnit.id)
        } else {
          form.value.unitId = defaultUnit.id
        }
      }
    }
  } else {
    if (setFieldValue) {
      setFieldValue('unitId', '')
    } else {
      form.value.unitId = ''
    }
  }
}

function handleSubmit(values: any) {
  emit('update:form', values)
  emit('submit', values)
}

// 暴露外部提交方法
async function submitForm() {
  try {
    if (formRef.value) {
      // @ts-ignore
      const formElement = formRef.value.$el || (formRef.value as any)
      if (formElement && typeof formElement.requestSubmit === 'function') {
        formElement.requestSubmit()
      } else {
        const evt = new Event('submit', { bubbles: true, cancelable: true })
        formElement.dispatchEvent(evt)
      }
    }
  } catch (error) {
    console.error('表单提交失败:', error)
  }
}

defineExpose({ submit: submitForm })
</script>
