// src/api/axios.ts
import axios, { AxiosError, AxiosHeaders, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ALLOWED_DUPLICATE_ENDPOINTS, axiosConfig } from '@/api/axiosConfig'
import { generateRequestKey, requestManager } from '@/api/requestManager'
import { tokenService } from '@/api/tokenService'
import { ErrorHandler } from '@/api/errorHandler'

// 🔥 添加 API 错误响应类型定义
interface ApiErrorResponse {
    success: boolean
    code?: string
    message?: string
    data?: any
    needCaptcha?: boolean
}

type CustomRequestConfig = InternalAxiosRequestConfig & {
    allowDuplicate?: boolean
    skipAuthRetry?: boolean
    retry?: number
    retryDelay?: number
    signal?: AbortSignal
    _isRetry?: boolean
}

const AUTH_WHITELIST: string[] = [
    '/api/user/login',
    '/api/user/register',
    '/api/captcha',
    '/api/user/refresh-token',
]

const instance = axios.create(axiosConfig)

instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const customConfig = config as CustomRequestConfig
        const requestKey = generateRequestKey(customConfig)

        const allowDuplicate = customConfig.allowDuplicate ||
            ALLOWED_DUPLICATE_ENDPOINTS.some(endpoint => customConfig.url?.includes(endpoint))

        if (!allowDuplicate) {
            if (requestManager.has(requestKey)) {
                const previous = requestManager.get(requestKey)
                previous?.abort('取消重复请求')
                requestManager.delete(requestKey)
            }
            const controller = new AbortController()
            customConfig.signal = controller.signal
            requestManager.add(requestKey, controller)
        }

        // 解析请求路径判断是否是白名单接口
        const rawUrl = customConfig.url || ''
        const baseURL = customConfig.baseURL || location.origin
        const fullUrl = new URL(rawUrl, baseURL)
        const requestPath = fullUrl.pathname

        const isAuthEndpoint = AUTH_WHITELIST.some(endpoint => requestPath.startsWith(endpoint))

        if (!isAuthEndpoint) {
            // 非白名单接口注入 accessToken
            const token = tokenService.getToken()
            if (!customConfig.headers) {
                customConfig.headers = new AxiosHeaders()
            }
            if (token) {
                customConfig.headers.set('Authorization', `Bearer ${token}`)
            }
        }

        return customConfig
    },
    error => Promise.reject(error)
)

instance.interceptors.response.use(
    (response: AxiosResponse) => {
        requestManager.delete(generateRequestKey(response.config as CustomRequestConfig))
        return response
    },
    async (error: unknown) => {
        if (axios.isAxiosError(error)) {
            // 🔥 正确类型化 AxiosError
            const axiosErr = error as AxiosError<ApiErrorResponse>
            const config = axiosErr.config as CustomRequestConfig | undefined

            if (config) {
                requestManager.delete(generateRequestKey(config))
            }

            if (axiosErr.code === 'ERR_CANCELED') {
                return Promise.reject(axiosErr)
            }

            const response = axiosErr.response
            if (!response) {
                ErrorHandler.handleNetworkError()
                return Promise.reject(axiosErr)
            }

            // 🔥 401错误完全静默处理 - 修复config可能为undefined的警告
            if (response.status === 401) {
                // 确保config存在才调用处理函数
                if (config) {
                    return handle401ErrorSilently(axiosErr, config)
                } else {
                    // config为undefined时的备用处理
                    ErrorHandler.handle401Silently(axiosErr)
                    return Promise.reject(axiosErr)
                }
            }

            // 🔥 只有非401错误才显示错误消息给用户
            ErrorHandler.handleOtherErrors(response.status, response.data as ApiErrorResponse)
            return ErrorHandler.handleRetry(axiosErr, config!)
        }

        return Promise.reject(error)
    }
)

// 🔥 401错误的静默处理方法
async function handle401ErrorSilently(
    axiosErr: AxiosError<ApiErrorResponse>,
    config: CustomRequestConfig
): Promise<AxiosResponse | never> {
    // 调用静默处理方法，记录日志但不显示错误消息
    ErrorHandler.handle401Silently(axiosErr)

    // 🔥 检查用户是否正在注销过程中
    try {
        // 动态导入避免循环依赖
        const { useUserStore } = await import('@/store/userStore')
        const userStore = useUserStore()

        if (userStore.isLoggingOut) {
            if (import.meta.env.DEV) {
                console.log('🟡 [401Handler] 用户正在注销，跳过401处理逻辑')
            }
            return createAuthRequiredResponse()
        }
    } catch (error) {
        // 如果获取store失败，继续正常流程
        if (import.meta.env.DEV) {
            console.warn('🟡 [401Handler] 无法获取用户状态，继续正常处理')
        }
    }

    // 白名单接口直接返回错误，让业务层处理
    if (config.skipAuthRetry || isWhitelistUrl(config.url)) {
        return Promise.reject(axiosErr)
    }

    try {
        const result = await ErrorHandler.handle401Error(axiosErr, config)
        if (result === null) {
            // 🔥 返回一个表示需要登录的友好响应
            return createAuthRequiredResponse()
        }
        return result
    } catch (e) {
        // 🔥 401错误处理失败时返回友好响应，不让业务层看到401错误
        if (import.meta.env.DEV) {
            console.warn('🔐 401错误处理失败，返回友好响应给业务层')
        }
        return createAuthRequiredResponse()
    }
}

// 🔥 检查是否是白名单URL
function isWhitelistUrl(url?: string): boolean {
    if (!url) return false
    return AUTH_WHITELIST.some(endpoint => url.includes(endpoint))
}

// 🔥 创建需要登录的友好响应
function createAuthRequiredResponse(): AxiosResponse {
    return {
        data: {
            success: false,
            code: 'AUTH_REQUIRED',
            message: '请重新登录',
            data: null
        },
        status: 200, // 🔥 返回200状态码，避免业务层看到401
        statusText: 'OK',
        headers: {} as any,
        config: {} as any
    }
}

export default instance
export type { ApiErrorResponse }