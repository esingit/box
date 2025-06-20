import axios, { AxiosRequestConfig, AxiosError } from 'axios'

export interface AxiosRetryConfig extends AxiosRequestConfig {
  retry?: number
  retryDelay?: number
  shouldRetry?: (error: unknown) => boolean
}

export const axiosConfig: AxiosRetryConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000,
  withCredentials: true,
  retry: 3,
  retryDelay: 1500,
  shouldRetry: (err: unknown): boolean => {
    // 强制断言 + 类型守卫双重保险
    const error = err as Partial<AxiosError> & { config?: any }

    const url = error.config?.url ?? ''

    if (
        url.includes('/login') ||
        url.includes('/register') ||
        axios.isCancel(error)
    ) {
      return false
    }

    const status = error.response?.status ?? 0
    return (
        error.code === 'ECONNABORTED' ||
        !error.response ||
        error.code === 'ERR_NETWORK' ||
        [500, 502, 503, 504].includes(status)
    )
  }
}
