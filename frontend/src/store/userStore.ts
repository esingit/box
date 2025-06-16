// pinia userStore.ts
import { defineStore } from 'pinia'
import axios, { AxiosInstance } from 'axios'
import type { Router } from 'vue-router'
import { tokenService } from '@/api/tokenService'

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

const createApiClient = (store: ReturnType<typeof useUserStore>): AxiosInstance => {
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

    return instance
}

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null,
        isInitialized: false,
        apiClient: null as AxiosInstance | null,
    }),

    getters: {
        currentUser: (state) => state.user,
        formattedLastLoginTime: (state) => {
            if (!state.user?.lastLoginTime) return 'N/A'
            try {
                return new Date(state.user.lastLoginTime).toLocaleString('zh-CN')
            } catch {
                return state.user.lastLoginTime
            }
        },
    },

    actions: {
        initApiClient() {
            if (!this.apiClient) {
                this.apiClient = createApiClient(this)
            }
        },

        setAuthHeader(token: string | null) {
            if (!this.apiClient) this.initApiClient()
            if (token) {
                this.apiClient!.defaults.headers.common['Authorization'] = `Bearer ${token}`
            } else {
                delete this.apiClient!.defaults.headers.common['Authorization']
            }
        },

        async hydrate(): Promise<boolean> {
            this.token = tokenService.getToken()
            if (!this.token) {
                this.clearAuth()
                this.isInitialized = true
                return false
            }
            this.setAuthHeader(this.token)
            try {
                const valid = await this.verifyToken()
                if (!valid) {
                    this.clearAuth()
                    this.isInitialized = true
                    return false
                }
                await this.fetchUser()
                this.isInitialized = true
                return true
            } catch {
                this.clearAuth()
                this.isInitialized = true
                return false
            }
        },

        async verifyToken(): Promise<boolean> {
            if (!this.apiClient) this.initApiClient()
            try {
                const res = await this.apiClient!.get<ApiResponse<{ shouldRefresh?: boolean }>>('/verify-token')
                if (res.data.success) {
                    if (res.data.data?.shouldRefresh) {
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
            if (!this.apiClient) this.initApiClient()
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
            }
        },

        async fetchUser(): Promise<void> {
            if (!this.token) {
                this.clearAuth()
                return
            }
            if (!this.apiClient) this.initApiClient()
            try {
                const res = await this.apiClient!.get<ApiResponse<User>>('/profile')
                if (res.data.success && res.data.data) {
                    this.user = res.data.data
                    localStorage.setItem('user', JSON.stringify(this.user))
                } else {
                    this.clearAuth()
                }
            } catch {
                this.clearAuth()
            }
        },

        async login(credentials: Record<string, any>): Promise<LoginResponse> {
            if (!this.apiClient) this.initApiClient()
            try {
                const res = await this.apiClient!.post<ApiResponse<string>>('/login', credentials)
                if (res.data.success && res.data.data) {
                    this.token = res.data.data
                    tokenService.setToken(this.token)
                    this.setAuthHeader(this.token)
                    await this.fetchUser()
                    return { success: true, message: '登录成功' }
                }
                return { success: false, message: res.data.message || '登录失败', needCaptcha: res.data.needCaptcha }
            } catch (err: any) {
                return { success: false, message: err.response?.data?.message || '登录失败，请重试' }
            }
        },

        async register(userData: Record<string, any>): Promise<ApiResponse> {
            if (!this.apiClient) this.initApiClient()
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

        async logout(callAPI = true, router?: Router): Promise<void> {
            if (!this.apiClient) this.initApiClient()
            if (callAPI) {
                try {
                    await this.apiClient!.post('/logout')
                } catch {}
            }
            this.clearAuth()
            if (router) router.push('/login')
        },

        clearAuth() {
            this.token = null
            this.user = null
            localStorage.removeItem('user')
            tokenService.removeToken()
            this.setAuthHeader(null)
        },
    },
})
