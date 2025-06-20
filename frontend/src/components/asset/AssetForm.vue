<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="500px"
      @update:visible="handleClose"
      :zIndex="2001"
  >
    <Form
        ref="formRef"
        id="asset-form"
        :validation-schema="schema"
        :initial-values="form"
        v-slot="{ handleSubmit, setFieldValue }"
        class="space-y-6"
    >
      <form @submit.prevent="handleSubmit(onSubmit)">
        <!-- 资产名称 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产名称 <span class="msg-error">*</span>
          </label>
          <Field name="assetNameId" v-slot="{ value, setValue }">
            <div class="flex items-center space-x-2">
              <BaseSelect
                  :modelValue="value"
                  :options="assetNameStore.assetNameOptions"
                  placeholder="请选择资产名称"
                  clearable
                  @update:modelValue="val => setValue(val)"
              />
              <button
                  type="button"
                  class="btn-outline"
                  @click="assetNameRef?.open()"
              >
                <LucideSettings :size="16" class="mr-1" />
                名称管理
              </button>
            </div>
          </Field>
          <ErrorMessage name="assetNameId" class="msg-error mt-1" />
        </div>

        <!-- 资产分类 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产分类 <span class="msg-error">*</span>
          </label>
          <Field name="assetTypeId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="assetTypes"
                placeholder="请选择资产分类"
                clearable
                @update:modelValue="val => {
                setValue(val)
                onAssetTypeChange(val, setFieldValue)
              }"
            />
          </Field>
          <ErrorMessage name="assetTypeId" class="msg-error mt-1" />
        </div>

        <!-- 资产位置 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产位置 <span class="msg-error">*</span>
          </label>
          <Field name="assetLocationId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="assetLocations"
                placeholder="请选择资产位置"
                clearable
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="assetLocationId" class="msg-error mt-1" />
        </div>

        <!-- 金额 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            金额 <span class="msg-error">*</span>
          </label>
          <Field
              name="amount"
              type="number"
              min="0"
              step="0.01"
              class="input-base"
              required
          />
          <ErrorMessage name="amount" class="msg-error mt-1" />
        </div>

        <!-- 货币单位 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            货币单位 <span class="msg-error">*</span>
          </label>
          <Field name="unitId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="units"
                placeholder="请选择货币单位"
                clearable
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="unitId" class="msg-error mt-1" />
        </div>

        <!-- 日期 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            日期 <span class="msg-error">*</span>
          </label>
          <Field
              name="acquireTime"
              type="date"
              class="input-base"
              required
          />
          <ErrorMessage name="acquireTime" class="msg-error mt-1" />
        </div>

        <!-- 备注 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">备注</label>
          <Field
              name="remark"
              as="textarea"
              rows="3"
              :placeholder="remarkPlaceholder"
              class="input-base"
          />
        </div>

        <!-- 底部按钮区域 -->
        <div class="flex justify-end gap-4 mt-4">
          <button
              type="button"
              class="btn-outline"
              @click="handleClose"
              :disabled="loading"
          >
            取消
          </button>
          <button
              type="submit"
              class="btn-primary"
              :disabled="loading"
          >
            {{ loading ? '处理中...' : confirmText }}
          </button>
        </div>
      </form>
    </Form>
  </BaseModal>

  <AssetName ref="assetNameRef" @refresh="refreshAssetNames" />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ErrorMessage, Field, Form } from 'vee-validate'
import * as yup from 'yup'
import { LucideSettings } from 'lucide-vue-next'
import { useAssetNameStore } from '@/store/assetNameStore'
import { useMetaStore } from '@/store/metaStore'
import { setDefaultUnit } from '@/utils/commonMeta'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import AssetName from './assetName/AssetName.vue'

const props = defineProps({
  visible: Boolean,
  form: Object,
  loading: Boolean,
  title: String,
  confirmText: String,
  remarkPlaceholder: String
})

const emit = defineEmits(['close', 'submit', 'update:form'])

const formRef = ref()
const assetNameRef = ref()

const form = ref({ ...props.form })

const assetNameStore = useAssetNameStore()
const metaStore = useMetaStore()

const assetTypes = computed(() => metaStore.typeMap?.ASSET_TYPE?.map(i => ({ label: String(i.value1), value: i.id })) || [])
const assetLocations = computed(() => metaStore.typeMap?.ASSET_LOCATION?.map(i => ({ label: String(i.value1), value: i.id })) || [])
const units = computed(() => metaStore.typeMap?.UNIT?.map(i => ({ label: String(i.value1), value: i.id })) || [])

const schema = yup.object({
  assetNameId: yup.string().required('请选择资产名称'),
  assetTypeId: yup.string().required('请选择资产分类'),
  assetLocationId: yup.string().required('请选择资产位置'),
  amount: yup.number().typeError('请输入正确金额').required('请输入金额').min(0, '金额不能小于0'),
  unitId: yup.string().required('请选择货币单位'),
  acquireTime: yup.string().required('请选择日期'),
  remark: yup.string().nullable()
})

watch(
    () => props.form,
    val => {
      form.value = { ...val }
      if (form.value.acquireTime?.length > 10) {
        form.value.acquireTime = form.value.acquireTime.slice(0, 10)
      }
      formRef.value?.resetForm({ values: form.value })
      if (form.value.assetTypeId) {
        setDefaultUnit(form.value.assetTypeId, undefined, { unitId: form.value.unitId })
      }
    },
    { immediate: true }
)

function onAssetTypeChange(assetTypeId: string, setFieldValue: (field: string, value: any) => void) {
  if (!assetTypeId) {
    setFieldValue('unitId', null)
    return
  }
  setDefaultUnit(assetTypeId, setFieldValue, { unitId: form.value.unitId })
}

function onSubmit(values: any) {
  emit('update:form', values)
  emit('submit', values)
}

function handleClose() {
  emit('close')
}

function refreshAssetNames() {
  assetNameStore.fetchAssetName()
}
</script>
