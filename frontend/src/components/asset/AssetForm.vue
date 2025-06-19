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
        <!-- 资产名称 + 名称管理 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产名称<span class="msg-error">*</span>
          </label>
          <Field name="assetNameId" v-slot="{ value, setValue }">
            <div class="flex items-center space-x-2">
              <BaseSelect
                  :modelValue="value"
                  :options="assetName.map(n => ({ label: n.name, value: n.id }))"
                  placeholder="请选择资产名称"
                  @update:modelValue="val => setValue(val)"
              />
              <button
                  type="button"
                  class="btn-outline"
                  @click="showNamesModal = true"
              >
                <LucideSettings size="16" class="mr-1"/>
                名称管理
              </button>
            </div>
          </Field>
          <ErrorMessage name="assetNameId" class="msg-error mt-1"/>
        </div>

        <!-- 资产分类 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产分类<span class="msg-error">*</span>
          </label>
          <Field name="assetTypeId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="types.map(t => ({ label: t.value1, value: t.id }))"
                placeholder="请选择资产分类"
                @update:modelValue="val => { setValue(val); onAssetTypeChange(val, setFieldValue) }"
            />
          </Field>
          <ErrorMessage name="assetTypeId" class="msg-error mt-1"/>
        </div>

        <!-- 资产位置 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            资产位置<span class="msg-error">*</span>
          </label>
          <Field name="assetLocationId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="locations.map(l => ({ label: l.value1, value: l.id }))"
                placeholder="请选择资产位置"
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="assetLocationId" class="msg-error mt-1"/>
        </div>

        <!-- 金额 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            金额<span class="msg-error">*</span>
          </label>
          <Field
              name="amount"
              type="number"
              min="0"
              step="0.01"
              class="input-base"
              required
          />
          <ErrorMessage name="amount" class="msg-error mt-1"/>
        </div>

        <!-- 货币单位 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            货币单位<span class="msg-error">*</span>
          </label>
          <Field name="unitId" v-slot="{ value, setValue }">
            <BaseSelect
                :modelValue="value"
                :options="filteredUnits.map(u => ({ label: u.value1, value: u.id }))"
                placeholder="请选择货币单位"
                @update:modelValue="val => setValue(val)"
            />
          </Field>
          <ErrorMessage name="unitId" class="msg-error mt-1"/>
        </div>

        <!-- 日期 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            日期<span class="msg-error">*</span>
          </label>
          <Field
              name="acquireTime"
              type="date"
              class="input-base"
              required
          />
          <ErrorMessage name="acquireTime" class="msg-error mt-1"/>
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
    </template>

    <template #footer>
      <div class="flex justify-end gap-4">
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
  </BaseModal>

  <AssetNameForm
      v-if="showNamesModal"
      :show="showNamesModal"
      @close="handleNamesModalClose"
      @refresh="handleNamesModalRefresh"
  />
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import * as yup from 'yup'
import {ErrorMessage, Field, Form} from 'vee-validate'
import {LucideSettings} from 'lucide-vue-next'
import {useMetaStore} from '@/store/metaStore'
import {setDefaultUnit} from '@/utils/commonMeta'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseSelect from '@/components/base/BaseSelect.vue'
import AssetNameForm from './assetName/AssetNameForm.vue'

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
const assetTypes = computed(() => metaStore.typeMap?.ASSET_TYPE || [])
const assetLocationTypes = computed(() => metaStore.typeMap?.ASSET_LOCATION || [])
const units = computed(() => metaStore.typeMap?.UNIT || [])

const schema = yup.object({
  assetNameId: yup.string().required('请选择资产名称'),
  assetTypeId: yup.string().required('请选择资产分类'),
  assetLocationId: yup.string().required('请选择资产位置'),
  amount: yup.number().typeError('请输入正确金额').required('请输入金额').min(0, '金额不能小于0'),
  unitId: yup.string().required('请选择货币单位'),
  acquireTime: yup.string().required('请选择日期'),
  remark: yup.string().nullable()
})

// 监听 props.form 变化，同步到内部 form 和重置表单
watch(
    () => props.form,
    val => {
      form.value = {...val}
      if (form.value.acquireTime && form.value.acquireTime.length > 10) {
        form.value.acquireTime = form.value.acquireTime.slice(0, 10)
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
