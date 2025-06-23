// src/store/userStore.ts
import { defineStore } from 'pinia'
import axios, { AxiosInstance } from 'axios'
import type { Router } from 'vue-router'
import { tokenService } from '@/api/tokenService'
import emitter from '@/utils/eventBus'

interface User {
    username: string
    email: string
    lastLoginTime: string
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

const createApiClient = (store: ReturnType<typeof useUserStore>): AxiosInstance => {
    if (apiClientInstance) {
        return apiClientInstance
    }

    const instance = axios.create({
        baseURL: '/api/user',
        timeout: 10000,
    })

    instance.interceptors.request.use(config => {
        if (store.token && config.headers) {
            config.headers.Authorization = `Bearer ${store.token}`
        }
        return config
    })

    instance.interceptors.response.use(
        res => res,
        err => Promise.reject(err)
    )

    apiClientInstance = instance
    return instance
}

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: (() => {
            try {
                const stored = localStorage.getItem('user')
                return stored ? JSON.parse(stored) : {}
            } catch {
                return {}
            }
        })(),
        isInitialized: false,
        apiClient: null as AxiosInstance | null,
        isLoggedIn: false,
        isRefreshing: false, // 防止重复刷新token
    }),

    getters: {
        currentUser: state => state.user || {},
        formattedLastLoginTime: state => {
            if (!state.user?.lastLoginTime) return 'N/A'
            try {
                return new Date(state.user.lastLoginTime).toLocaleString('zh-CN')
            } catch {
                return state.user.lastLoginTime
            }
        },
        isAuthenticated: state => state.isLoggedIn && !!state.token,
    },

    actions: {
        initApiClient() {
            if (!this.apiClient) {
                this.apiClient = createApiClient(this)
            }
            return this.apiClient
        },

        setAuthHeader(token: string | null) {
            this.initApiClient()
            if (token) {
                this.apiClient!.defaults.headers.common['Authorization'] = `Bearer ${token}`
            } else {
                delete this.apiClient!.defaults.headers.common['Authorization']
            }
        },

        async hydrate(): Promise<boolean> {
            if (this.isInitialized) {
                console.log('hydrate 已初始化，isLoggedIn:', this.isLoggedIn)
                return this.isLoggedIn
            }

            this.token = tokenService.getToken()
            console.log('hydrate token:', this.token)

            if (!this.token) {
                console.log('无token，清除登录状态')
                await this.clearAuth(false)
                this.isInitialized = true
                return false
            }

            this.setAuthHeader(this.token)

            try {
                const valid = await this.verifyToken()
                console.log('verifyToken 结果:', valid)
                if (!valid) {
                    console.log('token无效，清除登录状态')
                    await this.clearAuth(false)
                    this.isInitialized = true
                    return false
                }

                // 等待 fetchUser 完成，确保用户信息加载
                await this.fetchUser()

                const loggedIn = this.isLoggedIn
                console.log('hydrate 完成，isLoggedIn:', loggedIn)
                this.isInitialized = true
                return loggedIn
            } catch (e) {
                console.error('hydrate异常', e)
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
            if (this.isRefreshing) {
                return false
            }

            this.isRefreshing = true
            this.initApiClient()

            try {
                const res = await this.apiClient!.post<ApiResponse<string>>('/refresh-token')
                if (res.data.success && res.data.data) {
                    this.token = res.data.data
                    tokenService.setToken(this.token)
                    this.setAuthHeader(this.token)
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
            if (!this.token) {
                await this.clearAuth(false)
                return
            }

            // 如果已登录且有用户数据，直接返回
            if (this.user && Object.keys(this.user).length > 0 && this.isLoggedIn) {
                return
            }

            this.initApiClient()
            try {
                const res = await this.apiClient!.get<ApiResponse<User>>('/profile')
                console.log('fetchUser res:', res)
                if (res.data.success && res.data.data) {
                    this.user = res.data.data
                    localStorage.setItem('user', JSON.stringify(this.user))
                    this.isLoggedIn = true
                } else {
                    console.log('fetchUser失败，清除登录')
                    this.user = {}
                    await this.clearAuth(false)
                }
            } catch (e) {
                console.error('fetchUser异常', e)
                this.user = {}
                await this.clearAuth(false)
            }
        },

        async login(credentials: Record<string, any>): Promise<LoginResponse> {
            this.initApiClient()
            try {
                const res = await this.apiClient!.post<ApiResponse<string>>('/login', credentials)
                if (res.data.success && res.data.data) {
                    this.token = res.data.data
                    tokenService.setToken(this.token)
                    this.setAuthHeader(this.token)
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

        async callLogoutAPI(): Promise<void> {
            if (!this.token) return

            this.initApiClient()
            try {
                await this.apiClient!.post('/logout')
            } catch (error) {
                console.warn('登出API调用失败', error)
            }
        },

        async clearAuth(clearUI = true): Promise<void> {
            this.token = null
            this.user = {}
            this.isLoggedIn = false
            this.isRefreshing = false
            localStorage.removeItem('user')
            tokenService.removeToken()
            this.setAuthHeader(null)

            if (clearUI) {
                emitter.emit('notify', {
                    message: '已成功注销',
                    type: 'success'
                })

                try {
                    if (window.location.pathname !== '/home') {
                        window.location.replace('/home')
                    }
                } catch (e) {
                    console.error('跳转失败', e)
                    window.location.replace('/')
                }
            }
        },

        async logout(clearUI = true, router?: Router): Promise<void> {
            try {
                await this.callLogoutAPI()
            } catch {}

            await this.clearAuth(clearUI)

            if (router && router.currentRoute.value.path !== '/login') {
                router.push('/login')
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
