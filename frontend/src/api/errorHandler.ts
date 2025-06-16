import { AxiosError, AxiosRequestConfig } from 'axios'

export const ErrorHandler = {
    handleNetworkError() {
        // 网络错误统一处理，比如弹通知
        console.error('网络异常，请检查您的网络连接')
        // 你可以用消息组件提示用户
    },

    async handle401Error(error: AxiosError, config?: AxiosRequestConfig) {
        // 401 处理逻辑，例如刷新token或跳转登录
        console.warn('未授权，请登录')
        // 这里可以调用刷新token的接口，或者直接登出
        // 例如：
        // await import('@/api/tokenService').then(module => module.default.refreshToken())
        // 或者
        // router.push('/login')
        return Promise.reject(error)
    },

    handleOtherErrors(status: number, data: any) {
        // 其他错误处理
        console.error(`请求错误，状态码：${status}`, data)
        // 弹窗提示，或者日志记录
    },

    async handleRetry(error: AxiosError, config?: AxiosRequestConfig) {
        // 简单重试示例（可根据需要实现复杂重试逻辑）
        // 这里不自动重试，直接reject
        return Promise.reject(error)
    }
}
