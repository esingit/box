import axios from './axios'

class TokenService {
  constructor() {
    this.isRefreshing = false
    this.waitingRequests = []
  }

  getToken() {
    return localStorage.getItem('token')
  }

  setToken(token) {
    localStorage.setItem('token', token)
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  }

  async refreshToken() {
    try {
      const oldToken = this.getToken()
      if (!oldToken) {
        throw new Error('No token found')
      }
      
      const verifyResponse = await axios.get('/api/user/verify-token', {
        headers: { Authorization: `Bearer ${oldToken}` },
        skipAuthRetry: true
      })
      
      if (verifyResponse.data.success && verifyResponse.data.shouldRefresh) {
        const response = await axios.post('/api/user/refresh-token', null, {
          headers: { Authorization: `Bearer ${oldToken}` },
          skipAuthRetry: true
        })
        
        if (response.data?.success) {
          const newToken = response.data.data
          this.setToken(newToken)
          return newToken
        }
      } else if (verifyResponse.data.success) {
        return oldToken
      }
      
      throw new Error('Token refresh failed')
    } catch (error) {
      console.error('刷新token失败:', error)
      throw error
    }
  }

  addToWaitingQueue(config) {
    return new Promise((resolve, reject) => {
      this.waitingRequests.push({ config, resolve, reject })
    })
  }

  processWaitingQueue(newToken) {
    this.waitingRequests.forEach(({ config, resolve }) => {
      config.headers['Authorization'] = `Bearer ${newToken}`
      resolve(axios(config))
    })
    this.waitingRequests = []
  }

  clearWaitingQueue() {
    this.waitingRequests = []
  }
}

export const tokenService = new TokenService()
