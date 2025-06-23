// src/api/errorHandler.ts
import emitter from '@/utils/eventBus'
import axiosInstance from '@/api/axios'
import { AxiosError, AxiosRequestConfig } from 'axios'
import { tokenService } from '@/api/tokenService'
import { cancelPendingRequests } from '@/api/requestManager'
import { useUserStore } from '@/store/userStore'

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  skipAuthRetry?: boolean
  retry?: number
}

export class ErrorHandler {
  static async handleAuthError(error: AxiosError): Promise<never> {
    tokenService.isRefreshing = false
    tokenService.clearWaitingQueue()
    tokenService.clearToken()

    const userStore = useUserStore()
    await userStore.logout()

    cancelPendingRequests('登录已过期')

    // 弹出登录弹窗，不跳转
    const { nextTick } = await import('vue')
    await nextTick()

    const { useAuth } = await import('@/composable/useAuth')
    const { showLogin } = useAuth()

    showLogin()

    return Promise.reject(error)
  }

  static async handle401Error(error: AxiosError, config: CustomAxiosRequestConfig): Promise<any> {
    if (config.url?.includes('/logout')) {
      return Promise.resolve({ data: { success: true } })
    }

    if (config.url?.includes('/login') || config.skipAuthRetry) {
      return Promise.reject(error)
    }

    try {
      if (!tokenService.isRefreshing) {
        tokenService.isRefreshing = true
        const newToken = await tokenService.refreshToken()

        if (newToken) {
          config.headers = config.headers || {}
          config.headers['Authorization'] = `Bearer ${newToken}`
          tokenService.processWaitingQueue(newToken)
          tokenService.isRefreshing = false
          return axiosInstance(config)
        }

        return this.handleAuthError(error)
      }

      return tokenService.addToWaitingQueue(config)
    } catch (refreshError) {
      return this.handleAuthError(refreshError as AxiosError)
    }
  }

  static handleNetworkError(): void {
    emitter.emit('notify', { message: '网络连接失败，请检查网络设置', type: 'error' })
  }

  static handleOtherErrors(status: number, data?: any): void {
    if (status === 403) {
      emitter.emit('notify', { message: '没有权限执行此操作', type: 'error' })
    } else if (status === 422) {
      const message = data?.message || '输入数据验证失败'
      emitter.emit('notify', { message, type: 'error' })
    } else if (status >= 500) {
      emitter.emit('notify', { message: '服务器暂时无法响应，请稍后重试', type: 'error' })
    } else {
      const message = data?.message || '请求失败，请重试'
      emitter.emit('notify', { message, type: 'error' })
    }
  }

  static async handleRetry(error: AxiosError, config: CustomAxiosRequestConfig): Promise<any> {
    if (config.retry && config.retry > 0) {
      config.retry--
      const delay = config.retryDelay ?? 1000
      await new Promise(resolve => setTimeout(resolve, delay))
      return axiosInstance(config)
    }
    return Promise.reject(error)
  }
}
