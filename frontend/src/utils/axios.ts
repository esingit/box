import axios, { AxiosResponse, AxiosError, InternalAxiosRequestConfig, AxiosHeaders } from 'axios'
import { axiosConfig, ALLOWED_DUPLICATE_ENDPOINTS } from '@/api/axiosConfig'
import { generateRequestKey, requestManager } from '@/api/requestManager'
import { tokenService } from '@/api/tokenService'
import { ErrorHandler } from '@/api/errorHandler'

type CustomRequestConfig = InternalAxiosRequestConfig & {
    allowDuplicate?: boolean
    skipAuthRetry?: boolean
    signal?: AbortSignal
}

// 创建 Axios 实例，使用统一配置
const instance = axios.create(axiosConfig)

// 请求拦截器：处理重复请求和 Token 注入
instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const customConfig = config as CustomRequestConfig
        const requestKey = generateRequestKey(customConfig)

        // 判断是否允许重复请求
        const allowDuplicate =
            customConfig.allowDuplicate ||
            ALLOWED_DUPLICATE_ENDPOINTS.some(endpoint =>
                customConfig.url?.includes(endpoint)
            )

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

        // 登录/注册接口跳过 Token 注入
        const url = customConfig.url || ''
        const isAuthEndpoint = url.includes('/user/login') || url.includes('/user/register')
        if (!isAuthEndpoint) {
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

// 响应拦截器：清理请求管理 & 错误处理
instance.interceptors.response.use(
    (response: AxiosResponse) => {
        // 成功时，删除请求管理中的记录
        requestManager.delete(generateRequestKey(response.config as CustomRequestConfig))
        return response
    },
    async (error: unknown) => {
        // 类型守卫，确保 error 为 AxiosError
        if (axios.isAxiosError(error)) {
            const axiosErr = error as AxiosError
            const config = axiosErr.config as CustomRequestConfig | undefined
            // 清理请求管理
            if (config) {
                requestManager.delete(generateRequestKey(config))
            }

            // 如果请求被取消，直接抛出
            if (axiosErr.code === 'ERR_CANCELED') {
                return Promise.reject(axiosErr)
            }

            const response = axiosErr.response
            if (!response) {
                ErrorHandler.handleNetworkError()
                return Promise.reject(axiosErr)
            }

            // 401 独立处理
            if (response.status === 401) {
                if (config?.skipAuthRetry) {
                    return Promise.reject(axiosErr)
                }
                return ErrorHandler.handle401Error(axiosErr, config)
            }

            // 其他错误
            ErrorHandler.handleOtherErrors(response.status, response.data)

            return ErrorHandler.handleRetry(axiosErr, config)
        }
        // 非 AxiosError，直接抛出
        return Promise.reject(error)
    }
)

export default instance
