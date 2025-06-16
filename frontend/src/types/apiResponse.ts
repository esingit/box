/**
 * 接口统一响应格式
 */
export interface ApiResponse<T = any> {
    success: boolean
    data: T
    message?: string
}