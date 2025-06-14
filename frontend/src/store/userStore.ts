import { defineStore } from 'pinia'
import axios from '@/utils/axios'
import type { Router } from 'vue-router'

interface User {
    username: string
    email: string
    lastLoginTime: string
}

interface LoginResponse {
    success: boolean
    message: string
    needCaptcha?: boolean
}

const API_URL = '/api/user'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: null as string | null,
        user: null as User | null,
        isLoggedIn: false
    }),

    getters: {
        currentUser: (state): User | null => state.user,
        formattedLastLoginTime: (state): string => {
            if (state.user?.lastLoginTime) {
                try {
                    return new Date(state.user.lastLoginTime).toLocaleString('zh-CN')
                } catch {
                    return state.user.lastLoginTime
                }
            }
            return 'N/A'
        }
    },

    actions: {
        async verifyToken(): Promise<boolean> {
            try {
                const response = await axios.get(`${API_URL}/verify-token`)
                if (response.data.success) {
                    if (response.data.data?.shouldRefresh) {
                        const refreshResult = await axios.post(`${API_URL}/refresh-token`)
                        if (refreshResult.data.success) {
                            const newToken = refreshResult.data.data
                            this.token = newToken
                            localStorage.setItem('token', newToken)
                            axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
                        }
                    }
                    return true
                }
                throw new Error(response.data.message || 'Token验证失败')
            } catch (error) {
                console.error('Token验证失败:', error)
                await this.logout(false)
                return false
            }
        },

        async hydrate(): Promise<boolean> {
            const token = localStorage.getItem('token')
            const userStr = localStorage.getItem('user')

            if (token) {
                this.token = token
                this.user = userStr ? JSON.parse(userStr) : null
                this.isLoggedIn = true
                axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
                return await this.verifyToken()
            }
            return false
        },

        async register(userData: Record<string, any>): Promise<any> {
            try {
                const response = await axios.post(`${API_URL}/register`, userData)
                return response.data
            } catch (error) {
                console.error('注册失败:', error)
                throw error
            }
        },

        async login(credentials: Record<string, any>): Promise<LoginResponse> {
            try {
                const response = await axios.post(`${API_URL}/login`, credentials)

                if (response.data.success && response.data.data) {
                    const data = response.data.data
                    axios.defaults.headers.common['Authorization'] = `Bearer ${data.token}`

                    this.token = data.token
                    this.user = {
                        username: data.username,
                        email: data.email,
                        lastLoginTime: data.lastLoginTime
                    }
                    this.isLoggedIn = true

                    localStorage.setItem('token', data.token)
                    localStorage.setItem('user', JSON.stringify(this.user))

                    return { success: true, message: '登录成功' }
                }

                return {
                    success: false,
                    message: response.data.message || '登录失败',
                    needCaptcha: response.data.needCaptcha
                }
            } catch (error: any) {
                console.error('登录失败:', error)
                return {
                    success: false,
                    message: error.response?.data?.message || '登录失败，请重试'
                }
            }
        },

        clearAuthHeader(): void {
            delete axios.defaults.headers.common['Authorization']
        },

        async callLogoutAPI(): Promise<any> {
            return await axios.post(`${API_URL}/logout`, null, {
                skipAuthRetry: true,
                timeout: 5000
            })
        },

        // 修改 logout，接收 router 参数，调用处传入
        async logout(callAPI = true, router?: Router): Promise<void> {
            if (callAPI) {
                try {
                    await this.callLogoutAPI()
                } catch (error) {
                    console.error('登出API调用失败:', error)
                }
            }

            localStorage.removeItem('token')
            localStorage.removeItem('user')

            this.token = null
            this.user = null
            this.isLoggedIn = false

            this.clearAuthHeader()

            if (router) {
                router.push('/login')
            }
        },

        formatLastLoginTime(): string {
            if (this.user?.lastLoginTime) {
                try {
                    return new Date(this.user.lastLoginTime).toLocaleString('zh-CN')
                } catch {
                    return this.user.lastLoginTime
                }
            }
            return 'N/A'
        }
    }
})
