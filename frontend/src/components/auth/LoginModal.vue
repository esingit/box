<!-- src/components/auth/LoginModal.vue -->
<template>
  <BaseModal :visible="visible" title="欢迎回来" @update:visible="close" width="500px">
    <Form :validation-schema="schema" v-slot="{ handleSubmit }">
      <form @submit.prevent="handleSubmit(onSubmit)" class="space-y-4" autocomplete="on">
        <!-- 用户名 -->
        <div>
          <label class="modal-label">用户名</label>
          <Field name="username" v-slot="{ field }">
            <BaseInput
                :model-value="field.value"
                @update:model-value="field.onChange"
                placeholder="请输入用户名"
                autocomplete="username"
                clearable
                :disabled="loading"
            />
          </Field>
          <ErrorMessage name="username" class="msg-error" />
        </div>

        <!-- 密码 -->
        <div>
          <label class="modal-label">密码</label>
          <Field name="password" v-slot="{ field }">
            <BaseInput
                :model-value="field.value"
                @update:model-value="field.onChange"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
                clearable
                :disabled="loading"
            />
          </Field>
          <ErrorMessage name="password" class="msg-error" />
        </div>

        <!-- 验证码 -->
        <div v-if="needCaptcha">
          <label class="modal-label">验证码</label>
          <div class="flex items-center gap-2">
            <Field name="captcha" v-slot="{ field }">
              <BaseInput
                  :model-value="field.value"
                  @update:model-value="field.onChange"
                  placeholder="请输入验证码"
                  autocomplete="off"
                  clearable
                  :disabled="loading"
              />
            </Field>
            <img
                :src="captchaUrl"
                @click="refreshCaptcha"
                class="h-10 cursor-pointer border rounded"
                :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
                alt="图片验证码"
            />
          </div>
          <ErrorMessage name="captcha" class="msg-error" />
        </div>

        <!-- 错误提示 -->
        <p v-if="error" class="msg-error">{{ error }}</p>

        <!-- 登录按钮 -->
        <BaseButton type="submit" color="primary" :icon="LucideLogIn" :disabled="loading" class="w-full">
          {{ loading ? '登录中...' : '登录' }}
        </BaseButton>
      </form>
    </Form>

    <!-- 底部切换 -->
    <template #footer>
      <div class="mt-4 text-center text-sm">
        没有账号？
        <button
            @click="switchToRegister"
            class="msg-strong hover:underline focus:outline-none"
            :disabled="loading"
        >
          立即注册
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Form, Field, ErrorMessage, type SubmissionHandler } from 'vee-validate'
import * as yup from 'yup'
import { LucideLogIn } from 'lucide-vue-next'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composables/useAuth'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const userStore = useUserStore()
const { onLoginSuccess } = useAuth()

const props = defineProps<{
  visible: boolean
}>()

// 🔥 明确定义 emits 避免 Vue 警告
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'switch-to-register': []
  'login-success': []
}>()

const visible = computed({
  get: () => props.visible,
  set: val => emit('update:visible', val)
})

const loading = ref(false)
const error = ref('')
const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)

const schema = yup.object({
  username: yup.string().min(4, '用户名至少4位').required('请输入用户名'),
  password: yup
      .string()
      .required('请输入密码')
      .min(6, '密码至少6位')
      .matches(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/, '密码必须包含数字和字母'),
  captcha: yup.string().when([], {
    is: () => needCaptcha.value,
    then: schema => schema.required('请输入验证码'),
    otherwise: schema => schema.notRequired()
  })
})

watch(needCaptcha, async val => {
  if (val && !captchaUrl.value) {
    await refreshCaptcha()
  } else if (!val) {
    clearCaptcha()
  }
})

function clearCaptcha() {
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

function close() {
  visible.value = false
  setTimeout(() => {
    error.value = ''
    clearCaptcha()
    needCaptcha.value = false
  }, 300)
}

function switchToRegister() {
  close()
  emit('switch-to-register')
}

const onSubmit: SubmissionHandler = async (values) => {
  if (loading.value) return
  error.value = ''
  loading.value = true

  try {
    const credentials: Record<string, any> = {
      username: values.username.trim(),
      password: values.password.trim()
    }

    if (needCaptcha.value) {
      credentials.captcha = values.captcha?.trim()
      credentials.captchaId = captchaId.value
    }

    const res = await userStore.login(credentials)

    if (res.success) {
      console.log('🟢 LoginModal: 登录成功，调用 onLoginSuccess')

      // 🔥 使用 useAuth 的统一登录成功处理逻辑
      await onLoginSuccess()

      // 发出登录成功事件给父组件
      emit('login-success')

      // 关闭弹窗
      close()
    } else {
      error.value = res.message || '登录失败'
      if (res.needCaptcha) {
        needCaptcha.value = true
        await refreshCaptcha()
      }
    }
  } catch (e: any) {
    console.error('LoginModal: 登录过程中出错:', e)
    error.value = e.message || '登录异常'
  } finally {
    loading.value = false
  }
}
</script>