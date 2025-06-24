// src/api/tokenService.ts
import axios from 'axios'

class TokenService {
  isRefreshing: boolean
  waitingRequests: Array<{
    config: any,
    resolve: (value?: any) => void,
    reject: (reason?: any) => void
  }>

  constructor() {
    this.isRefreshing = false
    this.waitingRequests = []
  }

  getToken(): string | null {
    return localStorage.getItem('token')
  }

  setToken(token: string) {
    localStorage.setItem('token', token)
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  }

  clearToken() {
    localStorage.removeItem('token')
    delete axios.defaults.headers.common['Authorization']
    this.isRefreshing = false
    this.clearWaitingQueue()
  }

  // 🔥 新增：清除浏览器记忆但保留重要数据
  clearBrowserMemoryExceptAuth() {
    try {
      console.log('🟡 开始清理浏览器记忆，保留认证相关数据')

      // 保存需要保留的重要数据
      const importantData = {
        token: this.getToken(),
      }

      // 清除 localStorage（除了重要数据）
      const localStorageKeysToKeep = ['token']
      const localStorageKeys = Object.keys(localStorage)
      localStorageKeys.forEach(key => {
        if (!localStorageKeysToKeep.includes(key)) {
          localStorage.removeItem(key)
        }
      })

      // 清除 sessionStorage
      sessionStorage.clear()

      // 清除除了认证相关的 cookies
      this.clearNonAuthCookies()

      // 恢复重要数据
      if (importantData.token) {
        this.setToken(importantData.token)
      }

      console.log('🟢 浏览器记忆清理完成，已保留认证数据')
    } catch (error) {
      console.error('🔴 清理浏览器记忆时出错:', error)
    }
  }

  // 🔥 新增：清除非认证相关的cookies
  private clearNonAuthCookies() {
    try {
      const cookies = document.cookie.split(';')
      const authRelatedCookies = ['token', 'refresh_token', 'auth_session'] // 定义需要保留的认证相关cookie名称

      cookies.forEach(cookie => {
        const [name] = cookie.split('=')
        const cookieName = name.trim()

        // 如果不是认证相关的cookie，则清除
        if (!authRelatedCookies.some(authCookie => cookieName.includes(authCookie))) {
          // 清除cookie的标准方法
          document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
          document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${window.location.hostname}`
        }
      })
    } catch (error) {
      console.error('清理cookies时出错:', error)
    }
  }

  async refreshToken(): Promise<string | null> {
    try {
      const token = this.getToken()
      if (!token) {
        throw new Error('No token found')
      }

      const { useUserStore } = await import('@/store/userStore')
      const userStore = useUserStore()

      const success = await userStore.refreshToken()
      if (success) {
        return this.getToken()
      }
      throw new Error('Token refresh failed')
    } catch (error) {
      console.error('刷新token失败:', error)
      this.clearToken()
      throw error
    }
  }

  addToWaitingQueue(config: any): Promise<any> {
    return new Promise((resolve, reject) => {
      this.waitingRequests.push({ config, resolve, reject })
    })
  }

  processWaitingQueue(newToken: string) {
    this.waitingRequests.forEach(({ config, resolve }) => {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${newToken}`
      resolve(axios(config))
    })
    this.waitingRequests = []
  }

  clearWaitingQueue() {
    this.waitingRequests.forEach(({ reject }) => {
      reject(new Error('Token refresh failed'))
    })
    this.waitingRequests = []
  }
}

export const tokenService = new TokenService()