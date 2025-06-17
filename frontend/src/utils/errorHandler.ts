import emitter from './eventBus'
import { axiosConfig } from './axiosConfig'
import axiosInstance from '@/utils/axios';
import { tokenService } from './tokenService'
import { cancelPendingRequests } from './requestManager'
import { useUserStore } from '@/store/userStore'

export class ErrorHandler {
  static async handleAuthError(error) {
    tokenService.isRefreshing = false
    tokenService.clearWaitingQueue()

    const userStore = useUserStore()
    await userStore.logout(false)
    cancelPendingRequests('登录已过期')

    const { nextTick } = await import('vue')
    await nextTick()

    const { useAuth } = await import('@/composable/useAuth')
    const { showLogin } = useAuth()
    showLogin('登录已过期，请重新登录')

    return Promise.reject(error)
  }

  static async handle401Error(error, config) {
    if (config.url.includes('/logout')) {
      return Promise.resolve({ data: { success: true } })
    }

    if (config.url.includes('/login') || config.skipAuthRetry) {
      return Promise.reject(error)
    }

    try {
      if (!tokenService.isRefreshing) {
        tokenService.isRefreshing = true
        const newToken = await tokenService.refreshToken()

        if (newToken) {
          config.headers['Authorization'] = `Bearer ${newToken}`
          tokenService.processWaitingQueue(newToken)
          tokenService.isRefreshing = false
          return axios(config)
        }

        return this.handleAuthError(error, userStore)
      }

      return tokenService.addToWaitingQueue(config)
    } catch (refreshError) {
      return this.handleAuthError(refreshError, userStore)
    }
  }

  static handleNetworkError() {
    emitter.emit('notify', '网络连接失败，请检查网络设置', 'error')
  }

  static handleOtherErrors(status, data) {
    if (status === 403) {
      emitter.emit('notify', '没有权限执行此操作', 'error')
    } else if (status === 422) {
      const message = data?.message || '输入数据验证失败'
      emitter.emit('notify', message, 'error')
    } else if (status >= 500) {
      emitter.emit('notify', '服务器暂时无法响应，请稍后重试', 'error')
    } else {
      const message = data?.message || '请求失败，请重试'
      emitter.emit('notify', message, 'error')
    }
  }

  static async handleRetry(error, config) {
    if (config.retry && axiosConfig.shouldRetry(error)) {
      config.retry--
      await new Promise(resolve => setTimeout(resolve, axiosConfig.retryDelay))
      return axios(config)
    }
    return Promise.reject(error)
  }
}
