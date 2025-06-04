import axios from 'axios'
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus'

// 创建 axios 实例
const instance = axios.create({
  timeout: 5000,
  withCredentials: true // 允许跨域请求携带 cookie
})

// 请求拦截器
instance.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response) {
      // 处理 401 未授权（token 过期等）
      if (error.response.status === 401) {
        // 这个方法会清除 token 并设置 isLoggedIn = false
        const userStore = useUserStore()
        userStore.logout()
        // 通过eventBus通知弹窗登录
        emitter.emit('notify', '登录已过期，请重新登录', 'error');
      }
      // 返回模拟响应，防止页面爆红报错
      return Promise.resolve({
        data: {
          code: 401,
          data: null,
          message: 'UNAUTHORIZED'
        }
      })
    }
    return Promise.reject(error)
  }
)

export default instance