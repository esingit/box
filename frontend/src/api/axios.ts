// src/api/axios.ts
import axios, {AxiosError, AxiosHeaders, AxiosResponse, InternalAxiosRequestConfig} from 'axios'
import {ALLOWED_DUPLICATE_ENDPOINTS, axiosConfig} from '@/api/axiosConfig'
import {generateRequestKey, requestManager} from '@/api/requestManager'
import {tokenService} from '@/api/tokenService'
import {ErrorHandler} from '@/api/errorHandler'

type CustomRequestConfig = InternalAxiosRequestConfig & {
    allowDuplicate?: boolean
    skipAuthRetry?: boolean
    retry?: number
    retryDelay?: number
    signal?: AbortSignal
}

// 白名单：无需携带 Token 的接口
const AUTH_WHITELIST: string[] = [
    '/api/user/login',
    '/api/user/register',
    '/api/captcha'
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

        // 非白名单接口才注入token
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

instance.interceptors.response.use(
    (response: AxiosResponse) => {
        requestManager.delete(generateRequestKey(response.config as CustomRequestConfig))
        return response
    },
    async (error: unknown) => {
        if (axios.isAxiosError(error)) {
            const axiosErr = error as AxiosError
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

            if (response.status === 401) {
                if (config?.skipAuthRetry) return Promise.reject(axiosErr)

                const result = await ErrorHandler.handle401Error(axiosErr, config!)
                // 🔥 处理失败后，result 可能是 null，我们直接返回空响应
                if (result === null) return Promise.resolve({ data: null, status: 401 })
                return result
            }

            ErrorHandler.handleOtherErrors(response.status, response.data)
            return ErrorHandler.handleRetry(axiosErr, config!)
        }

        return Promise.reject(error)
    }
)

export default instance