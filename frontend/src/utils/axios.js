import axios from 'axios'
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus'

// axios 配置
const axiosConfig = {
  timeout: 10000, // 超时时间设置为 10 秒
  withCredentials: true, // 允许跨域请求携带 cookie
  // 请求重试配置
  retry: 2, // 重试次数
  retryDelay: 1000, // 重试延迟时间（毫秒）
  shouldRetry: (error) => {
    // 判断是否需要重试
    return error.code === 'ECONNABORTED' || // 超时
           !error.response || // 网络错误
           [500, 502, 503, 504].includes(error.response?.status) // 服务器错误
  }
}

// 创建 axios 实例
const instance = axios.create(axiosConfig)

// 用于记录是否正在刷新 token
let isRefreshing = false;
// 存储等待 token 刷新的请求
let waitingRequests = [];

// 添加重试拦截器
instance.interceptors.response.use(undefined, async (error) => {
  if (error.response) {
    const { status } = error.response
    
    // 处理 401（未授权）、403（权限不足）以及其他 token 相关错误
    if (status === 401 || 
        status === 403 ||
        error.response.data?.message?.toLowerCase().includes('token')) {

      // 获取原始请求的配置
      const originalRequest = error.config
      
      // 如果请求包含 skipAuthRetry 标记，说明是刷新 token 的请求本身
      if (originalRequest.skipAuthRetry) {
        return Promise.reject(error)
      }
      
      // 如果当前正在刷新 token，将请求添加到等待队列
      if (isRefreshing) {
        return new Promise(resolve => {
          waitingRequests.push(token => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`
            resolve(instance(originalRequest))
          })
        })
      }
      
      isRefreshing = true
      
      try {
        // 尝试刷新 token
        const response = await instance({
          method: 'post',
          url: '/api/user/refresh-token',
          headers: { 
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          },
          skipAuthRetry: true // 标记这个请求不要再次尝试刷新 token
        })
        
        if (response.data.success) {
          const newToken = response.data.data
          // 更新本地存储的 token
          localStorage.setItem('token', newToken)
          // 更新当前请求的 Authorization 头
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
          
          // 执行所有等待的请求
          waitingRequests.forEach(callback => callback(newToken))
          waitingRequests = []
          
          // 重试原始请求
          return instance(originalRequest)
        } else {
          // 刷新失败时清空等待队列
          waitingRequests.forEach(callback => callback(null))
          waitingRequests = []
          
          // 如果刷新失败，清除token并显示带错误信息的登录对话框
          const userStore = useUserStore()
          await userStore.logout(false) // 不清除UI状态，保留侧边栏等UI组件
          // 显示登录对话框，但保持当前页面状态
          emitter.emit('show-auth', 'login', {
            message: response.data.message || 'Token已过期，请重新登录',
            preserveState: true
          })
          return Promise.reject(new Error(response.data.message || 'Token刷新失败'))
        }
      } catch (refreshError) {
        // 如果刷新 token 失败，清除token并显示登录对话框
        const userStore = useUserStore()
        await userStore.logout(false) // 不清除UI状态
        emitter.emit('show-auth', 'login', '登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    
    // 处理其他常见错误
    const statusMessages = {
      400: '请求参数错误',
      401: error.response.data?.message || 'Token已过期，请重新登录',
      403: error.response.data?.message || '权限不足',
      404: '请求的资源不存在',
      500: '服务器内部错误',
      502: '网关错误',
      503: '服务不可用',
      504: '网关超时'
    }
    
    const message = statusMessages[status] || '未知错误'
    emitter.emit('notify', message, 'error')
    
    // 输出错误日志
    console.error(`[API] ${error.config.method.toUpperCase()} ${error.config.url} [${status}]: ${message}`)
  } else if (error.request) {
    // 网络错误
    emitter.emit('notify', '网络连接失败，请检查网络设置', 'error')
    console.error('[API] Network Error:', error.message)
  }
  
  return Promise.reject(error)
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
  response => {
    // 输出请求日志
    console.debug(`[API] ${response.config.method.toUpperCase()} ${response.config.url} [${response.status}]`)
    return response
  },
  error => {
    // 错误已经在重试拦截器中处理
    return Promise.reject(error)
  }
)

export default instance