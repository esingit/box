<!-- components/auth/LoginModalWithCaptcha.vue -->
<template>
  <BaseModal :visible="visible" title="欢迎回来" @update:visible="close">
    <form @submit.prevent="handleSubmit" class="space-y-4" autocomplete="off">
      <div>
        <label class="block text-sm font-medium mb-1">用户名</label>
        <input
            v-model="form.username"
            type="text"
            class="input-base"
            placeholder="请输入用户名"
            autocomplete="username"
        />
      </div>

      <div>
        <label class="block text-sm font-medium mb-1">密码</label>
        <input
            v-model="form.password"
            type="password"
            class="input-base"
            placeholder="请输入密码"
            autocomplete="current-password"
        />
      </div>

      <div v-if="needCaptcha">
        <label class="block text-sm font-medium mb-1">验证码</label>
        <div class="flex items-center gap-2">
          <input
              v-model="form.captcha"
              type="text"
              class="input-base"
              placeholder="请输入验证码"
              autocomplete="off"
          />
          <img
              :src="captchaUrl"
              @click="refreshCaptcha"
              class="h-10 cursor-pointer border rounded"
              :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
          />
        </div>
      </div>

      <p v-if="error" class="msg-error">{{ error }}</p>

      <button
          type="submit"
          class="btn-primary w-full"
          :disabled="loading"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>

    <template #footer>
      <div class="mt-4 text-center text-sm">
        没有账号？
        <button
            @click="$emit('switch-to-register')"
            class="msg-strong hover:underline focus:outline-none"
        >
          立即注册
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useUserStore } from '@/store/userStore'
import BaseModal from '@/components/base/BaseModal.vue'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'login-success', 'switch-to-register'])

const visible = computed({
  get: () => props.visible,
  set: val => emit('update:visible', val)
})

const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

const loading = ref(false)
const error = ref('')
const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)

const userStore = useUserStore()

watch(needCaptcha, async val => {
  if (val && !captchaUrl.value) {
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
  captchaLoading.value = true
  try {
    const captchaData = await userStore.fetchCaptcha()
    if (captchaData) {
      captchaUrl.value = captchaData.imageUrl
      captchaId.value = captchaData.captchaId
    } else {
      error.value = '验证码加载失败，请稍后重试'
    }
  } catch {
    error.value = '验证码加载失败，请稍后重试'
  } finally {
    captchaLoading.value = false
  }
}

function resetForm() {
  form.username = ''
  form.password = ''
  form.captcha = ''
  error.value = ''
  loading.value = false
  needCaptcha.value = false
  clearCaptcha()
}

function close() {
  visible.value = false
  setTimeout(() => resetForm(), 300)
}

async function handleSubmit() {
  if (loading.value) return
  error.value = ''

  if (!form.username.trim() || form.username.length < 4) {
    error.value = '用户名至少4位'
    return
  }
  if (!form.password || form.password.length < 6) {
    error.value = '密码至少6位'
    return
  }
  if (needCaptcha.value && !form.captcha.trim()) {
    error.value = '请输入验证码'
    return
  }

  loading.value = true
  try {
    const credentials: Record<string, any> = {
      username: form.username.trim(),
      password: form.password.trim()
    }
    if (needCaptcha.value) {
      credentials.captcha = form.captcha.trim()
      credentials.captchaId = captchaId.value
    }

    const res = await userStore.login(credentials)

    if (res.success) {
      emit('login-success')
      close()
    } else {
      error.value = res.message || '登录失败'
      if (res.needCaptcha) {
        needCaptcha.value = true
        await refreshCaptcha()
      }
    }
  } catch (e: any) {
    error.value = e.message || '登录异常'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.input {
  @apply w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm;
}
</style>
