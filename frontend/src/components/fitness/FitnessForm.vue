<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="590px"
      @update:visible="handleClose"
  >
    <Form
        ref="formRef"
        :validation-schema="schema"
        :initial-values="form"
        v-slot="{ handleSubmit, values, setFieldValue }"
    >
      <form @submit.prevent="handleSubmit(onSubmit)" class="space-y-3 px-1" id="fitness-form">
        <!-- 类型 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            健身类型 <span class="msg-error">*</span>
          </label>
          <Field name="typeId" v-slot="{ value, setValue }">
            <BaseSelect
                title="健身类型"
                :modelValue="value"
                clearable
                searchable
                :disabled="loading"
                :options="fitnessTypesFiltered"
                @update:modelValue="val => {
                setValue(val)
                setDefaultUnit(val as SingleValue, setFieldValue, values)
              }"
            />
          </Field>
          <ErrorMessage name="typeId" class="msg-error" />
        </div>

        <!-- 次数 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            次数 <span class="msg-error">*</span>
          </label>
          <Field name="count" v-slot="{ value, setValue }">
            <BaseInput
                :modelValue="value"
                @update:modelValue="setValue"
                title="数值"
                type="number"
                :min="1"
                :disabled="loading"
                required
                clearable
            />
          </Field>
          <ErrorMessage name="count" class="msg-error" />
        </div>

        <!-- 单位 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            单位 <span class="msg-error">*</span>
          </label>
          <Field name="unitId" v-slot="{ value, setValue }">
            <BaseSelect
                title="单位"
                :modelValue="value"
                clearable
                required
                searchable
                :disabled="loading"
                :options="unitsFiltered"
                @update:modelValue="setValue"
            />
          </Field>
          <ErrorMessage name="unitId" class="msg-error" />
        </div>

        <!-- 完成时间 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1">
            完成时间 <span class="msg-error">*</span>
          </label>
          <Field name="finishTime" v-slot="{ value, setValue }">
            <BaseDateInput
                :modelValue="value"
                @update:modelValue="setValue"
                title="完成时间"
                type="date"
                required
                clearable
                :disabled="loading"
                :max="today"
            />
          </Field>
          <ErrorMessage name="finishTime" class="msg-error" />
        </div>

        <!-- 备注 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1">
            备注
          </label>
          <Field name="remark" v-slot="{ value, setValue }">
            <BaseInput
                :modelValue="value"
                @update:modelValue="setValue"
                title="备注"
                type="textarea"
                clearable
                :disabled="loading"
            />
          </Field>
        </div>

        <!-- 底部按钮区域 -->
        <div class="flex justify-end gap-4">
          <BaseButton type="button" title="取消" @click="handleClose" color="outline" :loading="loading" />
          <BaseButton
              type="submit"
              color="primary"
              :loading="loading"
          >
            {{ loading ? '处理中...' : confirmText }}
          </BaseButton>
        </div>
      </form>
    </Form>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Form, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { useMetaStore } from '@/store/metaStore'
import { setDefaultUnit } from '@/utils/commonMeta'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseDateInput from "@/components/base/BaseDateInput.vue"
import {SingleValue} from "@/types/common";

const props = defineProps({
  visible: Boolean,
  form: Object,
  loading: Boolean,
  title: String,
  confirmText: String,
})

const emit = defineEmits(['close', 'submit', 'update:form'])

const formRef = ref()
const form = ref({ ...props.form })

const today = new Date().toISOString().slice(0, 10)  // 获取今天 yyyy-mm-dd 格式

const metaStore = useMetaStore()
const fitnessTypes = computed(() => metaStore.typeMap?.FITNESS_TYPE || [])
const units = computed(() => metaStore.typeMap?.UNIT || [])

const fitnessTypesFiltered = computed(() =>
    fitnessTypes.value
        .filter(t => t.value1 != null)
        .map(t => ({
          label: String(t.value1),
          value: t.id,
        }))
)

const unitsFiltered = computed(() =>
    units.value
        .filter(u => u.value1 != null)
        .map(u => ({
          label: String(u.value1),
          value: u.id,
        }))
)

const schema = yup.object({
  typeId: yup.string().required('请选择类型'),
  count: yup
      .number()
      .typeError('请输入次数')
      .required('请输入次数')
      .min(1, '次数不能小于1'),
  unitId: yup.string().required('请选择单位'),
  finishTime: yup
      .string()
      .required('请输入完成时间')
      .test('is-not-future', '完成时间不能大于今天', val => {
        if (!val) return false
        return val <= today
      }),
  remark: yup.string().nullable(),
})

watch(
    () => props.form,
    val => {
      form.value = { ...val }
      if (form.value.finishTime && form.value.finishTime.length > 10) {
        form.value.finishTime = form.value.finishTime.slice(0, 10)
      }
      formRef.value?.resetForm({ values: form.value })

      if (form.value.typeId) {
        setDefaultUnit(form.value.typeId)
      }
    },
    { immediate: true }
)

function handleClose() {
  emit('close')
}

function onSubmit(values: object) {
  emit('update:form', values)
  emit('submit', values)
}
</script>
