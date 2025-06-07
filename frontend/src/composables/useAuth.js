import { ref } from 'vue';
import { useUserStore } from '../stores/userStore';
import emitter from '../utils/eventBus';

const API_URL = '/api/user';

// 创建一个全局单例来管理登录状态
const isShowingLoginModal = ref(false);
const isShowingRegisterModal = ref(false);

// 防抖函数
function debounce(fn, delay = 300) {
  let timer = null;
  return function (...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

export function useAuth() {
  const userStore = useUserStore();
  
  // 检查登录状态
  function checkAuthState() {
    const token = localStorage.getItem('token');
    if (!token) {
      userStore.logout();
      return false;
    }
    return true;
  }

  // 记录最后一次需要登录权限的操作
  const lastRequiredAuthAction = ref(null);
  
  // 显示登录框
  const showLogin = debounce((message, requiredAuthAction = null) => {
    if (!isShowingLoginModal.value) {
      isShowingLoginModal.value = true;
      
      // 确保消息显示
      if (message) {
        setTimeout(() => {
          emitter.emit('login-error', message);
        }, 100);
      }
      
      // 记录需要登录权限的操作
      if (requiredAuthAction) {
        lastRequiredAuthAction.value = requiredAuthAction;
      }
      
      // 触发显示登录模态框的事件
      emitter.emit('show-login-modal', message);
      
      // 通知其他组件更新状态
      emitter.emit('auth-state-changed', false);
    }
  }, 300);

  function hideLogin() {
    isShowingLoginModal.value = false;
  }

  // 显示注册框
  const showRegister = debounce(() => {
    if (!isShowingRegisterModal.value) {
      isShowingRegisterModal.value = true;
    }
  }, 300);

  function hideRegister() {
    isShowingRegisterModal.value = false;
  }

  // 初始化认证状态
  async function initAuth() {
    // 检查登录状态
    if (!checkAuthState()) {
      return false;
    }

    try {
      const token = localStorage.getItem('token');
      if (token) {
        const axios = (await import('../utils/axios')).default;
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        // 验证 token 有效性
        await axios.get(`${API_URL}/verify-token`);
        return true;
      }
    } catch (error) {
      console.error('Token 验证失败:', error);
      userStore.logout(false); // 不清除 UI 状态，只更新登录状态
      return false;
    }
    return false;
  }

  return {
    isShowingLoginModal,
    isShowingRegisterModal,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    checkAuthState,
    initAuth
  };
}

// 导出初始化函数供 App.vue 使用
export function initializeAuth() {
  const { initAuth } = useAuth();
  initAuth();
}
