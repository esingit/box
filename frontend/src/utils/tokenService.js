import axios from './axios'

class TokenService {
  constructor() {
    this.isRefreshing = false
    this.waitingRequests = []
  }

  getToken() {
    return localStorage.getItem('token')
  }

  setToken(token) {
    localStorage.setItem('token', token)
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  }

  async refreshToken() {
    try {
      const token = this.getToken();
      if (!token) {
        throw new Error('No token found');
      }

      const { useUserStore } = await import('@/stores/userStore');
      const userStore = useUserStore();

      // 使用统一的token验证方法
      if (await userStore.verifyToken()) {
        // verifyToken 已经处理了刷新逻辑
        return this.getToken(); // 返回可能已经更新的token
      }
      
      throw new Error('Token refresh failed');
    } catch (error) {
      console.error('刷新token失败:', error);
      throw error;
    }
  }

  addToWaitingQueue(config) {
    return new Promise((resolve, reject) => {
      this.waitingRequests.push({ config, resolve, reject })
    })
  }

  processWaitingQueue(newToken) {
    this.waitingRequests.forEach(({ config, resolve }) => {
      config.headers['Authorization'] = `Bearer ${newToken}`
      resolve(axios(config))
    })
    this.waitingRequests = []
  }

  clearWaitingQueue() {
    this.waitingRequests = []
  }
}

export const tokenService = new TokenService()
