// src/api/tokenService.ts
import axios, { type AxiosError } from 'axios'

interface TokenPair {
  accessToken: string
  refreshToken: string
}

interface ApiErrorResponse {
  success: boolean
  message?: string
  data?: any
}

// 类型保护函数
const isAxiosError = (error: unknown): error is AxiosError => {
  return typeof error === 'object' && error !== null && 'isAxiosError' in error
}

const isError = (error: unknown): error is Error => {
  return error instanceof Error
}

class TokenService {
  isRefreshing = false
  refreshPromise: Promise<string | null> | null = null
  waitingRequests: Array<{
    config: any,
    resolve: (value?: any) => void,
    reject: (reason?: any) => void
  }> = []

  private accessTokenKey = 'token'
  private refreshTokenKey = 'refresh_token'
  private isDev = import.meta.env.DEV

  getToken(): string | null {
    return localStorage.getItem(this.accessTokenKey)
  }

  setToken(token: string) {
    localStorage.setItem(this.accessTokenKey, token)
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  }

  clearToken() {
    localStorage.removeItem(this.accessTokenKey)
    delete axios.defaults.headers.common['Authorization']
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey)
  }

  setRefreshToken(refreshToken: string) {
    localStorage.setItem(this.refreshTokenKey, refreshToken)
  }

  clearRefreshToken() {
    localStorage.removeItem(this.refreshTokenKey)
  }

  setTokenPair(tokenPair: TokenPair) {
    this.setToken(tokenPair.accessToken)
    this.setRefreshToken(tokenPair.refreshToken)
  }

  clearAllTokens() {
    this.clearToken()
    this.clearRefreshToken()
    this.isRefreshing = false
    this.refreshPromise = null
    this.clearWaitingQueue()
  }

  clearBrowserMemoryExceptAuth() {
    try {
      const token = this.getToken()
      const refreshToken = this.getRefreshToken()

      Object.keys(localStorage).forEach(key => {
        if (![this.accessTokenKey, this.refreshTokenKey].includes(key)) {
          localStorage.removeItem(key)
        }
      })

      sessionStorage.clear()
      this.clearNonAuthCookies()

      if (token) this.setToken(token)
      if (refreshToken) this.setRefreshToken(refreshToken)
    } catch (error: unknown) {
      if (this.isDev) {
        console.error('清理浏览器记忆错误:', error)
      }
    }
  }

  private clearNonAuthCookies() {
    try {
      document.cookie.split(';').forEach(cookie => {
        const [name] = cookie.split('=')
        const n = name.trim()
        if (!['token', 'refresh_token', 'auth_session'].some(k => n.includes(k))) {
          document.cookie = `${n}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
          document.cookie = `${n}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${window.location.hostname}`
        }
      })
    } catch (error: unknown) {
      if (this.isDev) {
        console.error('清理cookies失败:', error)
      }
    }
  }

  private async doRefreshToken(): Promise<string | null> {
    this.isRefreshing = true

    try {
      const refreshToken = this.getRefreshToken()
      if (!refreshToken) {
        throw new Error('NO_REFRESH_TOKEN')
      }

      if (this.isDev) {
        console.log('🔄 开始刷新token...')
      }

      const res = await axios.post<ApiErrorResponse>('/api/user/refresh-token', {
        refreshToken
      }, {
        skipAuthRetry: true,
        headers: {
          'Content-Type': 'application/json'
        }
      })

      // 检查响应数据
      if (!res.data.success || !res.data.data?.accessToken) {
        const errorMsg = res.data.message || '刷新token失败'
        throw new Error(`REFRESH_FAILED: ${errorMsg}`)
      }

      const data = res.data.data as TokenPair

      // 更新token
      this.setTokenPair(data)

      // 处理等待队列
      this.processWaitingQueue(data.accessToken)

      if (this.isDev) {
        console.log('✅ Token刷新成功')
      }

      return data.accessToken
    } catch (error: unknown) {
      this.handleRefreshError(error)
      this.clearAllTokens()
      this.clearWaitingQueue(error)
      throw error
    } finally {
      this.isRefreshing = false
    }
  }

  private handleRefreshError(error: unknown) {
    if (!this.isDev) {
      // 生产环境：静默处理，不打印错误信息，提升用户体验
      return
    }

    // 开发环境：打印详细错误信息
    if (isAxiosError(error)) {
      const axiosErr = error as AxiosError<ApiErrorResponse>
      console.warn('🔄 Token刷新失败:', axiosErr.response?.data?.message || axiosErr.message)

      if (axiosErr.response?.status === 401) {
        console.info('ℹ️ Refresh Token已过期，需要重新登录')
      }
    } else if (isError(error)) {
      if (error.message === 'NO_REFRESH_TOKEN') {
        console.info('ℹ️ 没有refresh token，需要重新登录')
      } else if (error.message.startsWith('REFRESH_FAILED:')) {
        console.warn('🔄 Token刷新被拒绝:', error.message.replace('REFRESH_FAILED: ', ''))
      } else {
        console.error('🔄 Token刷新异常:', error.message)
      }
    } else {
      console.error('🔄 Token刷新未知错误:', error)
    }
  }

  async refreshToken(): Promise<string | null> {
    // 如果已经有刷新进程，返回现有的 promise
    if (this.refreshPromise) {
      return this.refreshPromise
    }

    // 创建新的刷新 promise
    this.refreshPromise = this.doRefreshToken()
        .finally(() => {
          this.refreshPromise = null
        })

    return this.refreshPromise
  }

  addToWaitingQueue(config: any): Promise<any> {
    return new Promise((resolve, reject) => {
      this.waitingRequests.push({ config, resolve, reject })
    })
  }

  processWaitingQueue(newToken: string) {
    if (this.isDev && this.waitingRequests.length > 0) {
      console.log(`🔄 重试 ${this.waitingRequests.length} 个等待的请求`)
    }

    this.waitingRequests.forEach(({ config, resolve }) => {
      if (config) {
        config.headers = config.headers || {}
        config.headers['Authorization'] = `Bearer ${newToken}`
        resolve(axios(config))
      } else {
        resolve(newToken)
      }
    })
    this.waitingRequests = []
  }

  clearWaitingQueue(error?: unknown) {
    const errorCount = this.waitingRequests.length

    this.waitingRequests.forEach(({ reject }) => {
      // 生产环境返回用户友好的错误
      const userFriendlyError = new Error('登录已过期，请重新登录')
      reject(this.isDev ? error : userFriendlyError)
    })

    this.waitingRequests = []

    if (this.isDev && errorCount > 0) {
      console.log(`🚫 取消了 ${errorCount} 个等待的请求`)
    }
  }

  // 获取token状态信息（仅开发环境）
  getTokenStatus() {
    if (!this.isDev) return null

    const accessToken = this.getToken()
    const refreshToken = this.getRefreshToken()

    const parseTokenTime = (token: string | null) => {
      if (!token) return null
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        return {
          exp: new Date(payload.exp * 1000),
          iat: new Date(payload.iat * 1000),
          type: payload.type,
          username: payload.sub
        }
      } catch {
        return null
      }
    }

    return {
      hasAccessToken: !!accessToken,
      hasRefreshToken: !!refreshToken,
      accessTokenInfo: parseTokenTime(accessToken),
      refreshTokenInfo: parseTokenTime(refreshToken),
      isRefreshing: this.isRefreshing,
      waitingRequestsCount: this.waitingRequests.length
    }
  }
}

export const tokenService = new TokenService()