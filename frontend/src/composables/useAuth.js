import { ref, computed } from 'vue';
import { useUserStore } from '../stores/userStore';
import emitter from '../utils/eventBus';

const API_URL = '/api/user';

export function useAuth() {
  const userStore = useUserStore();
  const isShowingLoginModal = ref(false);
  const isShowingRegisterModal = ref(false);
  const pendingAuthAction = ref(null);
  const pendingAuthMessage = ref(null);
  const tokenCheckTimer = ref(null);
  
  const isLoggedIn = computed(() => userStore.isLoggedIn);
  
  // 检查并刷新token状态，使用统一的验证方法
  async function checkTokenStatus() {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        await userStore.logout(false);
        return false;
      }
      
      // 使用 userStore 的统一验证方法
      return await userStore.verifyToken();
    } catch (error) {
      console.error('Token验证失败:', error);
      await userStore.logout(false);
      return false;
    }
  }

  // 启动动态token检查
  function startTokenCheck() {
    if (tokenCheckTimer.value) {
      clearTimeout(tokenCheckTimer.value);
    }
    
    const token = localStorage.getItem('token');
    if (token) {
      try {
        // 解析token以获取过期时间
        const tokenData = JSON.parse(atob(token.split('.')[1]));
        const expirationTime = tokenData.exp * 1000;
        const currentTime = Date.now();
        const timeUntilExpiry = expirationTime - currentTime;
        const refreshThreshold = 10 * 60 * 1000; // 10分钟阈值
        
        // 计算下次检查时间，为了性能考虑，最小间隔设为1分钟
        const nextCheckTime = Math.max(60000, Math.min(timeUntilExpiry - refreshThreshold, 300000));
        
        // 设置定时器
        tokenCheckTimer.value = setTimeout(async () => {
          const isValid = await checkTokenStatus();
          if (isValid) {
            startTokenCheck(); // 重新安排下次检查
          }
        }, nextCheckTime);
      } catch (error) {
        console.error('Token解析失败:', error);
        userStore.logout(false);
      }
    }
  }

  // 停止token检查
  function stopTokenCheck() {
    if (tokenCheckTimer.value) {
      clearTimeout(tokenCheckTimer.value);
      tokenCheckTimer.value = null;
    }
  }
  
  // 检查登录状态并启动token检查
  async function checkAuthState() {
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
    return await checkAuthState();
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
