// src/store/userStore.ts
import { defineStore } from 'pinia'
import type { Router } from 'vue-router'
import { tokenService } from '@/api/tokenService'
import axiosInstance from '@/api/axios' // 使用统一的axios实例
import emitter from '@/utils/eventBus'

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
    needCaptcha?: boolean
}

interface LoginResponse {
    success: boolean
    message: string
    needCaptcha?: boolean
}

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: {} as User,
        isInitialized: false,
        isLoggedIn: false,
        isRefreshing: false,
    }),

    getters: {
        currentUser: state => state.user,
        isAuthenticated: state => state.isLoggedIn && !!state.token,
    },

    actions: {
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

        async hydrate(): Promise<boolean> {
            if (this.isInitialized) return this.isLoggedIn

            this.token = tokenService.getToken()
            if (!this.token) {
                await this.clearAuth(false)
                this.isInitialized = true
                return false
            }

            this.setAuth(this.token)

            try {
                const valid = await this.verifyToken()
                if (!valid) {
                    await this.clearAuth(false)
                    this.isInitialized = true
                    return false
                }
                await this.fetchUser()
                this.isInitialized = true
                return true
            } catch {
                await this.clearAuth(false)
                this.isInitialized = true
                return false
            }
        },

        async verifyToken(): Promise<boolean> {
            try {
                const res = await axiosInstance.get<ApiResponse<{ shouldRefresh?: boolean }>>('/api/user/verify-token')
                if (res.data.success) {
                    if (res.data.data?.shouldRefresh && !this.isRefreshing) {
                        return await this.refreshToken()
                    }
                    return true
                }
                return false
            } catch {
                return false
            }
        },

        async refreshToken(): Promise<boolean> {
            if (this.isRefreshing) return false

            this.isRefreshing = true

            try {
                const res = await axiosInstance.post<ApiResponse<string>>('/api/user/refresh-token')
                if (res.data.success && res.data.data) {
                    this.setAuth(res.data.data)
                    return true
                }
                return false
            } catch {
                return false
            } finally {
                this.isRefreshing = false
            }
        },

        async fetchUser(): Promise<void> {
            if (!this.token) return await this.clearAuth(false)

            try {
                const res = await axiosInstance.get<ApiResponse<User>>('/api/user/profile')
                if (res.data.success && res.data.data) {
                    this.user = res.data.data
                    this.isLoggedIn = true
                } else {
                    await this.clearAuth(false)
                }
            } catch {
                await this.clearAuth(false)
            }
        },

        async login(credentials: Record<string, any>): Promise<LoginResponse> {
            try {
                const res = await axiosInstance.post<ApiResponse<string>>('/api/user/login', credentials)
                if (res.data.success && res.data.data) {
                    this.setAuth(res.data.data)
                    await this.fetchUser()
                    return { success: true, message: '登录成功' }
                }

                return {
                    success: false,
                    message: res.data.message || '登录失败',
                    needCaptcha: res.data.needCaptcha
                }
            } catch (err: any) {
                return {
                    success: false,
                    message: err.response?.data?.message || '登录失败，请重试'
                }
            }
        },

        async fetchCaptcha(): Promise<{ imageUrl: string; captchaId: string } | null> {
            try {
                const { data } = await axiosInstance.get('/api/captcha', { params: { t: Date.now() } })
                return {
                    captchaId: data.captchaId,
                    imageUrl: `/api${data.captchaUrl}`,
                }
            } catch (error) {
                console.error('验证码获取失败:', error)
                return null
            }
        },

        async logout(clearUI = true, router?: Router): Promise<void> {
            try {
                await axiosInstance.post('/api/user/logout')
            } catch {}

            await this.clearAuth(clearUI)

            if (router && router.currentRoute.value.path !== '/home') {
                router.push('/home')
            }
        },

        async clearAuth(clearUI = true): Promise<void> {
            this.setAuth(null)
            this.user = {} as User
            this.isRefreshing = false
            this.isLoggedIn = false

            if (clearUI) {
                emitter.emit('notify', { message: '已注销', type: 'success' })
                window.location.replace('/home')
            }
        },

        async register(userData: Record<string, any>): Promise<ApiResponse> {
            try {
                const res = await axiosInstance.post<ApiResponse>('/api/user/register', userData)
                return res.data
            } catch (err: any) {
                return {
                    success: false,
                    message: err.response?.data?.message || '注册失败，请稍后再试',
                    needCaptcha: err.response?.data?.needCaptcha || false,
                }
            }
        },

        async resetPassword(oldPassword: string, newPassword: string): Promise<{ success: boolean; message: string }> {
            try {
                const res = await axiosInstance.post<ApiResponse>('/api/user/reset-password', {
                    username: this.user?.username,
                    oldPassword,
                    newPassword
                })

                if (res.data.success) {
                    return { success: true, message: '密码修改成功' }
                } else {
                    return { success: false, message: res.data.message || '修改失败' }
                }
            } catch (err: any) {
                return {
                    success: false,
                    message: err.response?.data?.message || '请求失败'
                }
            }
        }
    }
})