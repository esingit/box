import axios from 'axios'
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus'

// axios 配置
const axiosConfig = {
  timeout: 15000, // 超时时间设置为 15 秒
  withCredentials: true, // 允许跨域请求携带 cookie
  // 请求重试配置
  retry: 3, // 重试次数增加到 3 次
  retryDelay: 1500, // 重试延迟时间增加到 1.5 秒
  shouldRetry: (error) => {
    // 判断是否需要重试
    // 1. 不重试登录和注册请求
    if (error.config?.url?.includes('/login') || error.config?.url?.includes('/register')) {
      return false
    }
    
    // 2. 不重试用户主动取消的请求
    if (axios.isCancel(error)) {
      return false
    }
    
    // 3. 根据错误类型判断是否重试
    return error.code === 'ECONNABORTED' || // 超时
           !error.response || // 网络错误
           error.code === 'ERR_NETWORK' || // 网络连接错误
           [500, 502, 503, 504].includes(error.response?.status) // 服务器错误
  }
}

// 创建 axios 实例
const instance = axios.create(axiosConfig)

// 添加请求拦截器用于调试和认证
instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('[DEBUG] 请求错误:', error)
    return Promise.reject(error)
  }
)

// 用于记录是否正在刷新 token
let isRefreshing = false;
// 存储等待 token 刷新的请求
let waitingRequests = [];

// 刷新 token 的函数
const refreshToken = async () => {
  try {
    const response = await axios.post('/api/user/refresh-token', null, {
      skipAuthRetry: true // 防止无限循环
    })
    if (response.data.success) {
      const newToken = response.data.data
      localStorage.setItem('token', newToken)
      return newToken
    }
    return null
  } catch (error) {
    console.error('刷新token失败:', error)
    return null
  }
}

// 添加响应拦截器
instance.interceptors.response.use(
  // 处理正常响应
  response => {
    return response
  },
  // 处理错误响应
  async error => {
    const { config, response } = error

    // 如果响应不存在，说明是网络错误
    if (!response) {
      emitter.emit('notify', '网络连接失败，请检查网络设置', 'error')
      return Promise.reject(error)
    }
    
    // 如果返回 401 且不是登录或验证 token 请求
    if (response.status === 401 && 
        !config.url.includes('/login') && 
        !config.url.includes('/verify-token') && 
        !config.skipAuthRetry) {
      
      if (!isRefreshing) {
        isRefreshing = true
        
        try {
          const newToken = await refreshToken()
          
          if (newToken) {
            // 更新 Authorization 头
            config.headers['Authorization'] = `Bearer ${newToken}`
            
            // 重试所有等待的请求
            waitingRequests.forEach(({ config, resolve }) => {
              config.headers['Authorization'] = `Bearer ${newToken}`
              resolve(instance(config))
            })
            
            // 重试当前请求
            return instance(config)
          }
        } catch (refreshError) {
          console.error('Token刷新失败:', refreshError)
          // 处理所有等待的请求
          waitingRequests.forEach(({ reject }) => {
            reject(error)
          })
          
          // 清理登录状态并显示登录框
          const userStore = useUserStore()
          await userStore.logout(false) // 不清除UI状态
          emitter.emit('show-auth', 'login', 'token已过期，请重新登录')
        } finally {
          waitingRequests = []
          isRefreshing = false
        }
      }
      
      // 将请求加入等待队列
      return new Promise((resolve, reject) => {
        waitingRequests.push({ config, resolve, reject })
      })
    }

    // 处理 401 未授权错误
    if (response.status === 401) {
      const userStore = useUserStore()
      
      // 如果是登录请求本身返回 401，说明登录失败
      if (config.url.includes('/login')) {
        return Promise.reject(error)
      }
      
      // 如果是登出请求，即使返回 401 也继续处理
      if (config.url.includes('/logout')) {
        return Promise.reject(error)
      }

      // 清理登录状态
      await userStore.logout(false) // 不主动清除UI状态
      
      // 显示登录框
      emitter.emit('show-auth', 'login', '登录已过期，请重新登录')
      
      return Promise.reject(error)
    }

    // 处理 403 禁止访问错误
    if (response.status === 403) {
      emitter.emit('notify', '没有权限执行此操作', 'error')
      return Promise.reject(error)
    }

    // 处理 422 验证错误
    if (response.status === 422) {
      const message = response.data?.message || '输入数据验证失败'
      emitter.emit('notify', message, 'error')
      return Promise.reject(error)
    }

    // 处理 500 等服务器错误
    if (response.status >= 500) {
      emitter.emit('notify', '服务器暂时无法响应，请稍后重试', 'error')
      
      // 如果配置了重试且应该重试
      if (config.retry && axiosConfig.shouldRetry(error)) {
        config.retry--
        // 延迟重试
        await new Promise(resolve => setTimeout(resolve, axiosConfig.retryDelay))
        return instance(config)
      }
      
      return Promise.reject(error)
    }

    // 其他错误
    const message = response.data?.message || '请求失败，请重试'
    emitter.emit('notify', message, 'error')
    return Promise.reject(error)
  }
)

export default instance