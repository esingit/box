<template>
  <div class="auth-form-modal-overlay" @click.self="handleClose">
    <div :class="['auth-form-modal', { 'show-captcha': showCaptcha }]">
      <div class="auth-form-header">
        <h2 class="auth-form-title">登录</h2>
        <button class="auth-form-close" @click="handleClose">×</button>
      </div>
      <div class="auth-form-divider"></div>
      <div class="auth-form-inner">
        <form @submit.prevent="submit">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="username" required autocomplete="username" />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input type="password" v-model="password" required autocomplete="current-password" />
          </div>
          <div class="form-group" v-if="showCaptcha">
            <label>验证码</label>
            <div class="captcha-container">
              <input v-model="captcha" required class="captcha-input" />
              <img :src="captchaUrl" @click="refreshCaptcha" class="captcha-image" alt="验证码" />
            </div>
          </div>
          <div class="form-submit">
            <p v-if="error" class="error-msg">{{ error }}</p>
            <button type="submit" class="btn btn-black" :disabled="isLoading">
              {{ isLoading ? '登录中...' : '登录' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import emitter from '@/utils/eventBus.js'
import { useRouter } from 'vue-router'

const emit = defineEmits(['close', 'login-success'])

const username = ref('')
const password = ref('')
const error = ref(null)
const isLoading = ref(false)
const userStore = useUserStore()
const router = useRouter()

// 监听错误消息
onMounted(() => {
  emitter.on('login-error', (message) => {
    error.value = message;
  });
});

onUnmounted(() => {
  emitter.off('login-error');
});

function handleClose() {
  // 只关闭弹窗，不清除用户状态
  emit('close')
}

const captcha = ref('')
const captchaId = ref('')
const captchaUrl = ref('')
const showCaptcha = ref(false);
async function fetchCaptchaAndId() {
  try {
    const response = await fetch('/api/captcha')
    const data = await response.json()
    
    if (data.captchaImage) {
      captchaUrl.value = `data:image/png;base64,${data.captchaImage}`;
    } else if (data.captchaUrl) {
      const imagePath = data.captchaUrl.startsWith('/') ? data.captchaUrl : `/${data.captchaUrl}`;
      captchaUrl.value = `/api${imagePath}`; 
    } else {
      captchaUrl.value = ''; 
    }
    captchaId.value = data.captchaId

  } catch (error) {
    console.error('获取验证码异常:', error)
    captchaUrl.value = '';
  }
}

watch(showCaptcha, async (newValue) => {
  if (newValue) {
    error.value = null;
    if (!captchaUrl.value) {
      await fetchCaptchaAndId();
    }
  } else {
    captcha.value = '';
    captchaId.value = '';
    captchaUrl.value = '';
  }
});

async function refreshCaptcha() {
  error.value = null;
  await fetchCaptchaAndId();
}

async function submit() {
  error.value = null;
  isLoading.value = true;
  try {
    let payload = {
      username: username.value,
      password: password.value,
    };
    if (showCaptcha.value) {
      if (!captcha.value) {
        error.value = '请输入验证码';
        isLoading.value = false;
        return;
      }
      payload.captchaId = captchaId.value;
      payload.captcha = captcha.value;
    }
    const response = await userStore.login(payload);
    if (response.success) {
      emitter.emit('notify', '登录成功', 'success');
      // 发送登录成功事件，并传递新的 token，确保和 axios.js 中的事件名一致
      emitter.emit('loginSuccess', response.token);
      // 发送数据刷新事件
      emitter.emit('refresh-data');
      // 关闭登录弹窗
      emit('close');
      emit('login-success');
      // 清空表单
      showCaptcha.value = false;
      username.value = '';
      password.value = '';
      captcha.value = '';
      captchaUrl.value = '';
      error.value = null;
    } else {
      error.value = response.message || '用户名或密码错误';
      if (response.showCaptcha) {
         showCaptcha.value = true;
         await fetchCaptchaAndId();
         captcha.value = '';
      } else {
         showCaptcha.value = false;
      }
    }
  } catch (err) {
    error.value = err.message || '登录失败，请稍后重试';
    if (showCaptcha.value) {
        await fetchCaptchaAndId();
    }
  } finally {
    isLoading.value = false;
  }
}
</script>