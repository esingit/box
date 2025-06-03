<template>
  <div class="auth-form-inner">
    <h2 class="menu-title">注册</h2>
    <form @submit.prevent="submit">
      <div class="form-group">
        <label>用户名</label>
        <input v-model="username" required autocomplete="username" class="input" />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input type="password" v-model="password" required autocomplete="new-password" class="input" />
      </div>
      <div class="form-group" v-if="showCaptcha">
        <label>验证码</label>
        <div class="captcha-container">
          <input v-model="captcha" required class="input captcha-input" />
          <img :src="captchaUrl" @click="refreshCaptcha" class="captcha-image" alt="验证码" />
        </div>
      </div>
      <div class="form-submit">
        <p v-if="error" class="error-msg">{{ error }}</p>
        <p v-if="success" class="success-msg">{{ success }}</p>
        <button type="submit" class="btn btn-black" :disabled="isLoading">
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useUserStore } from '../stores/userStore'
import emitter from '../utils/eventBus.js'

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

async function submit() {
  error.value = null;
  success.value = null;
  isLoading.value = true;
  try {
    let payload = {
      username: username.value,
      password: password.value,
      captcha: showCaptcha.value ? captcha.value : "",
      captchaId: showCaptcha.value ? captchaId.value : ""
    };
    const response = await userStore.register(payload);
    console.log("后端注册响应:", response);
    if (response.success) {
      success.value = response.message || '注册成功';
      emitter.emit('notify', '注册成功', 'success');
      showCaptcha.value = false;
      setTimeout(() => {
      }, 1000);
    } else {
      console.log("准备设置错误信息，后端返回的 message:", response.message);
      if (response.message) {
        error.value = `${response.message}`;
      } else {
        error.value = '注册失败，请稍后重试';
      }
      console.log("设置后的错误信息:", error.value);

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