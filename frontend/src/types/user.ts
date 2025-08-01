// 用户相关类型定义
import { ID } from './base'

// 用户信息接口
export interface User {
    username: string
    email: string
    lastLoginTime: string
    roles?: string[]
}

// 登录响应接口
export interface LoginResponse {
    success: boolean
    message: string
    needCaptcha?: boolean
}

// Token对接口
export interface TokenPair {
    accessToken: string
    refreshToken: string
}