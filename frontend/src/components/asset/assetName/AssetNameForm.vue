<template>
  <form @submit.prevent="onSubmit" class="space-y-4">
    <div>
      <label for="name" class="block text-sm font-medium text-gray-700">
        资产名称 <span class="msg-error">*</span>
      </label>
      <BaseInput
          v-model="form.name"
          placeholder="请输入资产名称"
          required
          clearable
      />
    </div>

    <div>
      <label for="description" class="block text-sm font-medium text-gray-700">
        资产描述
      </label>
      <BaseInput
          type="text"
          v-model="form.description"
          placeholder="请输入资产描述（选填）"
          clearable
      />
    </div>

    <div class="flex justify-end space-x-3">
      <BaseButton
          type="button"
          title="取消"
          @click="closeModal"
          color="outline"
      />
      <BaseButton
          type="submit"
          :loading="loadingSubmit"
          color="primary"
      >
        {{ isEdit ? '保存' : '新增' }}
      </BaseButton>
    </div>
  </form>
</template>

<script setup lang="ts">
import { reactive, watchEffect, toRefs } from 'vue'
import BaseInput from "@/components/base/BaseInput.vue";
import BaseButton from "@/components/base/BaseButton.vue";

const props = defineProps<{
  formData: { id: number | null; name: string; description: string }
  loadingSubmit: boolean
  formError: string
  isEdit: boolean
}>()

const emit = defineEmits(['submit', 'closeModal', 'update:formData', 'clearError'])

const { formData } = toRefs(props)

interface FormData {
  id: number | null
  name: string
  description: string
}

const form = reactive<FormData>({
  id: null,
  name: '',
  description: ''
})

watchEffect(() => {
  form.id = formData.value.id
  form.name = formData.value.name
  form.description = formData.value.description
})

function onSubmit() {
  emit('submit', {...form})
}

function closeModal() {
  emit('closeModal')
}

watch(
    () => ({...form}),
    (newVal) => {
      emit('update:formData', {...newVal})
    },
    {deep: true}
)
</script>

