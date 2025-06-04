import { defineStore } from 'pinia'
import axios from '@/utils/axios.js'
import { useRouter } from 'vue-router'

const API_URL = '/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user')) || null,
    isLoggedIn: !!localStorage.getItem('token'),
  }),

  getters: {
    currentUser: (state) => state.user,
    formattedLastLoginTime: (state) => {
      if (state.user?.lastLoginTime) {
        try {
          return new Date(state.user.lastLoginTime).toLocaleString('zh-CN')
        } catch (e) {
          return state.user.lastLoginTime
        }
      }
      return 'N/A'
    }
  },

  actions: {
    async register(userData) {
      try {
        const response = await axios.post('/api/user/register', userData)
        return response.data
      } catch (error) {
        console.error('注册失败:', error)
        throw error
      }
    },

    async login(credentials) {
      try {
        const response = await axios.post(`${API_URL}/login`, credentials)
        
        if (response.data.success && response.data.data) {
          const responseData = response.data.data
          
          this.token = responseData.token
          this.user = {
            username: responseData.username,
            email: responseData.email,
            lastLoginTime: responseData.lastLoginTime
          }
          this.isLoggedIn = true

          localStorage.setItem('token', this.token)
          localStorage.setItem('user', JSON.stringify(this.user))
          
          return { 
            success: true,
            token: this.token 
          }
        } else {
          return {
            success: false,
            message: response.data.message || '登录失败',
            showCaptcha: response.data.showCaptcha || false
          }
        }
      } catch (error) {
        console.error('登录错误:', error)
        return {
          success: false,
          message: error.response?.data?.message || '登录请求失败',
          showCaptcha: error.response?.data?.showCaptcha || false
        }
      }
    },

    async logout(clearUI = true, router = null) {
      try {
        // 尝试调用后端登出接口，添加skipAuthRetry标记避免401刷新
        await axios.post(`${API_URL}/logout`, null, {
          skipAuthRetry: true
        }).catch(() => {
          // 忽略后端登出失败的错误
        })
      } finally {
        // 清除 token
        localStorage.removeItem('token')
        this.token = null
        
        // 只有在需要时才清除UI状态
        if (clearUI) {
          this.user = null
          this.isLoggedIn = false
          localStorage.removeItem('user')
        } else {
          // 当不清除UI状态时，也需要设置登录状态为false
          this.isLoggedIn = false
        }

        // 如果提供了router实例，则跳转到主页
        if (router) {
          router.push('/home')
        }
      }
    },

    formatLastLoginTime() {
      if (this.user?.lastLoginTime) {
        try {
          return new Date(this.user.lastLoginTime).toLocaleString('zh-CN')
        } catch (e) {
          return this.user.lastLoginTime
        }
      }
      return 'N/A'
    }
  }
})