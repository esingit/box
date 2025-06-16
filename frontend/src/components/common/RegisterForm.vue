<template>
  <!-- 注册弹窗 -->
  <n-modal
      v-model:show="visibleProxy"
      title="用户注册"
      preset="card"
      :mask-closable="false"
      @close="handleClose"
      style="max-width: 500px;"
  >
    <template #header>
      <div class="modal-header">注册新用户</div>
      <div class="modal-divider"></div>
    </template>

    <n-form
        ref="registerFormRef"
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
            name="username"
            autocomplete="username"
            spellcheck="false"
            autocapitalize="none"
            clearable
        />
      </n-form-item>

      <n-form-item label="密码" path="password">
        <n-input
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            name="password"
            autocomplete="new-password"
            spellcheck="false"
            autocapitalize="none"
            clearable
        />
      </n-form-item>

      <n-form-item label="确认密码" path="confirmPassword">
        <n-input
            v-model:value="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            name="confirmPassword"
            autocomplete="new-password"
            spellcheck="false"
            autocapitalize="none"
            clearable
        />
      </n-form-item>

      <n-form-item v-if="needCaptcha" label="验证码" path="captcha">
        <n-space align="center" :wrap="false" style="width: 100%;">
          <n-input
              v-model:value="form.captcha"
              placeholder="请输入验证码"
              autocomplete="off"
              spellcheck="false"
              autocapitalize="none"
              style="flex: 1;"
              clearable
          />
          <img
              :src="captchaUrl"
              @click="refreshCaptcha"
              alt="captcha"
              class="captcha-img"
              title="点击刷新验证码"
              style="cursor: pointer; height: 38px;"
          />
        </n-space>
      </n-form-item>

      <n-form-item style="margin-top: 8px;">
        <n-button
            type="primary"
            block
            :loading="loading"
            @click="handleSubmit"
            style="height: 40px; font-size: 14px;"
        >
          注册
        </n-button>
      </n-form-item>

      <div v-if="error" class="error-message">
        {{ error }}
      </div>
    </n-form>
  </n-modal>

  <!-- 注册成功弹窗 -->
  <n-modal
      v-model:show="successModalVisible"
      title="注册成功"
      preset="card"
      :mask-closable="false"
      @close="handleSuccessClose"
      style="max-width: 400px; text-align: center;"
      show-icon
  >
    <div>恭喜您，注册成功！请使用您的账号登录。</div>
    <n-button style="margin-top: 24px;" type="primary" @click="handleSuccessClose">确定</n-button>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import axios from 'axios'
import type { FormInst, FormRules } from 'naive-ui'
import { tokenService } from '@/api/tokenService'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'register-success'])

const visibleProxy = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val)
})

const loading = ref(false)
const error = ref('')
const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)
const successModalVisible = ref(false)
const registerFormRef = ref<FormInst | null>(null)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度应为 4~20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度应为 6~32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      required: true,
      trigger: 'blur',
      validator: (_rule, value) => {
        if (value !== form.password) return new Error('两次输入的密码不一致')
        return true
      }
    }
  ],
  captcha: [
    {
      trigger: 'blur',
      validator: () => {
        if (needCaptcha.value && !form.captcha.trim()) return new Error('请输入验证码')
        return true
      }
    }
  ]
}

watch(needCaptcha, async (val) => {
  if (val && !captchaId.value) {
    await refreshCaptcha()
  }
})

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const res = await axios.get('/api/captcha', {
      params: { t: Date.now() }
    })
    captchaUrl.value = `/api${res.data.captchaUrl}`
    captchaId.value = res.data.captchaId
  } catch {
    error.value = '验证码加载失败，请稍后再试'
    captchaUrl.value = ''
    captchaId.value = ''
  } finally {
    captchaLoading.value = false
  }
}

function handleClose() {
  visibleProxy.value = false
  resetForm()
}

function resetForm() {
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.captcha = ''
  error.value = ''
  needCaptcha.value = false
  captchaUrl.value = ''
  captchaId.value = ''
  captchaLoading.value = false
  registerFormRef.value?.restoreValidation?.()
}

async function handleSubmit() {
  if (loading.value) return
  error.value = ''

  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  form.username = form.username.trim()
  form.password = form.password.trim()
  form.confirmPassword = form.confirmPassword.trim()
  if (needCaptcha.value) form.captcha = form.captcha.trim()

  if (needCaptcha.value && (!captchaId.value || !captchaUrl.value || captchaLoading.value)) {
    error.value = '验证码加载中，请稍后再试'
    return
  }

  loading.value = true

  try {
    const registerData: Record<string, any> = {
      username: form.username,
      password: form.password
    }

    if (needCaptcha.value) {
      registerData.captcha = form.captcha
      registerData.captchaId = captchaId.value
    }

    const res = await axios.post('/api/user/register', registerData)

    if (res.data.success) {
      successModalVisible.value = true
      visibleProxy.value = false
      resetForm()
      emit('register-success', res.data)
    } else {
      error.value = res.data.message || '注册失败'
      if (res.data.needCaptcha) {
        needCaptcha.value = true
        await refreshCaptcha()
      }
    }
  } catch (e: any) {
    const data = e.response?.data
    if (data?.needCaptcha || data?.code === 'CAPTCHA_ERROR') {
      needCaptcha.value = true
      error.value = data.message || '验证码错误，请重试'
      await refreshCaptcha()
    } else {
      error.value = data?.message || e.message || '注册异常'
    }
  } finally {
    loading.value = false
  }
}

function handleSuccessClose() {
  successModalVisible.value = false
}
</script>