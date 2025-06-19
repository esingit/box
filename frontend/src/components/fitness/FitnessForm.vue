<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="500px"
      @update:visible="handleClose"
  >
    <!-- 表单主体 -->
    <template #default>
      <Form
          ref="formRef"
          id="fitness-form"
          :validation-schema="schema"
          :initial-values="form"
          @submit="handleSubmit"
          v-slot="{ values, setFieldValue }"
          class="space-y-6"
      >
        <!-- 类型 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">类型</label>
          <Field name="typeId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="fitnessTypes
                .filter(t => t.value1 != null)
                .map(t => ({ label: t.value1!, value: t.id }))"
                placeholder="请选择"
                @update:modelValue="val => {
                setValue(val)
                setDefaultUnit(val, setFieldValue, values)
              }"
                clearable
            />
          </Field>
          <ErrorMessage name="typeId" class="msg-error"/>
        </div>

        <!-- 次数 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">次数</label>
          <Field
              name="count"
              type="number"
              min="1"
              class="input-base"
          />
          <ErrorMessage name="count" class="msg-error"/>
        </div>

        <!-- 单位 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">单位</label>
          <Field name="unitId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="units
                          .filter(u => u.value1 != null)
                          .map(u => ({ label: u.value1!, value: u.id }))"
                placeholder="请选择"
                @update:modelValue="val => setValue(val)"
                clearable
            />
          </Field>
          <ErrorMessage name="unitId" class="msg-error"/>
        </div>

        <!-- 完成时间 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">完成时间</label>
          <Field
              name="finishTime"
              type="date"
              class="input-base"
          />
          <ErrorMessage name="finishTime" class="msg-error"/>
        </div>

        <!-- 备注 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
          <Field
              as="textarea"
              name="remark"
              :placeholder="remarkPlaceholder"
              rows="3"
              class="input-base"
          />
        </div>
      </Form>
    </template>

    <!-- 底部按钮区域 -->
    <template #footer>
      <div class="flex justify-end gap-4">
        <button type="button" @click="handleClose" class="btn-outline">取消</button>
        <button
            type="submit"
            form="fitness-form"
            :disabled="loading"
            class="btn-primary"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import {Form, Field, ErrorMessage} from 'vee-validate'
import * as yup from 'yup'
import {ref, watch, computed} from 'vue'
import {useMetaStore} from '@/store/metaStore'
import {setDefaultUnit} from '@/utils/commonMeta'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'

const props = defineProps({
  visible: Boolean,
  form: Object,
  loading: Boolean,
  title: String,
  confirmText: String,
  remarkPlaceholder: String,
})

const emit = defineEmits(['close', 'submit', 'update:form'])

const formRef = ref()
const form = ref({...props.form})

const metaStore = useMetaStore()
const fitnessTypes = computed(() => metaStore.typeMap?.FITNESS_TYPE || [])
const units = computed(() => metaStore.typeMap?.UNIT || [])

const schema = yup.object({
  assetTypeId: yup.string().required('请选择类型'),
  count: yup
      .number()
      .typeError('请输入次数')
      .required('请输入次数')
      .min(1, '次数不能小于1'),
  unitId: yup.string().required('请选择单位'),
  finishTime: yup.string().required('请输入完成时间'),
  remark: yup.string().nullable(),
})

// 监听 props.form 变化，同步到内部 form 和重置表单
watch(
    () => props.form,
    val => {
      form.value = {...val}
      if (form.value.finishTime && form.value.finishTime.length > 10) {
        form.value.finishTime = form.value.finishTime.slice(0, 10)
      }
      formRef.value?.resetForm({values: form.value})

      if (form.value.assetTypeId) {
        setDefaultUnit(form.value.assetTypeId)
      }
    },
    {immediate: true}
)

function handleClose() {
  emit('close')
}

function handleSubmit(values: any) {
  emit('update:form', values)
  emit('submit', values)
}
</script>
