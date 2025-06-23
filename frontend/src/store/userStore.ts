import { defineStore } from 'pinia'
import axios, { AxiosInstance } from 'axios'
import type { Router } from 'vue-router'
import { tokenService } from '@/api/tokenService'
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

let apiClientInstance: AxiosInstance | null = null

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: {} as User,
        isInitialized: false,
        isLoggedIn: false,
        isRefreshing: false,
        apiClient: null as AxiosInstance | null,
        pendingAction: null as (() => Promise<void>) | null,
    }),

    getters: {
        currentUser: state => state.user,
        isAuthenticated: state => state.isLoggedIn && !!state.token,
    },

    actions: {
        initApiClient() {
            if (!this.apiClient) {
                const instance = axios.create({
                    baseURL: '/api/user',
                    timeout: 10000,
                })

                instance.interceptors.request.use(config => {
                    if (this.token && config.headers) {
                        config.headers.Authorization = `Bearer ${this.token}`
                    }
                    return config
                })

                apiClientInstance = instance
                this.apiClient = instance
            }
            return this.apiClient!
        },

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
            this.initApiClient()

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
            this.initApiClient()
            try {
                const res = await this.apiClient!.get<ApiResponse<{ shouldRefresh?: boolean }>>('/verify-token')
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
            this.initApiClient()

            try {
                const res = await this.apiClient!.post<ApiResponse<string>>('/refresh-token')
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

            this.initApiClient()
            try {
                const res = await this.apiClient!.get<ApiResponse<User>>('/profile')
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
            this.initApiClient()
            try {
                const res = await this.apiClient!.post<ApiResponse<string>>('/login', credentials)
                if (res.data.success && res.data.data) {
                    this.setAuth(res.data.data)
                    await this.fetchUser()

                    // 登录成功后执行挂起的操作
                    if (this.pendingAction) {
                        const action = this.pendingAction
                        this.pendingAction = null
                        await action()
                    }

                    emitter.emit('login-success')
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
                const { data } = await axios.get('/api/captcha', { params: { t: Date.now() } })
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
                await this.apiClient?.post('/logout')
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
            this.initApiClient()
            try {
                const res = await this.apiClient!.post<ApiResponse>('/register', userData)
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
            this.initApiClient()

            try {
                const res = await this.apiClient!.post<ApiResponse>('/reset-password', {
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
