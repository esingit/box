<template>
  <BaseModal :visible="visible" title="注册新用户" @update:visible="close" width="500px">
    <form @submit.prevent="onSubmit" class="space-y-4">
      <div>
        <label class="modal-label">用户名</label>
        <BaseInput v-model="username" placeholder="请输入用户名" />
        <p v-if="errors.username" class="msg-error">{{ errors.username }}</p>
      </div>

      <div>
        <label class="modal-label">密码</label>
        <BaseInput v-model="password" type="password" placeholder="请输入密码" />
        <p v-if="errors.password" class="msg-error">{{ errors.password }}</p>
      </div>

      <div>
        <label class="modal-label">确认密码</label>
        <BaseInput v-model="confirmPassword" type="password" placeholder="请再次输入密码" />
        <p v-if="errors.confirmPassword" class="msg-error">{{ errors.confirmPassword }}</p>
      </div>

      <div v-if="needCaptcha">
        <label class="modal-label">验证码</label>
        <div class="flex items-center gap-2">
          <BaseInput v-model="captcha" placeholder="请输入验证码" />
          <img
              :src="captchaUrl"
              @click="refreshCaptcha"
              class="h-10 cursor-pointer border rounded"
              :title="captchaLoading ? '加载中...' : '点击刷新验证码'"
              alt="图片验证码"
          />
        </div>
        <p v-if="errors.captcha" class="msg-error">{{ errors.captcha }}</p>
      </div>

      <p v-if="error" class="msg-error">{{ error }}</p>

      <BaseButton type="submit" color="primary" :icon="LucideUserPlus" :disabled="loading" class="w-full">
        {{ loading ? '注册中...' : '注册' }}
      </BaseButton>
    </form>

    <template #footer>
      <div class="mt-4 text-center text-sm">
        已有账号？
        <button @click="$emit('switch-to-login')" class="msg-strong hover:underline focus:outline-none">
          立即登录
        </button>
      </div>
    </template>
  </BaseModal>

  <!-- 注册成功弹窗 -->
  <div v-if="successModalVisible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
    <div class="bg-white rounded-xl shadow-lg p-6 w-full max-w-sm text-center">
      <h3 class="text-lg font-semibold mb-4">注册成功</h3>
      <p class="mb-6">恭喜您，注册成功！请使用您的账号登录。</p>
      <BaseButton type="button" title="立即登录" @click="handleSuccessClose" color="primary" class="w-full"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useForm, useField } from 'vee-validate'
import * as yup from 'yup'
import { LucideUserPlus } from 'lucide-vue-next'
import { useUserStore } from '@/store/userStore'
import BaseModal from '@/components/base/BaseModal.vue'
import BaseInput from '@/components/base/BaseInput.vue'

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'switch-to-login'): void
}>()

const props = defineProps<{ visible: boolean }>()

const visible = computed<boolean>({
  get: () => props.visible,
  set: val => emit('update:visible', val)
})

const userStore = useUserStore()
const loading = ref(false)
const error = ref('')
const successModalVisible = ref(false)

const needCaptcha = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')
const captchaLoading = ref(false)

const schema = yup.object({
  username: yup.string().min(4, '用户名至少4位').required('请输入用户名'),
  password: yup
      .string()
      .min(6, '密码至少6位')
      .matches(/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/, '密码必须包含字母和数字')
      .required('请输入密码'),
  confirmPassword: yup
      .string()
      .oneOf([yup.ref('password')], '两次密码不一致')
      .required('请确认密码'),
  captcha: yup.string().when([], {
    is: () => needCaptcha.value,
    then: s => s.required('请输入验证码'),
    otherwise: s => s.notRequired()
  })
})

const { handleSubmit, resetForm, errors } = useForm({ validationSchema: schema })

const { value: username } = useField<string>('username')
const { value: password } = useField<string>('password')
const { value: confirmPassword } = useField<string>('confirmPassword')
const { value: captcha } = useField<string>('captcha')

watch(needCaptcha, async val => {
  if (val && !captchaUrl.value) await refreshCaptcha()
  else if (!val) clearCaptcha()
})

function clearCaptcha() {
  captcha.value = ''
  captchaUrl.value = ''
  captchaId.value = ''
}

function close() {
  visible.value = false
  setTimeout(() => {
    resetForm()
    error.value = ''
    loading.value = false
    needCaptcha.value = false
    clearCaptcha()
  }, 300)
}

async function refreshCaptcha() {
  captchaLoading.value = true
  try {
    const res = await userStore.fetchCaptcha()
    if (res) {
      captchaUrl.value = res.imageUrl
      captchaId.value = res.captchaId
    } else {
      error.value = '验证码加载失败'
    }
  } catch {
    error.value = '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

const onSubmit = handleSubmit(async values => {
  if (loading.value) return
  error.value = ''
  loading.value = true

  try {
    const data: Record<string, any> = {
      username: values.username.trim(),
      password: values.password.trim()
    }
    if (needCaptcha.value) {
      data.captcha = values.captcha.trim()
      data.captchaId = captchaId.value
    }

    const res = await userStore.register(data)

    if (res.success) {
      successModalVisible.value = true
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
})

function handleSuccessClose() {
  successModalVisible.value = false
  emit('switch-to-login')
}
</script>
