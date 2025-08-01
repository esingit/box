// API相关类型定义

// API响应接口
export interface ApiResponse<T = any> {
    success: boolean
    message: string
    data?: T
    code?: string
    needCaptcha?: boolean
}