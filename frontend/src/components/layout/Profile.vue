<template>
  <n-modal
      v-model:show="isOpen"
      :closable="false"
      :auto-focus="false"
      style="width: 800px; height: 500px;"
  >
    <n-card
        title="设置"
        :bordered="false"
        size="huge"
        role="dialog"
        aria-labelledby="settings-modal"
        style="width: 100%; height: 100%;"
    >
      <template #header-extra>
        <n-button quaternary circle @click="closeModal">
          <template #icon>
            <LucideX class="close-icon" />
          </template>
        </n-button>
      </template>

      <n-layout has-sider style="height: calc(100% - 60px);">
        <!-- 左侧导航菜单 -->
        <n-layout-sider
            :width="200"
            :collapsed-width="200"
            show-trigger="bar"
            collapse-mode="width"
            :native-scrollbar="false"
            bordered
        >
          <n-menu
              :options="menuOptions"
              v-model:value="activeTab"
              :root-indent="18"
              :indent="24"
          />
        </n-layout-sider>

        <!-- 右侧内容区域 -->
        <n-layout-content :native-scrollbar="false" style="padding: 24px;">
          <!-- 个人信息面板 -->
          <div v-show="activeTab === 'profile'">
            <n-h3 style="margin-top: 0;">个人信息</n-h3>
            <n-descriptions
                :column="1"
                bordered
                size="medium"
                label-placement="left"
                :label-style="{ width: '120px' }"
            >
              <n-descriptions-item label="用户名">
                <n-text strong>{{ user?.username || 'N/A' }}</n-text>
              </n-descriptions-item>
              <n-descriptions-item label="上次登录">
                <n-text>{{ formatDateTime(user?.lastLoginTime) }}</n-text>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <!-- 安全设置面板 -->
          <div v-show="activeTab === 'security'">
            <n-h3 style="margin-top: 0;">修改密码</n-h3>
            <n-form
                ref="formRef"
                :model="formModel"
                :rules="formRules"
                size="medium"
                label-placement="left"
                :label-width="100"
                style="max-width: 400px;"
            >
              <n-form-item label="旧密码" path="oldPassword">
                <n-input
                    v-model:value="formModel.oldPassword"
                    type="password"
                    placeholder="请输入旧密码"
                    autocomplete="current-password"
                    show-password-on="click"
                />
              </n-form-item>

              <n-form-item label="新密码" path="newPassword">
                <n-input
                    v-model:value="formModel.newPassword"
                    type="password"
                    placeholder="6-20位字母数字组合"
                    autocomplete="new-password"
                    show-password-on="click"
                />
              </n-form-item>

              <n-form-item label="确认密码" path="confirmPassword">
                <n-input
                    v-model:value="formModel.confirmPassword"
                    type="password"
                    placeholder="再次确认新密码"
                    autocomplete="new-password"
                    show-password-on="click"
                />
              </n-form-item>

              <n-form-item v-if="resetMsg">
                <n-alert
                    :type="resetSuccess ? 'success' : 'error'"
                    closable
                    @close="resetMsg = ''"
                >
                  {{ resetMsg }}
                </n-alert>
              </n-form-item>

              <n-form-item>
                <n-space>
                  <n-button
                      type="primary"
                      :loading="loading"
                      @click="handlePasswordSubmit"
                  >
                    确认修改
                  </n-button>
                  <n-button @click="resetForm">重置</n-button>
                </n-space>
              </n-form-item>
            </n-form>
          </div>
        </n-layout-content>
      </n-layout>
    </n-card>
  </n-modal>
</template>

<script lang="ts" setup>
import { ref, computed, reactive } from 'vue'
import { useUserStore } from '@/store/userStore'
import { useMessage } from 'naive-ui'
import axios from '@/utils/axios'
import type { FormInst, FormRules, MenuOption } from 'naive-ui'
import { LucideX } from 'lucide-vue-next'

interface User {
  username?: string
  lastLoginTime?: string
}

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const userStore = useUserStore()
const message = useMessage()
const formRef = ref<FormInst | null>(null)

const user = computed<User>(() => userStore.user)
const isOpen = ref<boolean>(false)
const activeTab = ref<string>('profile')
const resetMsg = ref<string>('')
const resetSuccess = ref<boolean>(false)
const loading = ref<boolean>(false)

const formModel = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const formRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/,
      message: '密码必须为6-20位，包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value) => {
        return value === formModel.newPassword
      },
      message: '两次新密码输入不一致',
      trigger: 'blur'
    }
  ]
}

const menuOptions: MenuOption[] = [
  {
    label: '个人信息',
    key: 'profile'
  },
  {
    label: '安全设置',
    key: 'security'
  }
]

const closeModal = (): void => {
  isOpen.value = false
  resetForm()
  resetMsg.value = ''
  resetSuccess.value = false
}

const resetForm = (): void => {
  formModel.oldPassword = ''
  formModel.newPassword = ''
  formModel.confirmPassword = ''
  formRef.value?.restoreValidation()
}

const formatDateTime = (val: string | undefined): string => {
  if (!val) return 'N/A'

  try {
    const date = new Date(val)
    if (isNaN(date.getTime())) return val

    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  } catch {
    return val
  }
}

const handlePasswordSubmit = async (): Promise<void> => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  resetMsg.value = ''
  resetSuccess.value = false
  loading.value = true

  try {
    const { data } = await axios.post('/api/user/reset-password', {
      username: user.value.username,
      oldPassword: formModel.oldPassword,
      newPassword: formModel.newPassword
    })

    if (data.success) {
      resetMsg.value = '密码重置成功！'
      resetSuccess.value = true
      message.success('密码修改成功')
      resetForm()
    } else {
      resetMsg.value = data.message || '重置失败'
      resetSuccess.value = false
    }
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || '请求失败，请稍后再试'
    resetMsg.value = errorMessage
    resetSuccess.value = false
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const openModal = (): void => {
  isOpen.value = true
  activeTab.value = 'profile'
  resetForm()
}

defineExpose({ openModal })
</script>