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

// 添加重试拦截器
instance.interceptors.response.use(undefined, async (error) => {
  const config = error.config
  
  // 如果配置了重试，并且需要重试
  if (config.retry && axiosConfig.shouldRetry(error)) {
    config.__retryCount = config.__retryCount || 0
    
    // 判断是否超过重试次数
    if (config.__retryCount < config.retry) {
      config.__retryCount++
      
      // 延迟重试
      await new Promise(resolve => setTimeout(resolve, config.retryDelay))
      console.debug(`[API] Retrying request (${config.__retryCount}/${config.retry}):`, config.url)
      
      // 重新发起请求
      return instance(config)
    }
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
  async error => {
    if (error.response) {
      const { status } = error.response
      
      // 处理 401（未授权）、403（权限不足）以及其他 token 相关错误
      if (status === 401 || 
          status === 403 ||
          error.response.data?.message?.toLowerCase().includes('token')) {
            
        // 如果当前请求已经是登录请求，则不需要重试
        if (error.config.url.includes('/login')) {
          return Promise.reject(error)
        }
        
        console.debug('Token 无效或过期，准备重新登录:', error.response.data?.message);
        
        // 清除用户状态
        const userStore = useUserStore()
        userStore.logout() // 使用 store 的 logout 方法来清理状态
        
        // 保存当前失败的请求配置
        const failedRequest = error.config
        
        // 触发登录弹窗事件
        return new Promise((resolve, reject) => {
          let isHandled = false; // 标记是否已经处理过响应
          
          // 发送显示登录弹窗的事件
          emitter.emit('show-auth', 'login')
          
          // 监听登录成功事件
          const loginSuccessHandler = async (newToken) => {
            if (isHandled) return; // 防止重复处理
            isHandled = true;
            
            if (!newToken) {
              emitter.off('loginSuccess', loginSuccessHandler); // 清理监听器
              reject(new Error('登录失败：未收到新的 token'));
              return;
            }
            
            console.debug('收到新的 token，准备重试请求:', failedRequest.url);
            
            try {
              // 确保移除旧的 token
              delete failedRequest.headers['Authorization'];
              // 使用新token重试之前失败的请求
              const retryResponse = await instance({
                ...failedRequest,
                headers: {
                  ...failedRequest.headers,
                  'Authorization': `Bearer ${newToken}`
                }
              });
              
              console.debug('重试请求成功:', failedRequest.url);
              resolve(retryResponse);
            } catch (retryError) {
              console.error('使用新 token 重试请求失败:', retryError);
              reject(retryError);
            } finally {
              // 确保清理事件监听
              emitter.off('loginSuccess', loginSuccessHandler);
            }
          };
          
          // 添加登录成功的事件监听
          emitter.on('loginSuccess', loginSuccessHandler);
          
          // 添加超时处理
          setTimeout(() => {
            if (!isHandled) {
              isHandled = true;
              emitter.off('loginSuccess', loginSuccessHandler);
              reject(new Error('登录超时'));
            }
          }, 300000); // 5分钟超时
        })
      }
      
      // 处理其他常见错误
      const statusMessages = {
        400: '请求参数错误',
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
  }
)

export default instance