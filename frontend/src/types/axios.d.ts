// src/types/axios.d.ts
import 'axios'

declare module 'axios' {
    export interface AxiosRequestConfig {
        skipAuthRetry?: boolean
        retry?: number
        retryDelay?: number
        shouldRetry?: (error: unknown) => boolean
    }
}
