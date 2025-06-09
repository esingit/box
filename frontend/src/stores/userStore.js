import { defineStore } from 'pinia'
import axios from '@/utils/axios.js'
import { useRouter } from 'vue-router'

const API_URL = '/api/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    user: null,
    isLoggedIn: false,
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
    async hydrate() {
      const token = localStorage.getItem('token');
      const userStr = localStorage.getItem('user');
      
      if (token) {
        this.token = token;
        this.user = JSON.parse(userStr);
        this.isLoggedIn = true;
        
        // 设置axios默认header
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        
        try {
          // 验证token
          await axios.get(`${API_URL}/verify-token`);
        } catch (error) {
          // token无效，清理状态
          console.error('Token验证失败:', error);
          this.logout(false);
        }
      }
    },
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
          
          // 先设置 axios 默认 header
          axios.defaults.headers.common['Authorization'] = `Bearer ${responseData.token}`
          
          // 更新状态
          this.token = responseData.token
          this.user = {
            username: responseData.username,
            email: responseData.email,
            lastLoginTime: responseData.lastLoginTime
          }
          this.isLoggedIn = true

          // 保存到本地存储
          localStorage.setItem('token', responseData.token)
          localStorage.setItem('user', JSON.stringify(this.user))
          
          return {
            success: true,
            message: '登录成功'
          }
        }
        
        return {
          success: false,
          message: response.data.message || '登录失败',
          needCaptcha: response.data.needCaptcha
        }
      } catch (error) {
        console.error('登录失败:', error)
        return {
          success: false,
          message: error.response?.data?.message || '登录失败，请重试'
        }
      }
    },

    async logout(clearUI = true) {
      try {
        // 只在主动登出时调用后端接口
        if (clearUI) {
          try {
            await axios.post(`${API_URL}/logout`, null, {
              skipAuthRetry: true,
              timeout: 5000 // 设置超时时间为 5 秒
            })
          } catch (error) {
            console.error('登出请求失败:', error)
            // 即使后端请求失败，也继续清理前端状态
          }
        }
        
        // 清除 token
        localStorage.removeItem('token')
        this.token = null
        // 清除用户数据
        localStorage.removeItem('user')
        this.user = null
        // 更新登录状态
        this.isLoggedIn = false
        // 清除 axios 默认 header
        axios.defaults.headers.common['Authorization'] = ''
        
        // 使用 router 进行页面跳转
        const router = useRouter()
        await router.push('/home')
      } catch (error) {
        console.error('注销过程发生错误:', error)
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