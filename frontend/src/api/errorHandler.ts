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
  _isRetry?: boolean
}

interface ApiErrorResponse {
  success: boolean
  code?: string
  message?: string
  data?: any
  needCaptcha?: boolean
}

// 错误类型枚举
enum ErrorType {
  NETWORK = 'NETWORK',
  TOKEN_EXPIRED = 'TOKEN_EXPIRED',
  AUTH_FAILED = 'AUTH_FAILED',
  PERMISSION = 'PERMISSION',
  VALIDATION = 'VALIDATION',
  SERVER = 'SERVER',
  RATE_LIMIT = 'RATE_LIMIT',
  UNKNOWN = 'UNKNOWN'
}

// 用户友好的错误消息映射
const USER_FRIENDLY_MESSAGES: Record<ErrorType, string> = {
  [ErrorType.NETWORK]: '网络连接异常，请检查网络后重试',
  [ErrorType.TOKEN_EXPIRED]: '登录已过期，请重新登录',
  [ErrorType.AUTH_FAILED]: '身份验证失败，请重新登录',
  [ErrorType.PERMISSION]: '您没有权限执行此操作',
  [ErrorType.VALIDATION]: '输入信息有误，请检查后重试',
  [ErrorType.SERVER]: '服务暂时不可用，请稍后重试',
  [ErrorType.RATE_LIMIT]: '操作过于频繁，请稍后重试',
  [ErrorType.UNKNOWN]: '操作失败，请重试'
}

// 特殊端点配置
const SPECIAL_ENDPOINTS = {
  LOGOUT: '/logout',
  LOGIN: '/login',
  REGISTER: '/register',
  REFRESH_TOKEN: '/refresh-token'
} as const

// 全局状态管理
class ErrorHandlerState {
  private static instance: ErrorHandlerState
  private isHandlingAuthError = false
  private authErrorTimeout: ReturnType<typeof setTimeout> | null = null
  private isDev = import.meta.env.DEV

  static getInstance(): ErrorHandlerState {
    if (!ErrorHandlerState.instance) {
      ErrorHandlerState.instance = new ErrorHandlerState()
    }
    return ErrorHandlerState.instance
  }

  get isHandling(): boolean {
    return this.isHandlingAuthError
  }

  get isDevelopment(): boolean {
    return this.isDev
  }

  setHandling(value: boolean): void {
    this.isHandlingAuthError = value
    if (value) {
      this.resetHandlingFlag()
    }
  }

  private resetHandlingFlag(): void {
    if (this.authErrorTimeout) {
      clearTimeout(this.authErrorTimeout)
    }
    this.authErrorTimeout = setTimeout(() => {
      this.isHandlingAuthError = false
      this.authErrorTimeout = null
    }, 2000)
  }

  cleanup(): void {
    this.isHandlingAuthError = false
    if (this.authErrorTimeout) {
      clearTimeout(this.authErrorTimeout)
      this.authErrorTimeout = null
    }
  }
}

export class ErrorHandler {
  private static state = ErrorHandlerState.getInstance()

  /**
   * 处理认证错误
   */
  static async handleAuthError(error: AxiosError): Promise<never> {
    if (this.state.isHandling) {
      return Promise.reject(error)
    }

    this.state.setHandling(true)

    try {
      await this.cleanupAuthState()
      await this.triggerAuthFailedEvents()

      if (this.state.isDevelopment) {
        console.info('🔐 用户认证已过期，已清理状态并显示登录弹窗')
      }
    } catch (processingError: unknown) {
      this.logError('处理认证错误时异常', processingError)
      // 确保登录弹窗仍然显示
      emitter.emit('show-login')
    }

    return Promise.reject(error)
  }

  /**
   * 处理401错误的主入口
   */
  static async handle401Error(
      error: AxiosError<ApiErrorResponse>,
      config: CustomAxiosRequestConfig
  ): Promise<any> {
    const url = config.url

    // 处理特殊端点
    if (url) {
      const specialResult = this.handleSpecialEndpoint(url, error)
      if (specialResult) return specialResult
    }

    // 分类并处理错误
    const errorType = this.classifyError(error)

    switch (errorType) {
      case ErrorType.TOKEN_EXPIRED:
        return this.handleTokenExpiredRequest(error, config)

      case ErrorType.AUTH_FAILED:
      default:
        await this.handleAuthError(error)
        return Promise.reject(error)
    }
  }

  /**
   * 处理特殊端点
   */
  private static handleSpecialEndpoint(url: string, error: AxiosError): Promise<any> | null {
    if (url.includes(SPECIAL_ENDPOINTS.LOGOUT)) {
      // 登出接口总是返回成功，避免用户看到错误
      return Promise.resolve({ data: { success: true } })
    }

    if (url.includes(SPECIAL_ENDPOINTS.LOGIN) ||
        url.includes(SPECIAL_ENDPOINTS.REGISTER)) {
      // 登录注册接口的401错误直接返回，由业务层处理
      return Promise.reject(error)
    }

    if (url.includes(SPECIAL_ENDPOINTS.REFRESH_TOKEN)) {
      // 刷新token失败，触发重新登录
      this.handleTokenRefreshFailed()
      return Promise.reject(error)
    }

    return null // 不是特殊端点
  }

