<template>
  <BaseModal
      :visible="visible"
      :title="title"
      width="590px"
      @update:visible="handleClose"
      :zIndex="2001"
  >
    <Form
        ref="formRef"
        id="asset-form"
        :validation-schema="schema"
        :initial-values="form"
        v-slot="{ handleSubmit, setFieldValue }"
    >
      <form @submit.prevent="handleSubmit(onSubmit)" class="space-y-3 px-1">
        <!-- 资产名称 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            资产名称 <span class="msg-error">*</span>
          </label>
          <Field name="assetNameId" v-slot="{ value, setValue }">
            <div class="flex items-center gap-2">
              <BaseSelect
                  title="资产名称"
                  :modelValue="value"
                  :options="assetNameStore.assetNameOptions"
                  clearable
                  required
                  searchable
                  @update:modelValue="val => setValue(val)"
              />
              <BaseButton
                  type="button"
                  title="名称管理"
                  color="outline"
                  @click="assetNameRef?.open()"
                  :icon="LucideSettings"
                  variant="search"
                  class="w-40"
              />
            </div>
          </Field>
          <ErrorMessage name="assetNameId" class="msg-error mt-1"/>
        </div>

        <!-- 资产分类 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            资产分类 <span class="msg-error">*</span>
          </label>
          <Field name="assetTypeId" v-slot="{ value, setValue }">
            <BaseSelect
                title="资产分类"
                :modelValue="value"
                :options="assetTypes"
                clearable
                required
                :multiple="false"
                searchable
                @update:modelValue="(val) => {
                  setValue(val)
                  onAssetTypeChange(val as SingleValue, setFieldValue)
                }"
            />
          </Field>
          <ErrorMessage name="assetTypeId" class="msg-error mt-1"/>
        </div>

        <!-- 资产位置 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            资产位置 <span class="msg-error">*</span>
          </label>
          <Field name="assetLocationId" v-slot="{ value, setValue }">
            <BaseSelect
                title="资产位置"
                :modelValue="value"
                :options="assetLocations"
                clearable
                required
                searchable
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="assetLocationId" class="msg-error mt-1"/>
        </div>

        <!-- 金额 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            金额 <span class="msg-error">*</span>
          </label>
          <Field name="amount" v-slot="{ value, setValue }">
            <BaseInput
                title="金额"
                type="number"
                :min="1"
                required
                clearable
                :disabled="loading"
                :modelValue="value"
                @update:modelValue="setValue"
            />
          </Field>
          <ErrorMessage name="amount" class="msg-error mt-1"/>
        </div>

        <!-- 货币单位 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            货币单位 <span class="msg-error">*</span>
          </label>
          <Field name="unitId" v-slot="{ value, setValue }">
            <BaseSelect
                title="货币单位"
                :modelValue="value"
                :options="units"
                clearable
                required
                searchable
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="unitId" class="msg-error mt-1"/>
        </div>

        <!-- 登记日期 -->
        <div>
          <label class="text-sm font-medium text-gray-700 mb-1 block">
            登记日期 <span class="msg-error">*</span>
          </label>
          <Field name="acquireTime" v-slot="{ value, setValue }">
            <BaseDateInput
                :modelValue="value"
                @update:modelValue="setValue"
                title="登记日期"
                type="date"
                required
                clearable
                :max="today"
            />
          </Field>
          <ErrorMessage name="acquireTime" class="msg-error mt-1" />
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
        <div class="flex justify-end gap-4 mt-6">
          <BaseButton type="button" title="取消" @click="handleClose" color="outline" :loading="loading"/>
          <BaseButton type="submit" color="primary" :loading="loading">
            {{ loading ? '处理中...' : confirmText }}
          </BaseButton>
        </div>
      </form>
    </Form>
  </BaseModal>

  <AssetName ref="assetNameRef" @refresh="refreshAssetNames"/>
</template>

<script setup lang="ts">
import {ref, computed, watch} from 'vue'
import {ErrorMessage, Field, Form} from 'vee-validate'
import * as yup from 'yup'
import dayjs from 'dayjs'
import {LucideSettings} from 'lucide-vue-next'
import {useAssetNameStore} from '@/store/assetNameStore'
import {useMetaStore} from '@/store/metaStore'
import {setDefaultUnit} from '@/utils/commonMeta'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import BaseDateInput from '@/components/base/BaseDateInput.vue'
import AssetName from './assetName/AssetName.vue'
import BaseButton from "@/components/base/BaseButton.vue"
import {SingleValue} from "@/types/common"

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
const form = ref({...props.form})

const assetNameStore = useAssetNameStore()
const metaStore = useMetaStore()

const today = new Date().toISOString().slice(0, 10) // 格式: 'YYYY-MM-DD'

const assetTypes = computed(() => metaStore.typeMap?.ASSET_TYPE?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])
const assetLocations = computed(() => metaStore.typeMap?.ASSET_LOCATION?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])
const units = computed(() => metaStore.typeMap?.UNIT?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])

const schema = yup.object({
  assetNameId: yup.string().required('请选择资产名称'),
  assetTypeId: yup.string().required('请选择资产分类'),
  assetLocationId: yup.string().required('请选择资产位置'),
  amount: yup.number().typeError('请输入正确金额').required('请输入金额').min(0, '金额不能小于0'),
  unitId: yup.string().required('请选择货币单位'),
  acquireTime: yup
      .string()
      .required()
      .test('is-not-future', '登记日期不能大于今日', val => {
        if (!val) return true // 为空时不进行此验证
        const inputDate = dayjs(val).startOf('day')
        const todayDate = dayjs().startOf('day')
        // 使用 unix() 或 valueOf() 进行比较
        return inputDate.valueOf() <= todayDate.valueOf()
      }),

  remark: yup.string().nullable()
})

watch(
    () => props.form,
    val => {
      form.value = {...val}
      if (form.value.acquireTime?.length > 10) {
        form.value.acquireTime = form.value.acquireTime.slice(0, 10)
      }
      formRef.value?.resetForm({values: form.value})
      if (form.value.assetTypeId) {
        setDefaultUnit(form.value.assetTypeId, undefined, {unitId: form.value.unitId})
      }
    },
    {immediate: true}
)

function onAssetTypeChange(
    assetTypeId: string | number | null,
    setFieldValue: (field: string, value: any) => void
) {
  if (!assetTypeId) {
    setFieldValue('unitId', null)
    return
  }
  setDefaultUnit(String(assetTypeId), setFieldValue, {unitId: form.value.unitId})
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
