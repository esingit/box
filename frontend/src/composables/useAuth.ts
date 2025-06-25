// src/composables/useAuth.ts
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/userStore'
import emitter from '@/utils/eventBus'
import axiosInstance from '@/api/axios'
import { tokenService } from '@/api/tokenService'
import type { AxiosRequestConfig } from 'axios'

interface PendingRetryRequest {
  config: AxiosRequestConfig
  resolve: (value: any) => void
  reject: (error: any) => void
  timestamp: number
}

interface AuthAction {
  (): Promise<void>
}

const isShowingLoginModal = ref(false)
const isShowingRegisterModal = ref(false)
const pendingRetryRequests = ref<PendingRetryRequest[]>([])
const pendingAuthAction = ref<AuthAction | null>(null)
const isAuthFailed = ref(false)

let eventListenerSetup = false
let eventCleanupFunctions: Array<() => void> = []

const REQUEST_TIMEOUT = 5 * 60 * 1000

function setupEventListeners() {
  if (eventListenerSetup) return

  const handleShowLogin = () => {
    if (isShowingLoginModal.value) return
    const userStore = useUserStore()
    if (userStore.isLoggedIn && userStore.token) return

    isShowingLoginModal.value = true
    isShowingRegisterModal.value = false
  }

  emitter.on('show-login', handleShowLogin)
  const loginListener = () => handleShowLogin()
  window.addEventListener('force-login', loginListener)

  eventCleanupFunctions.push(() => {
    emitter.off('show-login', handleShowLogin)
    window.removeEventListener('force-login', loginListener)
  })

  const cleanupInterval = setInterval(() => {
    cleanupExpiredRequests()
  }, 60000)

  eventCleanupFunctions.push(() => clearInterval(cleanupInterval))
  eventListenerSetup = true
}

function cleanupExpiredRequests() {
  const now = Date.now()
  const validRequests: PendingRetryRequest[] = []
  const expiredRequests: PendingRetryRequest[] = []

  pendingRetryRequests.value.forEach(request => {
    if (now - request.timestamp > REQUEST_TIMEOUT) {
      expiredRequests.push(request)
    } else {
      validRequests.push(request)
    }
  })

  if (expiredRequests.length > 0) {
    expiredRequests.forEach(({ reject }) => reject(new Error('请求超时')))
    pendingRetryRequests.value = validRequests
  }
}

function cleanup() {
  eventCleanupFunctions.forEach(fn => fn())
  eventCleanupFunctions = []
  eventListenerSetup = false

  pendingRetryRequests.value.forEach(({ reject }) => reject(new Error('系统清理')))
  pendingRetryRequests.value = []

  isAuthFailed.value = false
  pendingAuthAction.value = null
  isShowingLoginModal.value = false
  isShowingRegisterModal.value = false
}

export function useAuth() {
  const userStore = useUserStore()

  setupEventListeners()

  const isLoggedIn = computed(() => userStore.isLoggedIn && !!userStore.token)
  const token = computed(() => userStore.token)
  const user = computed(() => userStore.user)
  const userRoles = computed(() => user.value?.roles || [])

  const hasRole = (role: string) => userRoles.value.includes(role)

  function clearToken() {
    userStore.clearAuth(false)
  }

  function showLogin(action?: AuthAction) {
    if (action) pendingAuthAction.value = action
    isShowingLoginModal.value = true
    isShowingRegisterModal.value = false
  }

  function hideLogin() {
    isShowingLoginModal.value = false
  }

  function showRegister() {
    isShowingRegisterModal.value = true
    isShowingLoginModal.value = false
  }

  function hideRegister() {
    isShowingRegisterModal.value = false
  }

  function hideAll() {
    isShowingLoginModal.value = false
    isShowingRegisterModal.value = false
  }

  function handleSwitchToLogin() {
    showLogin()
  }

  function handleSwitchToRegister() {
    showRegister()
  }

  function addPendingRetryRequest(
      config: AxiosRequestConfig,
      resolve: (value: any) => void,
      reject: (error: any) => void
  ) {
    if (isAuthFailed.value) {
      reject(new Error('用户未登录，请先登录'))
      return
    }

    // ✅ 不再 reject 重复请求，允许多个请求共享 token 刷新
    pendingRetryRequests.value.push({
      config,
      resolve,
      reject,
      timestamp: Date.now()
    })
  }

  function clearPendingRetryRequests() {
    pendingRetryRequests.value.forEach(({ reject }) => reject(new Error('AUTH_CANCELED')))
    pendingRetryRequests.value = []
  }

  async function retryAllPendingRequestsWithToken(newToken: string) {
    if (pendingRetryRequests.value.length === 0) return

    const requests = [...pendingRetryRequests.value]
    pendingRetryRequests.value = []

    const batchSize = 5
    for (let i = 0; i < requests.length; i += batchSize) {
      const batch = requests.slice(i, i + batchSize)
      const batchPromises = batch.map(async ({ config, resolve, reject }) => {
        try {
          const newConfig = { ...config, headers: { ...config.headers, Authorization: `Bearer ${newToken}` } }
          const response = await axiosInstance(newConfig)
          resolve(response)
        } catch (error) {
          reject(error)
        }
      })
      await Promise.allSettled(batchPromises)
      if (i + batchSize < requests.length) {
        await new Promise(r => setTimeout(r, 100))
      }
    }
  }

  function clearBrowserMemory() {
    try {
      tokenService.clearBrowserMemoryExceptAuth()
    } catch (error) {
      console.error('清理浏览器记忆失败:', error)
    }
  }

  async function onLoginSuccess() {
    try {
      isAuthFailed.value = false
      clearBrowserMemory()

      // 不需要再获取用户信息，login 时已经获取了
      // await userStore.fetchUser()

      // 清空等待队列，因为新的 token 刷新机制会处理重试
      if (pendingRetryRequests.value.length > 0) {
        clearPendingRetryRequests()
      }

      if (pendingAuthAction.value) {
        const action = pendingAuthAction.value
        pendingAuthAction.value = null
        try {
          await action()
        } catch {}
      }

      emitter.emit('login-success')
      hideAll()
    } catch (error) {
      clearPendingRetryRequests()
      throw error
    } finally {
      pendingAuthAction.value = null
    }
  }

  function onAuthFailed() {
    if (isAuthFailed.value) return

    isAuthFailed.value = true
    setTimeout(() => clearPendingRetryRequests(), 100)
    pendingAuthAction.value = null
    clearToken()
  }

  function resetAuthState() {
    isAuthFailed.value = false
    pendingAuthAction.value = null
    hideAll()
  }

  function shouldRejectRequest() {
    return isAuthFailed.value
  }

  function getRetryRequestsStats() {
    return {
      count: pendingRetryRequests.value.length,
      oldestTimestamp: pendingRetryRequests.value.length > 0
          ? Math.min(...pendingRetryRequests.value.map(req => req.timestamp))
          : null
    }
  }

  return {
    isShowingLoginModal,
    isShowingRegisterModal,
    isLoggedIn,
    token,
    user,
    userRoles,
    hasRole,
    clearToken,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    hideAll,
    handleSwitchToLogin,
    handleSwitchToRegister,
    addPendingRetryRequest,
    clearPendingRetryRequests,
    retryAllPendingRequestsWithToken,
    clearBrowserMemory,
    onLoginSuccess,
    onAuthFailed,
    resetAuthState,
    shouldRejectRequest,
    getRetryRequestsStats,
    cleanup,
  }
}