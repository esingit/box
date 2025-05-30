<template>
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
    <button type="submit" class="btn" :disabled="isLoading">
      {{ isLoading ? '登录中...' : '登录' }}
    </button>
    <p v-if="error" class="error-msg">{{ error }}</p>
  </form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useUserStore } from '../stores/userStore'
import { emitter } from '../utils/eventBus'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const error = ref(null)
const isLoading = ref(false)
const userStore = useUserStore()
const router = useRouter()

// --- 添加的验证码相关状态 --- START
const captcha = ref('')
const captchaId = ref('')
const captchaUrl = ref('')
const showCaptcha = ref(false); // 控制是否显示验证码
// --- 添加的验证码相关状态 --- END

// --- 添加的获取和刷新验证码方法 --- START
async function fetchCaptchaAndId() {
  try {
    const response = await fetch('/api/captcha')
    const data = await response.json()
    
    if (data.captchaImage) {
      // If backend provides base64 image directly
      captchaUrl.value = `data:image/png;base64,${data.captchaImage}`;
    } else if (data.captchaUrl) {
      const imagePath = data.captchaUrl.startsWith('/') ? data.captchaUrl : `/${data.captchaUrl}`;
      captchaUrl.value = `/api${imagePath}`; 
    } else {
      // Fallback or error
      captchaUrl.value = ''; 
    }
    captchaId.value = data.captchaId

  } catch (error) {
    console.error('获取验证码异常:', error)
    captchaUrl.value = '' // Clear captcha on error
  }
}

watch(showCaptcha, async (newValue) => {
  if (newValue && !captchaUrl.value) { // Only fetch if showing and no captcha yet
    await fetchCaptchaAndId()
  }
})

async function refreshCaptcha() {
  await fetchCaptchaAndId()
}
// --- 添加的获取和刷新验证码方法 --- END

async function submit() {
  error.value = null
  isLoading.value = true
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

    const response = await userStore.login(payload)

    if (response.success) {
      emitter.emit('login-success')
      showCaptcha.value = false;
      // Clear form fields on successful login
      username.value = '';
      password.value = '';
      captcha.value = '';
      captchaUrl.value = ''; // Clear captcha image
      error.value = null;
      router.push('/')
    } else {
      error.value = response.message || '用户名或密码错误'
      if (response.showCaptcha) {
         showCaptcha.value = true;
         // Always refresh captcha when backend asks for it
         await fetchCaptchaAndId();
         captcha.value = ''; // Clear previous captcha input
      } else {
         showCaptcha.value = false;
      }
    }
  } catch (err) {
    error.value = err.message || '登录失败，请稍后重试'
    // It might be good to show captcha on generic errors too,
    // or at least ensure it's refreshed if already visible.
    if (showCaptcha.value) {
        await fetchCaptchaAndId();
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.form-group {
  margin-bottom: 15px;
}
label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
}
input {
  width: 100%;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
  font-size: 1rem;
  box-sizing: border-box;
}

.captcha-container {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-input {
  flex: 1;
  width: auto !important;
}

.captcha-image {
  height: 38px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.btn {
  width: 100%;
  padding: 10px 0;
  background: #3b82f6;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s;
}
.btn:hover:not(:disabled) {
  background: #2563eb;
}
.btn:disabled {
  background: #93c5fd;
  cursor: not-allowed;
}
.error-msg {
  margin-top: 12px;
  color: #f43f5e;
  font-weight: 600;
}
</style>