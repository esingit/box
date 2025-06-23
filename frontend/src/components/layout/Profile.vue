<template>
  <BaseModal
      v-model:visible="isOpen"
      title="设置"
      width="700px"
  >
    <!-- 主体内容 -->
    <div class="flex overflow-hidden">
      <!-- 左侧菜单 -->
      <nav class="w-40 space-y-1" aria-label="设置菜单">
        <BaseButton
            type="button"
            title="个人信息"
            block
            color="text"
            :class="activeTab === 'profile' ? 'bg-[var(--bg-btn-hover)]' : ''"
            @click="switchTab('profile')"
        />
        <BaseButton
            type="button"
            title="安全设置"
            block
            color="text"
            :class="activeTab === 'security' ? 'bg-[var(--bg-btn-hover)]' : ''"
            @click="switchTab('security')"
        />
      </nav>

      <!-- 右侧内容区 -->
      <section
          ref="contentRef"
          class="flex-1 p-8 overflow-auto outline-none h-[560px]"
          tabindex="0"
          aria-live="polite"
      >
        <div v-if="activeTab === 'profile'">
          <h3 class="text-xl font-semibold mb-6 text-gray-800 ">个人信息</h3>
          <table class="w-full text-sm text-gray-700 border-collapse">
            <tbody>
            <tr class="border-b last:border-b-0">
              <td class="font-medium w-32 py-3">用户名</td>
              <td class="py-3">{{ user?.username || 'N/A' }}</td>
            </tr>
            <tr class="border-b last:border-b-0">
              <td class="font-medium py-3">上次登录</td>
              <td class="py-3">{{ formatDateTime(user?.lastLoginTime) }}</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div v-else-if="activeTab === 'security'">
          <h3 class="text-xl font-semibold mb-6 text-gray-800 ">修改密码</h3>
          <Form
              :validation-schema="schema"
              v-slot="{ handleSubmit, errors }"
              class="space-y-6 max-w-md"
          >
            <form @submit.prevent="handleSubmit(handlePasswordSubmit)" autocomplete="off" novalidate>
              <div class="mb-4">
                <label for="oldPassword" class="block text-sm font-medium text-gray-700 mb-2">旧密码</label>
                <Field name="oldPassword" v-slot="{ field }">
                  <BaseInput
                      id="oldPassword"
                      type="password"
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="请输入旧密码"
                      clearable
                  />
                </Field>
                <span class="msg-error mt-1 block text-xs">{{ errors.oldPassword }}</span>
              </div>

              <div class="mb-4">
                <label for="newPassword" class="block text-sm font-medium text-gray-700 mb-2">新密码</label>
                <Field name="newPassword" v-slot="{ field }">
                  <BaseInput
                      id="newPassword"
                      type="password"
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="6-20位字母数字组合"
                      clearable
                  />
                </Field>
                <span class="msg-error mt-1 block text-xs">{{ errors.newPassword }}</span>
              </div>

              <div class="mb-4">
                <label for="confirmPassword" class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
                <Field name="confirmPassword" v-slot="{ field }">
                  <BaseInput
                      id="confirmPassword"
                      type="password"
                      :model-value="field.value"
                      @update:model-value="field.onChange"
                      placeholder="再次确认新密码"
                      clearable
                  />
                </Field>
                <span class="msg-error mt-1 block text-xs">{{ errors.confirmPassword }}</span>
              </div>

              <p
                  v-if="resetMsg"
                  :class="resetSuccess ? 'msg-success' : 'msg-error'"
                  class="mt-1 text-sm font-medium"
                  role="alert"
              >
                {{ resetMsg }}
              </p>

              <div class="flex gap-3">
                <BaseButton type="submit" color="primary" :disabled="loading">
                  {{ loading ? '提交中...' : '确认修改' }}
                </BaseButton>
                <BaseButton type="button" title="重置" color="outline" @click="onResetClicked"/>
              </div>
            </form>
          </Form>
        </div>
      </section>
    </div>
  </BaseModal>
</template>

<script setup lang="ts">
import {ref, computed, nextTick} from 'vue'
import * as yup from 'yup'
import {Field, Form} from 'vee-validate'
import {useUserStore} from '@/store/userStore'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseInput from '@/components/base/BaseInput.vue'

const isOpen = ref(false)
const activeTab = ref<'profile' | 'security'>('profile')
const contentRef = ref<HTMLElement | null>(null)

const loading = ref(false)
const resetMsg = ref('')
const resetSuccess = ref(false)

const userStore = useUserStore()
const user = computed(() => userStore.user)

const schema = yup.object({
  oldPassword: yup.string().required('请输入旧密码'),
  newPassword: yup
      .string()
      .required('请输入新密码')
      .matches(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/, '密码必须为6-20位字母数字组合'),
  confirmPassword: yup
      .string()
      .oneOf([yup.ref('newPassword')], '两次密码不一致')
      .required('请确认新密码'),
})

function formatDateTime(val?: string): string {
  if (!val) return 'N/A'
  const d = new Date(val)
  return isNaN(d.getTime()) ? val : d.toLocaleString()
}

function onResetClicked() {
  resetMsg.value = ''
  resetSuccess.value = false
}

function switchTab(tab: 'profile' | 'security') {
  if (activeTab.value !== tab) {
    activeTab.value = tab
    resetMsg.value = ''
    resetSuccess.value = false
    nextTick(() => {
      contentRef.value?.scrollTo({top: 0, behavior: 'smooth'})
    })
  }
}

async function handlePasswordSubmit(values: any) {
  loading.value = true
  const {oldPassword, newPassword} = values
  const result = await userStore.resetPassword(oldPassword, newPassword)
  resetMsg.value = result.message
  resetSuccess.value = result.success
  loading.value = false
}

function openModal() {
  isOpen.value = true
  activeTab.value = 'profile'
  resetMsg.value = ''
  resetSuccess.value = false
}

defineExpose({openModal})
</script>
