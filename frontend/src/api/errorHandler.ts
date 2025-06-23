import { AxiosError, AxiosRequestConfig } from 'axios'
import { useAuth } from '@/composable/useAuth'
import axiosInstance from '@/utils/axios'

const failedQueue: Array<{
    resolve: (value?: any) => void
    reject: (reason?: any) => void
    config: AxiosRequestConfig
}> = []

export const ErrorHandler = {
    async handle401Error(error: AxiosError, config?: AxiosRequestConfig) {
        if (config?.skipAuthRetry) {
            return Promise.reject(error)
        }

        const { showLogin, pendingAuthAction, token } = useAuth()

        // 触发登录弹窗显示
        showLogin()

        return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject, config: config! })

            if (!pendingAuthAction.value) {
                pendingAuthAction.value = async () => {
                    const newToken = token.value
                    failedQueue.forEach(({ resolve, reject, config }) => {
                        if (!config.headers) config.headers = {}
                        config.headers['Authorization'] = `Bearer ${newToken}`
                        axiosInstance
                            .request(config)
                            .then(resolve)
                            .catch(reject)
                    })
                    failedQueue.length = 0
                    pendingAuthAction.value = null
                }
            }
        })
    },

    handleNetworkError() {
        console.error('网络异常，请检查您的网络连接')
    },

    handleOtherErrors(status: number, data: any) {
        console.error(`请求错误，状态码：${status}`, data)
    },

    async handleRetry(error: AxiosError, config?: AxiosRequestConfig) {
        return Promise.reject(error)
    }
}
