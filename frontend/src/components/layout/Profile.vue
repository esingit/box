<template>
  <div
      v-if="isOpen"
      class="modal-overlay"
      @click="closeModal"
  >
    <div
        class="modal-container animate-fade-in w-[800px]"
        @click.stop
    >
      <!-- Header -->
      <header class="modal-header">
        <h2 class="modal-title">设置</h2>
        <button
            @click="closeModal"
            aria-label="关闭设置窗口"
            class="btn-text text-xl"
            type="button"
        >
          ✕
        </button>
      </header>

      <div class="modal-divider"/>

      <!-- Body -->
      <div class="flex flex-1 overflow-hidden">
        <!-- Sidebar -->
        <nav class="w-48 p-3 space-y-1" aria-label="设置菜单">
          <button
              type="button"
              class="w-full menu-btn"
              :class="activeTab === 'profile' ? 'active' : ''"
              @click="switchTab('profile')"
          >
            个人信息
          </button>
          <button
              type="button"
              class="w-full menu-btn"
              :class="activeTab === 'security' ? 'active' : ''"
              @click="switchTab('security')"
          >
            安全设置
          </button>
        </nav>

        <!-- Content -->
        <section
            ref="contentRef"
            class="flex-1 p-8 overflow-auto outline-none h-[560px]"
            tabindex="0"
            aria-live="polite"
        >
          <!-- 你的内容这里不变 -->
          <div v-if="activeTab === 'profile'">
            <h3 class="text-xl font-semibold mb-6 text-gray-900">个人信息</h3>
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
            <h3 class="text-xl font-semibold mb-6 text-gray-900">修改密码</h3>
            <Form
                :validation-schema="schema"
                @submit="handlePasswordSubmit"
                class="space-y-6 max-w-md"
                v-slot="{ errors }"
            >
              <!-- 表单内容不变 -->
              <div>
                <label for="oldPassword" class="block text-sm font-medium text-gray-700 mb-2">旧密码</label>
                <Field
                    id="oldPassword"
                    name="oldPassword"
                    type="password"
                    placeholder="请输入旧密码"
                    class="input-base"
                />
                <span class="msg-error">{{ errors.oldPassword }}</span>
              </div>

              <div>
                <label for="newPassword" class="block text-sm font-medium text-gray-700 mb-2">新密码</label>
                <Field
                    id="newPassword"
                    name="newPassword"
                    type="password"
                    placeholder="6-20位字母数字组合"
                    class="input-base"
                />
                <span class="msg-error">{{ errors.newPassword }}</span>
              </div>

              <div>
                <label for="confirmPassword" class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
                <Field
                    id="confirmPassword"
                    name="confirmPassword"
                    type="password"
                    placeholder="再次确认新密码"
                    class="input-base"
                />
                <span class="msg-error">{{ errors.confirmPassword }}</span>
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
                <button type="submit" class="btn-primary" :disabled="loading">
                  {{ loading ? '提交中...' : '确认修改' }}
                </button>
                <button type="button" class="btn-outline" @click="onResetClicked">重置</button>
              </div>
            </Form>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, computed, nextTick} from 'vue'
import {useUserStore} from '@/store/userStore'
import {Field, Form} from 'vee-validate'
import * as yup from 'yup'

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

function resetForm() {
  // 这里只重置输入，由vee-validate的Form实现
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

function closeModal() {
  isOpen.value = false
  resetMsg.value = ''
  resetSuccess.value = false
}

defineExpose({openModal, closeModal})
</script>
