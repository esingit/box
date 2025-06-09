import { ref, computed } from 'vue';
import { useUserStore } from '../stores/userStore';
import emitter from '../utils/eventBus';

const API_URL = '/api/user';
const TOKEN_CHECK_INTERVAL = 300000; // 每5分钟检查一次token状态

export function useAuth() {
  const userStore = useUserStore();
  const isShowingLoginModal = ref(false);
  const isShowingRegisterModal = ref(false);
  const pendingAuthAction = ref(null);
  const pendingAuthMessage = ref(null);
  const tokenCheckTimer = ref(null);
  
  const isLoggedIn = computed(() => userStore.isLoggedIn);
  
  // 检查并刷新token状态
  async function checkTokenStatus() {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        await userStore.logout(false);
        return false;
      }

      const axios = (await import('../utils/axios')).default;
      const response = await axios.get(`${API_URL}/verify-token`);
      
      if (response.data.success) {
        // 如果token接近过期，尝试刷新
        if (response.data.shouldRefresh) {
          const refreshResult = await axios.post(`${API_URL}/refresh-token`);
          if (refreshResult.data.success) {
            const newToken = refreshResult.data.data;
            localStorage.setItem('token', newToken);
            axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
          }
        }
        return true;
      }
      return false;
    } catch (error) {
      console.error('Token验证失败:', error);
      await userStore.logout(false);
      return false;
    }
  }

  // 启动定期检查token
  function startTokenCheck() {
    if (tokenCheckTimer.value) {
      clearInterval(tokenCheckTimer.value);
    }
    tokenCheckTimer.value = setInterval(checkTokenStatus, TOKEN_CHECK_INTERVAL);
  }

  // 停止token检查
  function stopTokenCheck() {
    if (tokenCheckTimer.value) {
      clearInterval(tokenCheckTimer.value);
      tokenCheckTimer.value = null;
    }
  }
  
  // 检查登录状态
  async function checkAuthState() {
    const token = localStorage.getItem('token');
    if (!token) {
      await userStore.logout(false);
      return false;
    }
    
    const isValid = await checkTokenStatus();
    if (isValid) {
      startTokenCheck();
    }
    return isValid;
  }
  
  // 显示登录框
  const showLogin = (message = '', callback = null) => {
    if (!isShowingLoginModal.value) {
      console.log('显示登录框，设置回调函数');
      
      // 保存消息和回调（先设置回调，再显示模态框）
      pendingAuthMessage.value = message;
      if (callback !== null && callback !== undefined) {
        console.log('设置登录后的回调函数');
        pendingAuthAction.value = () => {
          try {
            callback();
          } catch (error) {
            console.error('执行回调函数时出错:', error);
          }
        };
      } else {
        console.log('无回调函数需要设置');
        pendingAuthAction.value = null;
      }
      
      // 确保消息显示
      if (message) {
        console.log('显示登录提示消息:', message);
        emitter.emit('login-error', message);
      }
      
      // 显示登录框
      isShowingLoginModal.value = true;
      console.log('登录框已显示');
      
      // 通知其他组件更新状态
      emitter.emit('auth-state-changed', false);
    } else {
      console.log('登录框已经显示，不重复显示');
    }
  };
  
  const hideLogin = () => {
    console.log('隐藏登录框');
    isShowingLoginModal.value = false;
    // 清理待执行的操作和消息
    pendingAuthAction.value = null;
    pendingAuthMessage.value = null;
  };

  // 显示注册框
  const showRegister = () => {
    if (!isShowingRegisterModal.value) {
      isShowingRegisterModal.value = true;
    }
  };

  const hideRegister = () => {
    isShowingRegisterModal.value = false;
  };

  // 初始化认证状态
  async function initAuth() {
    const isValid = await checkAuthState();
    if (isValid) {
      startTokenCheck();
    }
    return isValid;
  }

  // 组件卸载时清理
  function cleanup() {
    stopTokenCheck();
  }

  return {
    isShowingLoginModal,
    isShowingRegisterModal,
    pendingAuthAction,
    pendingAuthMessage,
    isLoggedIn,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    checkAuthState,
    initAuth,
    cleanup
  };
}

// 导出初始化函数供 App.vue 使用
export function initializeAuth() {
  const auth = useAuth();
  return auth.initAuth();
}
