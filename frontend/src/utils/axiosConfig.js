export const axiosConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000,
  withCredentials: true,
  retry: 3,
  retryDelay: 1500,
  shouldRetry: (error) => {
    if (error.config?.url?.includes('/login') || 
        error.config?.url?.includes('/register') ||
        axios.isCancel(error)) {
      return false
    }
    
    return error.code === 'ECONNABORTED' ||
           !error.response ||
           error.code === 'ERR_NETWORK' ||
           [500, 502, 503, 504].includes(error.response?.status)
  }
}

export const ALLOWED_DUPLICATE_ENDPOINTS = [
  '/login',
  '/register',
  '/refresh-token',
  '/verify-token'
]
