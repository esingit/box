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