  /**
   * 错误分类
   */
  private static classifyError(error: AxiosError<ApiErrorResponse>): ErrorType {
    const errorCode = error.response?.data?.code
    const status = error.response?.status

    // 优先根据业务错误码分类
    if (errorCode === 'TOKEN_EXPIRED') {
      return ErrorType.TOKEN_EXPIRED
    }

    // 根据HTTP状态码分类
    switch (status) {
      case 401:
        return ErrorType.AUTH_FAILED
      case 403:
        return ErrorType.PERMISSION
      case 422:
        return ErrorType.VALIDATION
      case 429:
        return ErrorType.RATE_LIMIT
      case 500:
      case 502:
      case 503:
      case 504:
        return ErrorType.SERVER
      default:
        return ErrorType.UNKNOWN
    }
  }

  /**
   * 处理Token过期请求
   */
  static async handleTokenExpiredRequest(
      error: AxiosError,
      config: CustomAxiosRequestConfig
  ): Promise<any> {
    // 防止无限循环
    if (config._isRetry || config.skipAuthRetry) {
      await this.handleTokenRefreshFailed()
      return Promise.reject(error)
    }

    config._isRetry = true

    try {
      if (this.state.isDevelopment) {
        console.log('🔄 检测到token过期，尝试自动刷新...')
      }

      const newToken = await tokenService.refreshToken()

      if (newToken) {
        return this.retryRequestWithNewToken(config, newToken)
      } else {
        throw new Error('Token刷新返回空值')
      }
    } catch (refreshError: unknown) {
      this.logError('Token自动刷新失败', refreshError)
      await this.handleTokenRefreshFailed()
      return Promise.reject(error)
    }
  }

  /**
   * 使用新token重试请求
   */
  private static async retryRequestWithNewToken(
      config: CustomAxiosRequestConfig,
      newToken: string
  ): Promise<any> {
    config.headers = config.headers || {}
    config.headers['Authorization'] = `Bearer ${newToken}`

    const axiosInstance = (await import('@/api/axios')).default

    if (this.state.isDevelopment) {
      console.log('✅ Token刷新成功，重试原始请求')
    }

    return axiosInstance(config)
  }

  /**
   * 清理认证状态
   */
  private static async cleanupAuthState(): Promise<void> {
    tokenService.clearAllTokens()
    const userStore = useUserStore()
    await userStore.clearAuth(false)
    cancelPendingRequests('登录已过期')
  }

  /**
   * 触发认证失败相关事件
   */
  private static async triggerAuthFailedEvents(): Promise<void> {
    try {
      const { useAuth } = await this.importUseAuth()
      const { onAuthFailed } = useAuth()
      onAuthFailed()
    } catch (importError: unknown) {
      this.logError('无法导入useAuth，使用备用方案', importError)
    }

    // 显示登录弹窗
    emitter.emit('show-login')
  }

