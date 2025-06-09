import axios from 'axios'
import { useUserStore } from '@/stores/userStore'
import { axiosConfig, ALLOWED_DUPLICATE_ENDPOINTS } from './axiosConfig'
import { generateRequestKey, requestManager } from './requestManager'
import { tokenService } from './tokenService'
import { ErrorHandler } from './errorHandler'

// 创建 axios 实例
const instance = axios.create(axiosConfig)

// 添加请求拦截器
instance.interceptors.request.use(
  config => {
    const requestKey = generateRequestKey(config)
    
    // 检查是否允许重复请求
    const allowDuplicate = config.allowDuplicate || 
      ALLOWED_DUPLICATE_ENDPOINTS.some(endpoint => config.url.includes(endpoint))
    
    if (!allowDuplicate) {
      // 取消前一个相同的请求
      if (requestManager.has(requestKey)) {
        const previousRequest = requestManager.get(requestKey)
        previousRequest.abort('取消重复请求')
        requestManager.delete(requestKey)
      }

      // 创建新的取消控制器
      const controller = new AbortController()
      config.signal = controller.signal
      requestManager.add(requestKey, controller)
    }
    
    // 添加认证头
    const token = tokenService.getToken()
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



// 响应拦截器
instance.interceptors.response.use(
  response => {
    // 清理已完成的请求
    if (response.config) {
      requestManager.delete(generateRequestKey(response.config))
    }
    return response
  },
  async error => {
    // 清理已完成的请求
    if (error.config) {
      requestManager.delete(generateRequestKey(error.config))
    }

    // 处理已取消的请求
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    const { config, response } = error

    // 处理网络错误
    if (!response) {
      ErrorHandler.handleNetworkError()
      return Promise.reject(error)
    }

    // 获取 userStore 实例
    const userStore = useUserStore()
    
    // 处理不同类型的错误
    if (response.status === 401) {
      return ErrorHandler.handle401Error(error, config)
    }

    // 处理其他错误
    ErrorHandler.handleOtherErrors(response.status, response.data)
    
    // 处理重试逻辑
    return ErrorHandler.handleRetry(error, config)
  }
)

export default instance