<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h3 class="modal-title">创建新账号</h3>
        <button class="close-button" @click="$emit('close')">
          <LucideX />
        </button>
      </div>

      <form class="modal-body" @submit.prevent="submit">
        <div class="form-group">
          <label class="flex items-center">
            <LucideUser :size="16" class="input-icon" />
            用户名
          </label>
          <input
            class="input"
            type="text"
            v-model="username"
            required
            autocomplete="username"
            placeholder="4-16位字母、数字或下划线"
            :disabled="isLoading"
          />
          <small class="help-text">仅支持字母、数字和下划线，长度4-16位</small>
        </div>

        <div class="form-group">
          <label class="flex items-center">
            <LucideLock :size="16" class="input-icon" />
            密码
          </label>
          <input
            class="input"
            type="password"
            v-model="password"
            required
            autocomplete="new-password"
            placeholder="8-20位字母数字组合"
            :disabled="isLoading"
          />
          <small class="help-text">8-20位，必须包含字母和数字，可包含特殊字符(@$!%*#?&)</small>
        </div>

        <div v-if="showCaptcha" class="form-group">
          <label class="flex items-center">
            <LucideShieldCheck :size="16" class="input-icon" />
            验证码
          </label>
          <div class="flex gap-2">
            <input 
              class="input"
              type="text"
              v-model="captcha"
              placeholder="请输入验证码"
              :disabled="isLoading"
              required
            />
            <img 
              v-if="captchaUrl"
              :src="captchaUrl"
              @click="refreshCaptcha"
              alt="验证码"
              class="captcha-image"
            />
          </div>
        </div>

        <p v-if="error" class="error-text">{{ error }}</p>
        <p v-if="success" class="success-text">{{ success }}</p>

        <button type="submit" class="btn btn-primary w-full" :disabled="isLoading">
          <LucideUserPlus v-if="!isLoading" :size="16" class="btn-icon" />
          <LucideLoader2 v-else :size="16" class="btn-icon animate-spin" />
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/userStore'
import emitter from '@/utils/eventBus.js'
import { 
  LucideX,
  LucideUser, 
  LucideLock, 
  LucideShieldCheck,
  LucideUserPlus, 
  LucideLoader2 
} from 'lucide-vue-next'

const emit = defineEmits(['close'])
const username = ref('')
const password = ref('')
const captcha = ref('')
const captchaId = ref('')
const captchaUrl = ref('')
const error = ref(null)
const success = ref(null)
const isLoading = ref(false)
const userStore = useUserStore()
const showCaptcha = ref(false)

watch(showCaptcha, async (newValue) => {
  if (newValue) {
    error.value = null;
    success.value = null;
    if (!captchaUrl.value) {
      await fetchCaptchaAndId();
    }
  } else {
    captcha.value = '';
    captchaId.value = '';
    captchaUrl.value = '';
  }
});

async function fetchCaptchaAndId() {
  try {
    const response = await fetch('/api/captcha')
    const data = await response.json()

    if (data.captchaImage) {
      captchaUrl.value = `data:image/png;base64,${data.captchaImage}`;
    } else if (data.captchaUrl) {
      captchaUrl.value = `/api${data.captchaUrl}`;
    } else {
      captchaUrl.value = '';
    }
    captchaId.value = data.captchaId

  } catch (error) {
    console.error('获取验证码异常:', error)
    captchaUrl.value = ''
  }
}

async function refreshCaptcha() {
  error.value = null;
  success.value = null;
  await fetchCaptchaAndId()
}

function validateForm() {
  // 用户名验证：4-16位字母、数字、下划线
  const usernameRegex = /^[a-zA-Z0-9_]{4,16}$/;
  if (!usernameRegex.test(username.value)) {
    error.value = '用户名必须为4-16位字母、数字或下划线';
    return false;
  }
  
  // 密码验证：8-20位，必须包含字母和数字，可以包含特殊字符
  const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,20}$/;
  if (!passwordRegex.test(password.value)) {
    error.value = '密码必须为8-20位，包含字母和数字';
    return false;
  }

  return true;
}

async function submit() {
  error.value = null;
  success.value = null;

  if (!validateForm()) {
    return;
  }

  isLoading.value = true;
  try {
    let payload = {
      username: username.value,
      password: password.value,
      captcha: showCaptcha.value ? captcha.value : "",
      captchaId: showCaptcha.value ? captchaId.value : ""
    };
    const response = await userStore.register(payload);
    
    if (response.success) {
      success.value = response.message || '注册成功';
      emitter.emit('notify', '注册成功', 'success');
      showCaptcha.value = false;
      setTimeout(() => {
        emit('close');
      }, 1000);
    } else {
      if (response.message) {
        error.value = `${response.message}`;
      } else {
        error.value = '注册失败，请稍后重试';
      }

      if (response.showCaptcha || (response.message && response.message.includes('验证码'))) {
        showCaptcha.value = true;
        await fetchCaptchaAndId();
        captcha.value = '';
      } else {
        showCaptcha.value = false;
      }
    }
  } catch (err) {
    console.error('注册请求异常:', err);

    if (err.response && err.response.data) {
      error.value = err.response.data.message || '注册失败';
      if (err.response.data.showCaptcha || (err.response.data.message && err.response.data.message.includes('验证码'))) {
        showCaptcha.value = true;
        await fetchCaptchaAndId();
        captcha.value = '';
      }
    } else {
      error.value = err.message || '注册失败，请稍后重试';
    }
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  fetchCaptchaAndId()
})
</script>