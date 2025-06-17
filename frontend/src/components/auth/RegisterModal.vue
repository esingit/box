<template>
  <BaseModal :visible="visible" title="注册新用户" @update:visible="close">
    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div>
        <label class="modal-label">用户名</label>
        <input
            v-model="form.username"
            type="text"
            class="input-base"
            placeholder="请输入用户名"
            autocomplete="username"
        />
      </div>

      <div>
        <label class="modal-label">密码</label>
        <input
            v-model="form.password"
            type="password"
            class="input-base"
            placeholder="请输入密码"
            autocomplete="new-password"
        />
      </div>

      <div>
        <label class="modal-label">确认密码</label>
        <input
            v-model="form.confirmPassword"
            type="password"
            class="input-base"
            placeholder="请再次输入密码"
            autocomplete="new-password"
        />
      </div>

      <div v-if="needCaptcha">
        <label class="modal-label">验证码</label>
        <div class="flex items-center gap-2">
          <input
              v-model="form.captcha"
              type="text"
              class="input-base"
              placeholder="请输入验证码"
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
        {{ loading ? '注册中...' : '注册' }}
      </button>
    </form>

    <template #footer>
      <div class="mt-4 text-center text-sm">
        已有账号？
        <button
            @click="$emit('switch-to-login')"
            class="msg-strong hover:underline focus:outline-none"
        >
          立即登录
        </button>
      </div>
    </template>
  </BaseModal>

  <div
      v-if="successModalVisible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
  >
    <div
        class="bg-white rounded-xl shadow-lg p-6 w-full max-w-sm text-center"
    >
      <h3 class="text-lg font-semibold mb-4">注册成功</h3>
      <p class="mb-6">恭喜您，注册成功！请使用您的账号登录。</p>
      <button
          @click="handleSuccessClose"
          class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded"
      >
        确定
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useUserStore } from '@/store/userStore'
import BaseModal from '@/components/base/BaseModal.vue'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'register-success', 'switch-to-login'])

const visible = computed({
  get: () => props.visible,
  set: val => emit('update:visible', val)
})

const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const loading = ref(false)
const error = ref('')
const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)
const successModalVisible = ref(false)

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

function resetForm() {
  Object.assign(form, {
    username: '',
    password: '',
    confirmPassword: '',
    captcha: ''
  })
  error.value = ''
  loading.value = false
  needCaptcha.value = false
  clearCaptcha()
}

function close() {
  visible.value = false
  setTimeout(() => resetForm(), 300)
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const captchaData = await userStore.fetchCaptcha()
    if (captchaData) {
      captchaUrl.value = captchaData.imageUrl
      captchaId.value = captchaData.captchaId
    } else {
      error.value = '验证码加载失败，请稍后再试'
    }
  } catch {
    error.value = '验证码加载失败，请稍后再试'
  } finally {
    captchaLoading.value = false
  }
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
  if (form.password !== form.confirmPassword) {
    error.value = '两次密码不一致'
    return
  }
  if (needCaptcha.value && !form.captcha.trim()) {
    error.value = '请输入验证码'
    return
  }

  loading.value = true
  try {
    const data: Record<string, any> = {
      username: form.username.trim(),
      password: form.password.trim()
    }
    if (needCaptcha.value) {
      data.captcha = form.captcha.trim()
      data.captchaId = captchaId.value
    }

    const res = await userStore.register(data)

    if (res.success) {
      // 注册成功弹窗
      successModalVisible.value = true

      // 通知父组件
      emit('register-success', res.data)

      close()
    } else {
      error.value = res.message || '注册失败'
      if (res.needCaptcha) {
        needCaptcha.value = true
        await refreshCaptcha()
      }
    }
  } catch (e: any) {
    const err = e.response?.data
    if (err?.needCaptcha || err?.code === 'CAPTCHA_ERROR') {
      needCaptcha.value = true
      error.value = err.message || '验证码错误'
      await refreshCaptcha()
    } else {
      error.value = e.message || '注册异常'
    }
  } finally {
    loading.value = false
  }
}

function handleSuccessClose() {
  successModalVisible.value = false
}
</script>

<style scoped>
.input {
  @apply w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm;
}
</style>
