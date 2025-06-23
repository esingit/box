// src/utils/axiosConfig.ts
import axios, { AxiosRequestConfig, AxiosError } from 'axios'

export interface AxiosRetryConfig extends AxiosRequestConfig {
  retry?: number
  retryDelay?: number
  shouldRetry?: (error: unknown) => boolean
}

export const ALLOWED_DUPLICATE_ENDPOINTS = [
  '/login',
  '/register',
  // 如果以后有其它不想重试的接口，加到这里
]

export const axiosConfig: AxiosRetryConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000,
  withCredentials: true,
  retry: 3,
  retryDelay: 1500,
  shouldRetry: (err: unknown): boolean => {
    const error = err as Partial<AxiosError> & { config?: any }
    const url = error.config?.url ?? ''

    // 不重试名单里的接口直接返回 false
    if (ALLOWED_DUPLICATE_ENDPOINTS.some(endpoint => url.includes(endpoint)) || axios.isCancel(error)) {
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