  /**
   * 动态导入useAuth
   */
  static async importUseAuth() {
    const maxRetries = 3
    let lastError: Error | null = null

    for (let i = 0; i < maxRetries; i++) {
      try {
        return await import('@/composables/useAuth')
      } catch (importError: unknown) {
        lastError = importError instanceof Error ? importError : new Error('Import failed')
        if (i < maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, 100 * (i + 1)))
        }
      }
    }

    throw new Error(`导入 useAuth 失败，已重试 ${maxRetries} 次: ${lastError?.message}`)
  }

  /**
   * 处理Token刷新失败（静默处理，不显示错误消息）
   */
  static async handleTokenRefreshFailed(): Promise<void> {
    if (this.state.isDevelopment) {
      console.info('🔐 Token刷新失败，开始清理认证状态...')
    }

    await this.cleanupAuthState()

    // 🔥 静默处理：只显示登录弹窗，不显示错误消息
    setTimeout(() => {
      emitter.emit('show-login')
      if (this.state.isDevelopment) {
        console.info('🔐 已显示登录弹窗，用户需要重新登录')
      }
    }, 100)
  }

  /**
   * 处理网络错误
   */
  static handleNetworkError(): void {
    this.showUserMessage(ErrorType.NETWORK)
  }

  /**
   * 处理其他HTTP错误（确保不处理401）
   */
  static handleOtherErrors(status: number, data?: ApiErrorResponse): void {
    // 🔥 双重保险：确保401错误不会在这里被处理
    if (status === 401) {
      if (this.state.isDevelopment) {
        console.warn('⚠️ 401错误意外进入 handleOtherErrors，已忽略')
      }
      return
    }

    const errorType = this.getErrorTypeByStatus(status)
    const customMessage = data?.message

    // 智能选择错误消息
    if (customMessage && this.isUserFriendlyMessage(customMessage)) {
      this.showCustomMessage(customMessage)
    } else {
      this.showUserMessage(errorType)
    }

    // 开发环境显示详细错误信息
    this.logDetailedError(status, data, errorType)
  }

  /**
   * 静默处理401错误（不显示任何用户消息）
   */
  static handle401Silently(error: AxiosError<ApiErrorResponse>): void {
    if (this.state.isDevelopment) {
      const errorCode = error.response?.data?.code
      const message = error.response?.data?.message
      const url = error.config?.url

      console.group('🔐 401错误静默处理')
      console.log('请求URL:', url)
      console.log('错误码:', errorCode)
      console.log('错误消息:', message)
      console.log('处理方式: 静默处理，不显示用户错误消息')
      console.groupEnd()
    }
    // 🔥 关键：这里不调用任何显示错误消息的方法
    // 401错误的处理完全由 handle401Error 方法负责
    // 这个方法只负责记录日志，不显示用户消息
  }

  /**
   * 根据状态码获取错误类型
   */
  private static getErrorTypeByStatus(status: number): ErrorType {
    switch (status) {
      case 403: return ErrorType.PERMISSION
      case 422: return ErrorType.VALIDATION
      case 429: return ErrorType.RATE_LIMIT
      case 500:
      case 502:
      case 503:
      case 504:
        return ErrorType.SERVER
      default:
        return ErrorType.UNKNOWN
    }
  }

  /**
   * 检查消息是否用户友好
   */
  private static isUserFriendlyMessage(message: string): boolean {
    const technicalTerms = [
      'Exception', 'Error', 'null', 'undefined', 'NullPointer',
      'SQL', 'HTTP', 'Stack', 'Trace', 'Internal'
    ]
    return !technicalTerms.some(term =>
        message.toLowerCase().includes(term.toLowerCase())
    )
  }

  /**
   * 显示用户友好消息
   */
  private static showUserMessage(errorType: ErrorType): void {
    const message = USER_FRIENDLY_MESSAGES[errorType]
    this.showCustomMessage(message)
  }

  /**
   * 显示自定义消息
   */
  private static showCustomMessage(message: string): void {
    emitter.emit('notify', {
      message,
      type: 'error'
    })
  }

  /**
   * 记录详细错误信息（仅开发环境）
   */
  private static logDetailedError(status: number, data?: ApiErrorResponse, errorType?: ErrorType): void {
    if (!this.state.isDevelopment || !data) return

    console.group(`🚨 HTTP ${status} 错误详情`)
    console.log('状态码:', status)
    console.log('错误数据:', data)
    if (errorType) console.log('错误类型:', errorType)
    console.groupEnd()
  }

  /**
   * 记录错误日志
   */
  private static logError(message: string, error: unknown): void {
    if (!this.state.isDevelopment) return

    if (error instanceof Error) {
      console.warn(`❌ ${message}:`, error.message)
    } else {
      console.warn(`❌ ${message}:`, error)
    }
  }

  /**
   * 处理请求重试
   */
  static async handleRetry(
      error: AxiosError,
      config: CustomAxiosRequestConfig
  ): Promise<any> {
    if (!config.retry || config.retry <= 0) {
      return Promise.reject(error)
    }

    config.retry--
    const delay = this.calculateRetryDelay(config)

    if (this.state.isDevelopment) {
      console.log(`🔄 请求重试中... 剩余次数: ${config.retry}, 延迟: ${delay}ms`)
    }

    await this.delay(delay)

    try {
      const axiosInstance = (await import('@/api/axios')).default
      return axiosInstance(config)
    } catch (retryError: unknown) {
      this.logError('重试失败', retryError)
      return Promise.reject(error)
    }
  }

  /**
   * 计算重试延迟（指数退避算法）
   */
  private static calculateRetryDelay(config: CustomAxiosRequestConfig): number {
    const baseDelay = config.retryDelay ?? 1000
    const exponentialDelay = baseDelay * Math.pow(2, (baseDelay / 1000) - (config.retry ?? 0))
    return Math.min(exponentialDelay, 5000) // 最大延迟5秒
  }

  /**
   * 延迟工具方法
   */
  private static delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 清理资源
   */
  static cleanup(): void {
    this.state.cleanup()
  }

  /**
   * 获取状态信息
   */
  static getStatus() {
    return {
      isHandlingAuthError: this.state.isHandling,
      tokenIsRefreshing: tokenService.isRefreshing,
      isDev: this.state.isDevelopment
    }
  }

  /**
   * 开发环境调试信息
   */
  static getDebugInfo() {
    if (!this.state.isDevelopment) return null

    return {
      errorTypes: ErrorType,
      userMessages: USER_FRIENDLY_MESSAGES,
      specialEndpoints: SPECIAL_ENDPOINTS,
      currentStatus: this.getStatus()
    }
  }
}

/**
 * 清理错误处理器
 */
export function cleanupErrorHandler(): void {
  ErrorHandler.cleanup()
}

export { ErrorType, type ApiErrorResponse, type CustomAxiosRequestConfig }