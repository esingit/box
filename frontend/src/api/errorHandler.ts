// src/api/errorHandler.ts
import emitter from '@/utils/eventBus'
import { AxiosError, AxiosRequestConfig } from 'axios'
import { tokenService } from '@/api/tokenService'
import { cancelPendingRequests } from '@/api/requestManager'
import { useUserStore } from '@/store/userStore'

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  skipAuthRetry?: boolean
  retry?: number
  retryDelay?: number
}

// 🔥 添加状态管理，避免重复处理
let isHandlingAuthError = false
let authErrorTimeout: ReturnType<typeof setTimeout> | null = null

export class ErrorHandler {
  static async handleAuthError(error: AxiosError): Promise<never> {
    // 🔥 防止重复处理认证错误
    if (isHandlingAuthError) {
      console.log('🟡 认证错误正在处理中，跳过重复处理')
      return Promise.reject(error)
    }

    isHandlingAuthError = true
    console.log('🔴 handleAuthError called')

    try {
      // 清理相关状态
      tokenService.isRefreshing = false
      tokenService.clearWaitingQueue()
      tokenService.clearToken()

      const userStore = useUserStore()
      await userStore.clearAuth(false)

      cancelPendingRequests('登录已过期')

      // 使用 useAuth 清理状态并弹出登录弹窗
      await this.triggerAuthFailedWithAuth()

      console.log('🔴 发送 show-login 事件')
      emitter.emit('show-login')

    } catch (processingError) {
      console.error('🔴 处理认证错误时出现异常:', processingError)
    } finally {
      // 🔥 延迟重置状态，避免快速重复触发
      if (authErrorTimeout) {
        clearTimeout(authErrorTimeout)
      }
      authErrorTimeout = setTimeout(() => {
        isHandlingAuthError = false
        authErrorTimeout = null
      }, 2000) // 2秒后允许重新处理
    }

    return Promise.reject(error)
  }

  static async handle401Error(error: AxiosError, config: CustomAxiosRequestConfig): Promise<any> {
    console.log('🟡 401 Error detected:', config.url)

    // 特殊处理：logout接口直接返回成功
    if (config.url?.includes('/logout')) {
      console.log('🟡 logout 接口，直接返回成功')
      return Promise.resolve({ data: { success: true } })
    }

    // 特殊处理：login接口直接拒绝
    if (config.url?.includes('/login') || config.skipAuthRetry) {
      console.log('🟡 login 接口或 skipAuthRetry，直接拒绝')
      return Promise.reject(error)
    }

    // 🔥 关键修复：refresh-token接口401时直接弹出登录弹窗
    if (config.url?.includes('/refresh-token')) {
      console.log('🔴 refresh-token 接口401，直接弹出登录弹窗')
      await this.handleTokenRefreshFailed()
      return Promise.reject(error)
    }

    // 🔥 检查并处理认证请求
    return this.handleAuthenticatedRequest(error, config)
  }

  // 🔥 新增：专门处理需要认证的请求
  static async handleAuthenticatedRequest(error: AxiosError, config: CustomAxiosRequestConfig): Promise<any> {
    try {
      const { useAuth } = await this.importUseAuth()
      const { shouldRejectRequest, addPendingRetryRequest, retryAllPendingRequestsWithToken } = useAuth()

      // 检查是否应该直接拒绝请求
      if (shouldRejectRequest()) {
        console.log('🔴 认证已失败，直接拒绝请求:', config.url)
        return Promise.reject(new Error('用户未登录，请先登录'))
      }

      // 将请求包装成Promise并添加到待重试队列
      return new Promise((resolve, reject) => {
        console.log('🟡 添加待重试请求到队列')

        try {
          addPendingRetryRequest(config, resolve, reject)

          // 🔥 如果当前没有在刷新token，开始刷新流程
          if (!tokenService.isRefreshing) {
            this.startTokenRefreshProcess(retryAllPendingRequestsWithToken)
          } else {
            console.log('🟡 token 正在刷新中，等待...')
          }
        } catch (addError) {
          console.error('🔴 添加待重试请求失败:', addError)
          reject(addError)
        }
      })

    } catch (importError) {
      console.error('🔴 导入 useAuth 失败:', importError)
      return Promise.reject(error)
    }
  }

