<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h3 class="modal-title">欢迎回来</h3>
        <button class="close-button" @click="$emit('close')">
          <LucideX />
        </button>
      </div>

      <form class="modal-body" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="flex items-center">
            <LucideUser :size="16" class="input-icon" />
            用户名
          </label>
          <input 
            class="input"
            type="text" 
            v-model="form.username" 
            placeholder="请输入用户名"
            :disabled="loading"
            required
          >
        </div>

        <div class="form-group">
          <label class="flex items-center">
            <LucideLock :size="16" class="input-icon" />
            密码
          </label>
          <input 
            class="input"
            type="password" 
            v-model="form.password" 
            placeholder="请输入密码"
            :disabled="loading"
            required
          >
        </div>

        <div v-if="showCaptcha" class="form-group">
          <label class="flex items-center">
            <LucideShieldCheck :size="16" class="input-icon" />
            验证码
          </label>
          <div class="captcha-group">
            <input 
              class="input"
              type="text" 
              v-model="form.captcha"
              placeholder="请输入验证码"
              :disabled="loading"
              required
            >
            <img 
              v-if="captchaUrl" 
              :src="captchaUrl" 
              @click="refreshCaptcha"
              @error="handleImageError"
              alt="验证码"
              class="captcha-image"
            >
          </div>
        </div>

        <p v-if="error" class="error-text">{{ error }}</p>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <LucideLogIn v-if="!loading" :size="16" class="btn-icon" />
          <LucideLoader2 v-else :size="16" class="btn-icon animate-spin" />
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { 
  LucideX, 
  LucideUser, 
  LucideLock, 
  LucideShieldCheck, 
  LucideLogIn, 
  LucideLoader2 
} from 'lucide-vue-next';
import { useUserStore } from '../stores/userStore';
import axios from '../utils/axios';

const emit = defineEmits(['close', 'login-success']);

const userStore = useUserStore();
const loading = ref(false);
const error = ref('');
const showCaptcha = ref(false);
const captchaUrl = ref('');
const captchaId = ref('');

const form = reactive({
  username: '',
  password: '',
  captcha: '',
});

// 处理验证码图片加载失败
const handleImageError = async (e) => {
  console.error('验证码图片加载失败');
  error.value = '验证码加载失败，正在重试...';
  await refreshCaptcha();
};

// 监听验证码状态变化
watch(showCaptcha, async (newValue) => {
  if (newValue) {
    error.value = '';
    await refreshCaptcha(); // 每次显示验证码时都刷新
  } else {
    form.captcha = '';
    captchaId.value = '';
    captchaUrl.value = '';
  }
});

// 移除初始化检查验证码，改为在登录失败时检查

async function handleSubmit() {
  if (loading.value) return;
  
  error.value = '';
  loading.value = true;
  
  try {
    const loginData = {
      username: form.username,
      password: form.password
    };
    
    if (showCaptcha.value) {
      if (!form.captcha) {
        error.value = '请输入验证码';
        loading.value = false;
        return;
      }
      loginData.captcha = form.captcha;
      loginData.captchaId = captchaId.value;
    }
    
    const res = await userStore.login(loginData);
    
    if (res.success) {
      emit('login-success');
      emit('close');
    } else {
      error.value = res.message;
      // 检查是否需要显示验证码
      if (res.needCaptcha || 
          res.message?.includes('验证码') || 
          res.code === 'NEED_CAPTCHA' ||
          (res.message && res.message.toLowerCase().includes('captcha'))) {
        showCaptcha.value = true;
        await refreshCaptcha();
        form.captcha = '';
      }
    }
  } catch (err) {
    console.error('登录失败:', err);
    const errorResponse = err.response?.data;
    
    // 处理错误响应
    if (errorResponse) {
      error.value = errorResponse.message || '登录失败，请重试';
      if (errorResponse.needCaptcha || 
          errorResponse.message?.includes('验证码') ||
          errorResponse.code === 'NEED_CAPTCHA') {
        showCaptcha.value = true;
        await refreshCaptcha();
        form.captcha = '';
      }
    } else {
      error.value = err.message || '登录失败，请重试';
    }
  } finally {
    loading.value = false;
  }
}

async function refreshCaptcha() {
  try {
    console.log('开始获取验证码...');
    const res = await axios.get('/api/captcha');
    console.log('验证码原始响应:', res);

    // 检查响应状态
    if (!res || !res.data) {
      throw new Error('验证码接口返回数据为空');
    }

    console.log('验证码响应数据:', res.data);

    // 获取验证码数据
    const responseData = res.data;
    
    if (!responseData.captchaId || !responseData.captchaUrl) {
      throw new Error('验证码数据格式不正确');
    }

    // 设置验证码URL和ID
    captchaUrl.value = `/api${responseData.captchaUrl}`;
    captchaId.value = responseData.captchaId;
    console.log('验证码URL已设置:', captchaUrl.value);
    console.log('验证码ID已设置:', captchaId.value);

  } catch (err) {
    console.error('获取验证码详细错误:', err);
    // 检查是否是网络错误
    if (err.code === 'ECONNABORTED' || !navigator.onLine) {
      error.value = '网络连接失败，请检查网络后重试';
    } else if (err.response) {
      // 服务器返回错误
      error.value = err.response.data?.message || '服务器错误，请稍后重试';
    } else {
      // 其他错误
      error.value = err.message || '获取验证码失败，请刷新重试';
    }
    
    // 清理验证码状态
    captchaUrl.value = '';
    captchaId.value = '';
  }
}
</script>