import axios, { AxiosResponse, AxiosError, InternalAxiosRequestConfig, AxiosHeaders } from 'axios'
import { useUserStore } from '@/store/userStore'
import { axiosConfig, ALLOWED_DUPLICATE_ENDPOINTS } from '@/api/axiosConfig'
import { generateRequestKey, requestManager } from '@/api/requestManager'
import { tokenService } from '@/api/tokenService'
import { ErrorHandler } from '@/api/errorHandler'

type CustomRequestConfig = InternalAxiosRequestConfig & {
    allowDuplicate?: boolean
    skipAuthRetry?: boolean
    signal?: AbortSignal
}

const instance = axios.create(axiosConfig)

instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const customConfig = config as CustomRequestConfig
        const requestKey = generateRequestKey(customConfig)

        const allowDuplicate =
            customConfig.allowDuplicate ||
            ALLOWED_DUPLICATE_ENDPOINTS.some(endpoint =>
                customConfig.url?.includes(endpoint)
            )

        if (!allowDuplicate) {
            if (requestManager.has(requestKey)) {
                const previousRequest = requestManager.get(requestKey)
                previousRequest?.abort('取消重复请求')
                requestManager.delete(requestKey)
            }

            const controller = new AbortController()
            customConfig.signal = controller.signal
            requestManager.add(requestKey, controller)
        }

        const token = tokenService.getToken()

        if (!customConfig.headers) {
            // Axios 4.x 需要用 AxiosHeaders 实例
            customConfig.headers = new AxiosHeaders()
        }

        if (token) {
            // 使用 AxiosHeaders 的 set 方法添加请求头
            customConfig.headers.set('Authorization', `Bearer ${token}`)
        }

        return customConfig
    },
    error => Promise.reject(error)
)

instance.interceptors.response.use(
    response => {
        if (response.config) {
            requestManager.delete(generateRequestKey(response.config))
        }
        return response
    },
    async (error: AxiosError) => {
        const config = error.config as CustomRequestConfig | undefined
        if (config) {
            requestManager.delete(generateRequestKey(config))
        }

        if (axios.isCancel(error)) {
            return Promise.reject(error)
        }

        const response = error.response as AxiosResponse | undefined

        if (!response) {
            ErrorHandler.handleNetworkError()
            return Promise.reject(error)
        }

        if (response.status === 401) {
            if (config?.skipAuthRetry) {
                return Promise.reject(error)
            }
            return ErrorHandler.handle401Error(error, config)
        }

        ErrorHandler.handleOtherErrors(response.status, response.data)

        return ErrorHandler.handleRetry(error, config)
    }
)

export default instance