  // 🔥 新增：开始token刷新流程
  static startTokenRefreshProcess(retryAllPendingRequestsWithToken: (token: string) => Promise<void>) {
    console.log('🟡 开始刷新 token')
    tokenService.isRefreshing = true

    // 🔥 检查是否有token再尝试刷新
    const currentToken = tokenService.getToken()
    if (!currentToken) {
      console.log('🔴 没有token，直接弹出登录弹窗')
      tokenService.isRefreshing = false
      this.handleTokenRefreshFailed()
      return
    }

    tokenService.refreshToken()
        .then(async (newToken) => {
          if (newToken) {
            console.log('🟢 token 刷新成功，重试请求')
            try {
              // 刷新成功，使用 useAuth 重试所有待重试的请求
              await retryAllPendingRequestsWithToken(newToken)
            } catch (retryError) {
              console.error('🔴 重试请求时出错:', retryError)
              // 即使重试失败，也不触发登录弹窗，让各个请求自己处理错误
            }
          } else {
            console.log('🔴 token 刷新失败，弹出登录弹窗')
            await this.handleTokenRefreshFailed()
          }
        })
        .catch(async (refreshError) => {
          console.log('🔴 token 刷新异常:', refreshError)
          await this.handleTokenRefreshFailed()
        })
        .finally(() => {
          tokenService.isRefreshing = false
        })
  }

  // 🔥 优化：安全的动态导入 useAuth
  static async importUseAuth() {
    const maxRetries = 3
    let lastError: Error | null = null

    for (let i = 0; i < maxRetries; i++) {
      try {
        const authModule = await import('@/composables/useAuth')
        return authModule
      } catch (importError) {
        lastError = importError as Error
        console.warn(`🟡 导入 useAuth 失败 (尝试 ${i + 1}/${maxRetries}):`, importError)

        // 等待一段时间后重试
        if (i < maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, 100 * (i + 1)))
        }
      }
    }

    throw new Error(`导入 useAuth 失败，已重试 ${maxRetries} 次: ${lastError?.message}`)
  }

  // 🔥 优化：触发认证失败处理
  static async triggerAuthFailedWithAuth() {
    try {
      const { useAuth } = await this.importUseAuth()
      const { onAuthFailed } = useAuth()
      onAuthFailed()
    } catch (importError) {
      console.error('🔴 导入 useAuth 失败，使用备用方案:', importError)
      // 🔥 备用方案：直接发送事件
      emitter.emit('show-login')
    }
  }

  // token刷新失败，弹出登录弹窗
  static async handleTokenRefreshFailed() {
    console.log('🔴 handleTokenRefreshFailed called')

    // 🔥 确保状态正确重置
    tokenService.clearToken()
    tokenService.isRefreshing = false

    // 使用 useAuth 处理认证失败
    await this.triggerAuthFailedWithAuth()

    // 🔥 延迟发送登录事件，确保状态已正确设置
    setTimeout(() => {
      console.log('🔴 发送 show-login 事件')
      emitter.emit('show-login')
    }, 100)
  }

  static handleNetworkError(): void {
    emitter.emit('notify', {
      message: '网络连接失败，请检查网络设置',
      type: 'error'
    })
  }

  static handleOtherErrors(status: number, data?: any): void {
    let message = '请求失败，请重试'

    switch (status) {
      case 403:
        message = '没有权限执行此操作'
        break
      case 404:
        message = '请求的资源不存在'
        break
      case 422:
        message = data?.message || '输入数据验证失败'
        break
      case 429:
        message = '请求过于频繁，请稍后重试'
        break
      case 500:
        message = '服务器内部错误'
        break
      case 502:
        message = '服务器网关错误'
        break
      case 503:
        message = '服务暂时不可用'
        break
      case 504:
        message = '服务器响应超时'
        break
      default:
        if (status >= 500) {
          message = '服务器暂时无法响应，请稍后重试'
        } else if (data?.message) {
          message = data.message
        }
    }

    emitter.emit('notify', { message, type: 'error' })
  }

  static async handleRetry(error: AxiosError, config: CustomAxiosRequestConfig): Promise<any> {
    if (!config.retry || config.retry <= 0) {
      return Promise.reject(error)
    }

    config.retry--
    const delay = config.retryDelay ?? 1000

    console.log(`🟡 重试请求 ${config.url}，剩余重试次数: ${config.retry}`)

    // 🔥 指数退避策略
    const actualDelay = delay * Math.pow(2, (config.retryDelay ?? 1000) / 1000 - config.retry)
    await new Promise(resolve => setTimeout(resolve, Math.min(actualDelay, 5000)))

    try {
      // 重新导入 axios 实例避免循环依赖
      const axiosInstance = (await import('@/api/axios')).default
      return axiosInstance(config)
    } catch (importError) {
      console.error('🔴 导入 axios 实例失败:', importError)
      return Promise.reject(error)
    }
  }

  // 🔥 新增：清理方法，用于重置状态
  static cleanup() {
    isHandlingAuthError = false
    if (authErrorTimeout) {
      clearTimeout(authErrorTimeout)
      authErrorTimeout = null
    }
  }

  // 🔥 新增：获取当前处理状态
  static getStatus() {
    return {
      isHandlingAuthError,
      tokenIsRefreshing: tokenService.isRefreshing
    }
  }
}

// 🔥 导出清理函数供应用关闭时调用
export function cleanupErrorHandler() {
  ErrorHandler.cleanup()
}