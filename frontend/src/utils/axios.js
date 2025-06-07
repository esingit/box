import axios from 'axios'
import { useUserStore } from '@/stores/userStore';
import emitter from '@/utils/eventBus'

// axios 配置
const axiosConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000, // 超时时间设置为 15 秒
  withCredentials: true, // 允许跨域请求携带 cookie
  retry: 3, // 重试次数
  retryDelay: 1500, // 重试延迟时间
  shouldRetry: (error) => {
    // 不重试的情况
    if (error.config?.url?.includes('/login') || 
        error.config?.url?.includes('/register') ||
        axios.isCancel(error)) {
      return false
    }
    
    // 根据错误类型判断是否重试
    return error.code === 'ECONNABORTED' || // 超时
           !error.response || // 网络错误
           error.code === 'ERR_NETWORK' || // 网络连接错误
           [500, 502, 503, 504].includes(error.response?.status) // 服务器错误
  }
}

// 创建 axios 实例
const instance = axios.create(axiosConfig)

// 用于存储正在进行的请求的Map
const pendingRequests = new Map()

// 生成请求的唯一key
const generateRequestKey = (config) => {
  const { url, method, params, data } = config
  const queryStr = params ? `?${new URLSearchParams(params).toString()}` : ''
  return `${method}:${url}${queryStr}${data ? ':' + JSON.stringify(data) : ''}`
}

// 取消重复的请求
function cancelPendingRequests(message = '取消重复请求') {
  for (const [, controller] of pendingRequests) {
    controller.abort(message)
  }
  pendingRequests.clear()
}

// 添加请求拦截器
instance.interceptors.request.use(
  config => {
    const requestKey = generateRequestKey(config)
    
    // 允许重复请求的接口
    const allowDuplicate = config.allowDuplicate || 
      config.url.includes('/login') || 
      config.url.includes('/register') ||
      config.url.includes('/refresh-token') ||
      config.url.includes('/verify-token')
    
    if (!allowDuplicate) {
      // 取消前一个相同的请求
      if (pendingRequests.has(requestKey)) {
        const previousRequest = pendingRequests.get(requestKey)
        previousRequest.abort('取消重复请求')
        pendingRequests.delete(requestKey)
      }

      // 创建新的取消控制器
      const controller = new AbortController()
      config.signal = controller.signal
      pendingRequests.set(requestKey, controller)
    }
    
    // 添加认证头
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

// token 刷新相关状态
let isRefreshing = false
let waitingRequests = []

// 刷新 token
async function refreshToken() {
  try {
    const oldToken = localStorage.getItem('token')
    if (!oldToken) {
      throw new Error('No token found')
    }
    
    // 先验证 token 状态
    const verifyResponse = await instance.get('/api/user/verify-token', {
      headers: { Authorization: `Bearer ${oldToken}` },
      skipAuthRetry: true
    })
    
    // token 仍然有效且需要刷新
    if (verifyResponse.data.success && verifyResponse.data.shouldRefresh) {
      const response = await instance.post('/api/user/refresh-token', null, {
        headers: { Authorization: `Bearer ${oldToken}` },
        skipAuthRetry: true
      })
      
      if (response.data?.success) {
        const newToken = response.data.data
        localStorage.setItem('token', newToken)
        instance.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
        return newToken
      }
    } else if (verifyResponse.data.success) {
      // token 仍然有效,不需要刷新
      return oldToken
    }
    
    throw new Error('Token refresh failed')
  } catch (error) {
    console.error('刷新token失败:', error)
    throw error
  }
}

// 错误处理函数
async function handleAuthError(error, userStore) {
  isRefreshing = false
  waitingRequests = []
  
  // 清理登录状态
  await userStore.logout(false)
  
  // 取消所有待处理的请求
  cancelPendingRequests('登录已过期')
  
  // 确保状态更新后再显示登录框
  const { nextTick } = await import('vue')
  await nextTick()
  
  const { useAuth } = await import('@/composables/useAuth')
  const { showLogin } = useAuth()
  showLogin('登录已过期，请重新登录')
  
  return Promise.reject(error)
}

// 响应拦截器
instance.interceptors.response.use(
  response => {
    // 清理已完成的请求
    if (response.config) {
      pendingRequests.delete(generateRequestKey(response.config))
    }
    return response
  },
  async error => {
    // 清理已完成的请求
    if (error.config) {
      pendingRequests.delete(generateRequestKey(error.config))
    }

    // 处理已取消的请求
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const { config, response } = error

    // 处理网络错误
    if (!response) {
      emitter.emit('notify', '网络连接失败，请检查网络设置', 'error')
      return Promise.reject(error)
    }

    // 获取 userStore 实例
    const userStore = useUserStore()
    
    // 处理 401 未授权错误
    if (response.status === 401) {
      // 处理特殊请求
      if (config.url.includes('/login') || 
          config.url.includes('/logout') || 
          config.skipAuthRetry) {
        return Promise.reject(error)
      }

      try {
        if (!isRefreshing) {
          isRefreshing = true
          const newToken = await refreshToken()
          
          if (newToken) {
            // 重试当前请求
            config.headers['Authorization'] = `Bearer ${newToken}`
            
            // 处理等待队列中的请求
            waitingRequests.forEach(({ config: pendingConfig, resolve }) => {
              pendingConfig.headers['Authorization'] = `Bearer ${newToken}`
              resolve(instance(pendingConfig))
            })
            
            waitingRequests = []
            isRefreshing = false
            
            // 重试当前请求
            return instance(config)
          }
          
          return handleAuthError(error, userStore)
        }
        
        // 其他请求加入等待队列
        return new Promise((resolve, reject) => {
          waitingRequests.push({ config, resolve, reject })
        })
      } catch (refreshError) {
        return handleAuthError(refreshError, userStore)
      }
    }

    // 处理其他错误
    if (response.status === 403) {
      emitter.emit('notify', '没有权限执行此操作', 'error')
    } else if (response.status === 422) {
      const message = response.data?.message || '输入数据验证失败'
      emitter.emit('notify', message, 'error')
    } else if (response.status >= 500) {
      emitter.emit('notify', '服务器暂时无法响应，请稍后重试', 'error')
      
      // 处理重试逻辑
      if (config.retry && axiosConfig.shouldRetry(error)) {
        config.retry--
        await new Promise(resolve => setTimeout(resolve, axiosConfig.retryDelay))
        return instance(config)
      }
    } else {
      const message = response.data?.message || '请求失败，请重试'
      emitter.emit('notify', message, 'error')
    }

    return Promise.reject(error)
  }
)

export default instance