import 'axios'

declare module 'axios' {
    export interface AxiosRequestConfig {
        skipAuthRetry?: boolean
    }
}
