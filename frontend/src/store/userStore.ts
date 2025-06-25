// src/store/userStore.ts
import { defineStore } from 'pinia'
import type { Router } from 'vue-router'
import { tokenService } from '@/api/tokenService'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'

// 🔥 类型定义
interface User {
    username: string
    email: string
    lastLoginTime: string
    roles?: string[]
}

interface ApiResponse<T = any> {
    success: boolean
    message: string
    data?: T
    code?: string
    needCaptcha?: boolean
}

interface LoginResponse {
    success: boolean
    message: string
    needCaptcha?: boolean
}

interface TokenPair {
    accessToken: string
    refreshToken: string
}

// 🔥 常量定义
const isDev = import.meta.env.DEV
const AUTH_ERROR_CODES = ['401', 'UNAUTHORIZED', 'AUTH_REQUIRED', 'TOKEN_EXPIRED']

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: {} as User,
        isInitialized: false,
        isLoggedIn: false,
        isRefreshing: false,
        isLoggingOut: false,
    }),

    getters: {
        pageNoUser: (state) => state.user,
        isAuthenticated: (state) => state.isLoggedIn && !!state.token,
    },

    actions: {
        // 🔥 认证状态管理
        setAuth(token: string | null) {
            this.token = token
            if (token) {
                tokenService.setToken(token)
                this.isLoggedIn = true
            } else {
                tokenService.clearToken()
                this.isLoggedIn = false
            }
        },

        // 🔥 初始化用户状态
        async hydrate(): Promise<boolean> {
            if (this.isInitialized) return this.isLoggedIn

            this.token = tokenService.getToken()
            const refreshToken = tokenService.getRefreshToken()

            if (!this.token || !refreshToken) {
                await this.clearAuth(false)
                this.isInitialized = true
                return false
            }

            this.setAuth(this.token)

            try {
                // 直接获取用户信息，让拦截器处理 token 刷新
                await this.fetchUser()
                this.isInitialized = true
                return true
            } catch (error) {
                // 静默处理认证错误
                if (this.isAuthError(error)) {
                    if (isDev) {
                        console.log('🟡 [Hydrate] 认证失败，静默处理')
                    }
                }
                await this.clearAuth(false)
                this.isInitialized = true
                return false
            }
        },

        // 🔥 获取用户信息
        async fetchUser(): Promise<void> {
            if (!this.token) {
                await this.clearAuth(false)
                return
            }

            try {
                const res = await axiosInstance.get<ApiResponse<User>>('/api/user/profile')
                if (res.data.success && res.data.data) {
                    this.user = res.data.data
                    this.isLoggedIn = true
                } else {
                    await this.clearAuth(false)
                }
            } catch (error) {
                // 静默处理认证错误
                if (this.isAuthError(error)) {
                    if (isDev) {
                        console.log('🟡 [FetchUser] 认证失败，静默处理')
                    }
                }
                // 错误已经被拦截器处理，这里只需要确保状态正确
                if (!tokenService.getToken()) {
                    await this.clearAuth(false)
                }
            }
        },

        // 🔥 用户登录
        async login(credentials: Record<string, any>): Promise<LoginResponse> {
            try {
                const res = await axiosInstance.post<ApiResponse<TokenPair>>('/api/user/login', credentials)
                if (res.data.success && res.data.data) {
                    // 确保使用 setTokenPair 而不是单独设置
                    tokenService.setTokenPair(res.data.data)
                    this.setAuth(res.data.data.accessToken)

                    await this.fetchUser()

                    // 登录成功通知
                    emitter.emit('notify', {
                        message: '登录成功',
                        type: 'success'
                    })

                    return {
                        success: true,
                        message: '登录成功'
                    }
                }

                return {
                    success: false,
                    message: res.data.message || '登录失败',
                    needCaptcha: res.data.needCaptcha,
                }
            } catch (err: any) {
                const errorMessage = this.getErrorMessage(err)

                if (isDev && !this.isAuthError(err)) {
                    console.error('🔴 [Login] 登录错误:', err)
                }

                return {
                    success: false,
                    message: errorMessage,
                    needCaptcha: err.response?.data?.needCaptcha,
                }
            }
        },

        // 🔥 获取验证码
        async fetchCaptcha(): Promise<{ imageUrl: string; captchaId: string } | null> {
            try {
                const { data } = await axiosInstance.get('/api/captcha', {
                    params: { t: Date.now() }
                })

                return {
                    captchaId: data.captchaId,
                    imageUrl: `/api${data.captchaUrl}`,
                }
            } catch (error) {
                if (isDev) {
                    console.error('🔴 [Captcha] 验证码获取失败:', error)
                }
                emitter.emit('notify', {
                    message: '验证码获取失败',
                    type: 'error'
                })
                return null
            }
        },

        // 🔥 用户注销
        async logout(clearUI = true, router?: Router): Promise<void> {
            // 🔥 立即设置注销标记（在任何API调用之前）
            this.isLoggingOut = true
            localStorage.setItem('__user_logging_out__', 'true')
            sessionStorage.setItem('__user_logging_out__', 'true') // 双重保险

            if (isDev) {
                console.log('🚀 [Logout] 开始注销流程，已设置注销标记')
            }

            try {
                // 🔥 延迟一下确保标记已经设置
                await new Promise(resolve => setTimeout(resolve, 50))

                await axiosInstance.post('/api/user/logout', {}, {
                    skipAuthRetry: true,
                } as any)

                if (isDev) {
                    console.log('✅ [Logout] 注销API调用成功')
                }
            } catch (error) {
                if (isDev) {
                    console.log('🟡 [Logout] 注销请求失败，继续清理本地状态')
                }
            }

            // 清理本地状态
            await this.clearAuth(false)

            if (clearUI) {
                emitter.emit('notify', {
                    message: '已注销',
                    type: 'success'
                })
            }

            // 页面跳转
            if (router && router.currentRoute.value.path !== '/home') {
                await router.replace('/home')
            } else if (!router) {
                setTimeout(() => {
                    window.location.replace('/home')
                }, 100)
            }

            // 🔥 清理注销标记
            this.isLoggingOut = false
            localStorage.removeItem('__user_logging_out__')
            sessionStorage.removeItem('__user_logging_out__')

            if (isDev) {
                console.log('🏁 [Logout] 注销流程完成，已清理注销标记')
            }
        },

        // 🔥 清理认证状态
        async clearAuth(clearUI = true): Promise<void> {
            // 🔥 如果是通过clearAuth清理，也设置标记防止401弹窗
            if (!this.isLoggingOut) {
                localStorage.setItem('__user_logging_out__', 'true')
                sessionStorage.setItem('__user_logging_out__', 'true')
            }

            this.setAuth(null)
            tokenService.clearAllTokens()
            this.user = {} as User
            this.isRefreshing = false
            this.isLoggedIn = false

            if (clearUI) {
                try {
                    tokenService.clearBrowserMemoryExceptAuth()
                } catch (error) {
                    if (isDev) {
                        console.error('🔴 [ClearAuth] 清理浏览器记忆时出错:', error)
                    }
                }

                emitter.emit('notify', {
                    message: '已注销',
                    type: 'success'
                })

                window.location.replace('/home')
            }

            // 🔥 延迟清理标记
            setTimeout(() => {
                localStorage.removeItem('__user_logging_out__')
                sessionStorage.removeItem('__user_logging_out__')
            }, 1000)
        },

        // 🔥 用户注册
        async register(userData: Record<string, any>): Promise<ApiResponse> {
            try {
                const res = await axiosInstance.post<ApiResponse>('/api/user/register', userData)

                if (res.data.success) {
                    emitter.emit('notify', {
                        message: '注册成功',
                        type: 'success'
                    })
                }

                return res.data
            } catch (err: any) {
                const errorMessage = this.getErrorMessage(err)

                if (isDev) {
                    console.error('🔴 [Register] 注册错误:', err)
                }

                return {
                    success: false,
                    message: errorMessage,
                    needCaptcha: err.response?.data?.needCaptcha || false,
                }
            }
        },

        // 🔥 重置密码
        async resetPassword(oldPassword: string, newPassword: string): Promise<{ success: boolean; message: string }> {
            try {
                const res = await axiosInstance.post<ApiResponse>('/api/user/reset-password', {
                    username: this.user?.username,
                    oldPassword,
                    newPassword,
                })

                if (res.data.success) {
                    emitter.emit('notify', {
                        message: '密码修改成功',
                        type: 'success'
                    })

                    return {
                        success: true,
                        message: '密码修改成功'
                    }
                } else {
                    const message = res.data.message || '修改失败'

                    emitter.emit('notify', {
                        message,
                        type: 'error'
                    })

                    return {
                        success: false,
                        message
                    }
                }
            } catch (err: any) {
                const errorMessage = this.getErrorMessage(err)

                if (isDev && !this.isAuthError(err)) {
                    console.error('🔴 [ResetPassword] 重置密码错误:', err)
                }

                // 非认证错误才显示通知
                if (!this.isAuthError(err)) {
                    emitter.emit('notify', {
                        message: errorMessage,
                        type: 'error'
                    })
                }

                return {
                    success: false,
                    message: errorMessage,
                }
            }
        },

        // 🔥 辅助方法：判断是否为认证错误
        isAuthError(error: any): boolean {
            if (!error) return false

            const err = error as any
            const errorCode = err.response?.data?.code || err.response?.status?.toString()
            const errorMessage = err.response?.data?.message || err.message || ''

            // 检查错误代码
            if (AUTH_ERROR_CODES.includes(errorCode)) {
                return true
            }

            // 检查401状态码
            if (err.response?.status === 401) {
                return true
            }

            // 检查认证相关的错误消息
            const authErrorMessages = [
                '未登录',
                '登录已过期',
                'token已过期',
                'token无效',
                '认证失败',
                'unauthorized'
            ]

            return authErrorMessages.some(msg =>
                errorMessage.toLowerCase().includes(msg.toLowerCase())
            )
        },

        // 🔥 辅助方法：获取错误消息
        getErrorMessage(error: any): string {
            if (!error) return '未知错误'

            // 如果是认证错误，返回更友好的消息
            if (this.isAuthError(error)) {
                return '登录已过期，请重新登录'
            }

            // 优先使用响应中的消息
            if (error.response?.data?.message) {
                return error.response.data.message
            }

            // 其次使用错误消息
            if (error.message) {
                return error.message
            }

            // 最后返回默认消息
            return '操作失败，请稍后再试'
        }
    },
})