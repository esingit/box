// src/services/tokenService.ts
import axios from 'axios';

class TokenService {
  isRefreshing: boolean;
  waitingRequests: Array<{
    config: any,
    resolve: (value?: any) => void,
    reject: (reason?: any) => void
  }>;

  clearToken() {
    localStorage.removeItem('token');
    delete axios.defaults.headers.common['Authorization'];
  }

  constructor() {
    this.isRefreshing = false;
    this.waitingRequests = [];
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  setToken(token: string) {
    localStorage.setItem('token', token);
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  }

  async refreshToken(): Promise<string | null> {
    try {
      const token = this.getToken();
      if (!token) {
        throw new Error('No token found');
      }

      const { useUserStore } = await import('@/store/userStore');
      const userStore = useUserStore();

      if (await userStore.verifyToken()) {
        return this.getToken();
      }
      throw new Error('Token refresh failed');
    } catch (error) {
      console.error('刷新token失败:', error);
      throw error;
    }
  }

  addToWaitingQueue(config: any): Promise<any> {
    return new Promise((resolve, reject) => {
      this.waitingRequests.push({ config, resolve, reject });
    });
  }

  processWaitingQueue(newToken: string) {
    this.waitingRequests.forEach(({ config, resolve }) => {
      config.headers['Authorization'] = `Bearer ${newToken}`;
      resolve(axios(config));
    });
    this.waitingRequests = [];
  }

  clearWaitingQueue() {
    this.waitingRequests = [];
  }
}

export const tokenService = new TokenService();
