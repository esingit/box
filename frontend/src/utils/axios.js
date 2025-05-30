import axios from 'axios'

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
      // 处理 403 错误
      if (error.response.status === 403) {
        // 可以在这里处理验证码逻辑
        console.log('需要验证码')
      }
    }
    return Promise.reject(error)
  }
)

export default instance 