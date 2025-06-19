<template>
  <BaseModal
      :visible="show"
      :title="title"
      width="500px"
      @update:visible="handleVisibleChange"
  >
    <Form
        id="asset-form"
        :validation-schema="schema"
        :initial-values="form"
        @submit="handleSubmit"
        v-slot="{ values, setFieldValue }"
        class="p-6 space-y-6 overflow-auto"
    >
      <!-- 资产名称 + 名称管理 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          资产名称<span class="text-red-500">*</span>
        </label>
        <Field name="assetNameId" v-slot="{ value, setValue }">
          <div class="flex items-center space-x-2">
            <BaseSelect
                :modelValue="value"
                :options="assetNames.map(n => ({ label: n.name, value: n.id }))"
                placeholder="请选择资产名称"
                @update:modelValue="val => setValue(val)"
            />
            <button
                type="button"
                class="inline-flex items-center px-3 py-2 border border-indigo-600 text-indigo-600 rounded hover:bg-indigo-50 focus:ring-2 focus:ring-indigo-500"
                @click="showNamesModal = true"
            >
              <LucideSettings size="16" class="mr-1" />
              名称管理
            </button>
          </div>
        </Field>
        <ErrorMessage name="assetNameId" class="msg-error mt-1" />
      </div>

      <!-- 资产分类 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          资产分类<span class="text-red-500">*</span>
        </label>
        <Field name="assetTypeId" v-slot="{ value, setValue }">
          <BaseSelect
              :modelValue="value"
              :options="types.map(t => ({ label: t.value1, value: t.id }))"
              placeholder="请选择资产分类"
              @update:modelValue="val => { setValue(val); onAssetTypeChange(val, setFieldValue) }"
          />
        </Field>
        <ErrorMessage name="assetTypeId" class="msg-error mt-1" />
      </div>

      <!-- 资产位置 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          资产位置<span class="text-red-500">*</span>
        </label>
        <Field name="assetLocationId" v-slot="{ value, setValue }">
          <BaseSelect
              :modelValue="value"
              :options="locations.map(l => ({ label: l.value1, value: l.id }))"
              placeholder="请选择资产位置"
              @update:modelValue="val => setValue(val)"
          />
        </Field>
        <ErrorMessage name="assetLocationId" class="msg-error mt-1" />
      </div>

      <!-- 金额 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          金额<span class="text-red-500">*</span>
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
          货币单位<span class="text-red-500">*</span>
        </label>
        <Field name="unitId" v-slot="{ value, setValue }">
          <BaseSelect
              :modelValue="value"
              :options="filteredUnits.map(u => ({ label: u.value1, value: u.id }))"
              placeholder="请选择货币单位"
              @update:modelValue="val => setValue(val)"
          />
        </Field>
        <ErrorMessage name="unitId" class="msg-error mt-1" />
      </div>

      <!-- 日期 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">
          日期<span class="text-red-500">*</span>
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
    </Form>

    <template #footer>
      <div class="flex justify-end gap-4 p-4 border-t border-gray-200">
        <button type="button" class="btn-outline" @click="handleCancel">
          取消
        </button>
        <button
            type="submit"
            form="asset-form"
            class="btn-primary"
            :disabled="loading"
        >
          {{ loading ? '处理中...' : confirmText }}
        </button>
      </div>
    </template>

    <AssetNameForm
        v-if="showNamesModal"
        :show="showNamesModal"
        @close="handleNamesModalClose"
        @refresh="handleNamesModalRefresh"
    />
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Form, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import AssetNameForm from './assetName/AssetNameForm.vue'
import { LucideSettings } from 'lucide-vue-next'

const props = defineProps({
  show: Boolean,
  title: String,
  form: Object,
  assetNames: Array,
  types: Array,
  units: Array,
  locations: Array,
  loading: Boolean,
  confirmText: { type: String, default: '确定' },
  remarkPlaceholder: { type: String, default: '备注' }
})

const emit = defineEmits(['submit', 'cancel', 'refresh-names', 'update:visible'])

const showNamesModal = ref(false)
const form = ref({ ...props.form })

const filteredUnits = computed(() => {
  const selected = props.types.find(t => t.id === form.value.assetTypeId)
  return selected?.key3
      ? props.units.filter(unit => unit.key1 === selected.key3)
      : props.units
})

const schema = yup.object({
  assetNameId: yup.string().required('请选择资产名称'),
  assetTypeId: yup.string().required('请选择资产分类'),
  assetLocationId: yup.string().required('请选择资产位置'),
  amount: yup.number().typeError('请输入正确金额').required('请输入金额').min(0, '金额不能小于0'),
  unitId: yup.string().required('请选择货币单位'),
  acquireTime: yup.string().required('请选择日期'),
  remark: yup.string().nullable()
})

watch(() => props.form, val => {
  form.value = { ...val }
})

function onAssetTypeChange(assetTypeId, setFieldValue) {
  const selected = props.types.find(t => t.id === assetTypeId)
  const match = selected?.key3
      ? props.units.find(unit => unit.key1 === selected.key3)
      : null
  if (setFieldValue) setFieldValue('unitId', match?.id || '')
  else form.value.unitId = match?.id || ''
}

function handleSubmit(values) {
  emit('submit', values)
}

function handleCancel() {
  emit('cancel')
  emit('update:visible', false)
}

function handleVisibleChange(val) {
  if (!val) handleCancel()
  emit('update:visible', val)
}

function handleNamesModalClose() {
  showNamesModal.value = false
}

function handleNamesModalRefresh() {
  emit('refresh-names')
}

watch(() => props.show, val => {
  if (!val) showNamesModal.value = false
})
</script>
