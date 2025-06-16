<template>
  <n-modal
      v-model:show="visibleProxy"
      title="欢迎回来"
      preset="card"
      :mask-closable="false"
      @close="handleClose"
      style="max-width: 500px;"
  >
    <template #header>
      <div class="modal-header">欢迎回来</div>
      <div class="modal-divider"></div>
    </template>

    <n-form
        ref="loginFormRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="70px"
        autocomplete="off"
        style="padding: 24px;"
    >
      <n-form-item label="用户名" path="username">
        <n-input
            v-model:value="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
            clearable
        />
      </n-form-item>

      <n-form-item label="密码" path="password">
        <n-input
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
            clearable
        />
        <template #feedback>
          <div v-if="errorMessage && !needCaptcha" class="error-message">{{ errorMessage }}</div>
        </template>
      </n-form-item>

      <n-form-item v-if="needCaptcha" label="验证码" path="captcha">
        <n-space align="center" :wrap="false" style="width: 100%;">
          <n-input
              v-model:value="form.captcha"
              placeholder="请输入验证码"
              clearable
              style="flex: 1;"
          />
          <img
              :src="captchaUrl"
              @click="refreshCaptcha"
              alt="captcha"
              class="captcha-img"
              title="点击刷新验证码"
          />
        </n-space>
        <template #feedback>
          <div v-if="errorMessage && needCaptcha" class="error-message">{{ errorMessage }}</div>
        </template>
      </n-form-item>

      <n-form-item style="margin-top: 8px;">
        <n-button
            type="primary"
            block
            :loading="loading"
            @click="handleSubmit"
            style="height: 40px; font-size: 14px;"
        >
          登录
        </n-button>
      </n-form-item>
    </n-form>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import axios from 'axios'
import type { FormInst, FormRules } from 'naive-ui'
import { useUserStore } from '@/store/userStore'
import { tokenService } from '@/api/tokenService'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'login-success'])

const visibleProxy = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const loginFormRef = ref<FormInst | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [
    {
      trigger: 'blur',
      validator: () =>
          needCaptcha.value && !form.captcha.trim()
              ? new Error('请输入验证码')
              : true
    }
  ]
}

watch(needCaptcha, async (val) => {
  if (val && !captchaId.value) {
    await refreshCaptcha()
  } else if (!val) {
    clearCaptcha()
  }
})

function clearCaptcha() {
  form.captcha = ''
  captchaUrl.value = ''
  captchaId.value = ''
}

async function refreshCaptcha() {
  try {
    const { data } = await axios.get('/api/captcha', {
      params: { t: Date.now() }
    })
    captchaUrl.value = `/api${data.captchaUrl}`
    captchaId.value = data.captchaId
    clearErrorMessage()
  } catch {
    setErrorMessage('验证码加载失败')
    clearCaptcha()
  }
}

function handleClose() {
  visibleProxy.value = false
  resetForm()
}

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    captcha: ''
  })
  clearErrorMessage()
  needCaptcha.value = false
  clearCaptcha()
  loginFormRef.value?.restoreValidation?.()
}

async function handleSubmit() {
  if (loading.value) return
  
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  clearErrorMessage()

  const loginData: Record<string, any> = {
    username: form.username.trim(),
    password: form.password.trim()
  }

  if (needCaptcha.value) {
    loginData.captcha = form.captcha.trim()
    loginData.captchaId = captchaId.value
  }

  try {
    const res = await userStore.login(loginData)
    handleLoginResponse(res)
  } catch (e: any) {
    handleLoginError(e)
  } finally {
    loading.value = false
  }
}

function handleLoginResponse(res: any) {
  if (res.success) {
    emit('login-success', res)
    visibleProxy.value = false
    resetForm()
  } else {
    setErrorMessage(res.message || '登录失败')
    handleCaptchaRequirement(res)
  }
}

function handleLoginError(e: any) {
  const data = e.response?.data
  setErrorMessage(data?.message || e.message || '登录异常')
  handleCaptchaRequirement(data)
}

function handleCaptchaRequirement(response: any) {
  if (response.needCaptcha || response.code === 'CAPTCHA_ERROR') {
    needCaptcha.value = true
    refreshCaptcha()
  }
}

function setErrorMessage(message: string) {
  errorMessage.value = message
}

function clearErrorMessage() {
  errorMessage.value = ''
}
</script>
