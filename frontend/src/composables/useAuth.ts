// src/composables/useAuth.ts
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/userStore'
import emitter from '@/utils/eventBus'
import axiosInstance from '@/api/axios'
import { tokenService } from '@/api/tokenService'
import type { AxiosRequestConfig } from 'axios'

// 🔥 改进类型定义
interface PendingRetryRequest {
  config: AxiosRequestConfig
  resolve: (value: any) => void
  reject: (error: any) => void
  timestamp: number // 添加时间戳，用于超时处理
}

interface AuthAction {
  (): Promise<void>
}

// 全局状态 - 弹窗显示状态
const isShowingLoginModal = ref(false)
const isShowingRegisterModal = ref(false)

// 全局状态 - 待重试的请求队列
const pendingRetryRequests = ref<PendingRetryRequest[]>([])

// 全局状态 - 待执行的认证后操作
const pendingAuthAction = ref<AuthAction | null>()

// 防止重复触发认证失败处理
const isAuthFailed = ref(false)

// 确保事件监听只设置一次
let eventListenerSetup = false
let eventCleanupFunctions: Array<() => void> = []

// 🔥 添加请求超时清理
const REQUEST_TIMEOUT = 5 * 60 * 1000 // 5分钟超时

function setupEventListeners() {
  if (eventListenerSetup) return

  const handleShowLogin = () => {
    // 🔥 更严格的重复检查
    if (isShowingLoginModal.value) {
      console.log('🟡 登录弹窗已显示，忽略重复事件')
      return
    }

    // 🔥 检查用户是否已登录
    const userStore = useUserStore()
    if (userStore.isLoggedIn && userStore.token) {
      console.log('🟡 用户已登录，忽略登录弹窗事件')
      return
    }

    console.log('🟢 收到 show-login 事件，显示登录弹窗')
    isShowingLoginModal.value = true
    isShowingRegisterModal.value = false
  }

  emitter.on('show-login', handleShowLogin)

  // 🔥 添加清理函数
  eventCleanupFunctions.push(() => {
    emitter.off('show-login', handleShowLogin)
  })

  // 🔥 定期清理超时的待重试请求
  const cleanupInterval = setInterval(() => {
    cleanupExpiredRequests()
  }, 60000) // 每分钟清理一次

  eventCleanupFunctions.push(() => {
    clearInterval(cleanupInterval)
  })

  eventListenerSetup = true
}

// 🔥 清理过期的待重试请求
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
    console.log(`🟡 清理 ${expiredRequests.length} 个过期的待重试请求`)
    expiredRequests.forEach(({ reject }) => {
      reject(new Error('请求超时'))
    })
    pendingRetryRequests.value = validRequests
  }
}

// 🔥 全局清理函数
function cleanup() {
  eventCleanupFunctions.forEach(cleanupFn => cleanupFn())
  eventCleanupFunctions = []
  eventListenerSetup = false

  // 清理所有待重试的请求
  pendingRetryRequests.value.forEach(({ reject }) => {
    reject(new Error('系统清理'))
  })
  pendingRetryRequests.value = []

  // 重置状态
  isAuthFailed.value = false
  pendingAuthAction.value = null
  isShowingLoginModal.value = false
  isShowingRegisterModal.value = false
}

export function useAuth() {
  const userStore = useUserStore()

  // 确保事件监听已设置
  setupEventListeners()

  // 🔥 移除 onBeforeUnmount，因为这是全局 composable，不应该依赖组件生命周期

  // 用户状态相关
  const isLoggedIn = computed(() => userStore.isLoggedIn && !!userStore.token)
  const token = computed(() => userStore.token)
  const user = computed(() => userStore.user)
  const userRoles = computed(() => user.value?.roles || [])

  const hasRole = (role: string) => userRoles.value.includes(role)

  function clearToken() {
    userStore.clearAuth(false) // 不跳转
  }

  // 弹窗控制方法
  function showLogin(action?: AuthAction) {
    if (action) {
      pendingAuthAction.value = action
    }
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

  // 请求重试相关方法
  function addPendingRetryRequest(
      config: AxiosRequestConfig,
      resolve: (value: any) => void,
      reject: (error: any) => void
  ) {
    // 🔥 如果已经认证失败，直接拒绝新请求
    if (isAuthFailed.value) {
      console.log('🔴 认证已失败，拒绝新请求:', config.url)
      reject(new Error('用户未登录，请先登录'))
      return
    }

    // 🔥 检查是否已有相同的请求
    const requestKey = `${config.method}-${config.url}`
    const existingRequest = pendingRetryRequests.value.find(req =>
        `${req.config.method}-${req.config.url}` === requestKey
    )

    if (existingRequest) {
      console.log('🟡 相同请求已在队列中，合并处理:', config.url)
      // 可以选择合并或替换，这里选择忽略新请求
      reject(new Error('重复请求'))
      return
    }

    console.log('🟡 添加待重试请求:', config.url)
    pendingRetryRequests.value.push({
      config,
      resolve,
      reject,
      timestamp: Date.now()
    })
  }

  function clearPendingRetryRequests() {
    const count = pendingRetryRequests.value.length
    console.log('🔴 清空待重试请求，数量:', count)

    // 🔥 分批处理，避免一次性处理太多请求导致性能问题
    const batchSize = 10
    for (let i = 0; i < pendingRetryRequests.value.length; i += batchSize) {
      const batch = pendingRetryRequests.value.slice(i, i + batchSize)
      batch.forEach(({ reject }) => {
        reject(new Error('AUTH_CANCELED'))
      })
    }

    pendingRetryRequests.value = []
  }

  // 使用新token重试所有待重试的请求
  async function retryAllPendingRequestsWithToken(newToken: string) {
    if (pendingRetryRequests.value.length === 0) return

    console.log('🟢 开始重试所有待重试请求，数量:', pendingRetryRequests.value.length)
    const requests = [...pendingRetryRequests.value]
    pendingRetryRequests.value = []

    // 🔥 分批处理请求，避免并发过多
    const batchSize = 5
    for (let i = 0; i < requests.length; i += batchSize) {
      const batch = requests.slice(i, i + batchSize)

      const batchPromises = batch.map(async ({ config, resolve, reject }) => {
        try {
          // 🔥 深拷贝配置，避免修改原始配置
          const newConfig = { ...config }
          newConfig.headers = { ...config.headers }
          newConfig.headers['Authorization'] = `Bearer ${newToken}`

          const response = await axiosInstance(newConfig)
          resolve(response)
        } catch (error) {
          console.error('重试请求失败:', config.url, error)
          reject(error)
        }
      })

      // 等待当前批次完成再处理下一批次
      await Promise.allSettled(batchPromises)

      // 🔥 添加小延迟，避免请求过于密集
      if (i + batchSize < requests.length) {
        await new Promise(resolve => setTimeout(resolve, 100))
      }
    }
  }

  // 🔥 新增：清理浏览器记忆的方法
  function clearBrowserMemory() {
    try {
      console.log('🟡 清理浏览器记忆...')
      tokenService.clearBrowserMemoryExceptAuth()
      console.log('🟢 浏览器记忆清理完成')
    } catch (error) {
      console.error('🔴 清理浏览器记忆失败:', error)
    }
  }

  // 登录成功后的完整处理逻辑
  async function onLoginSuccess() {
    try {
      console.log('🟢 登录成功，开始处理后续逻辑')

      // 🔥 重置认证失败状态
      isAuthFailed.value = false

      // 🔥 1. 首先清理浏览器记忆（在获取用户信息之前）
      clearBrowserMemory()

      // 2. 获取用户信息
      await userStore.fetchUser()

      // 3. 处理待重试的请求
      if (pendingRetryRequests.value.length > 0) {
        console.log('🟢 处理待重试请求，数量:', pendingRetryRequests.value.length)
        const currentToken = userStore.token
        if (currentToken) {
          await retryAllPendingRequestsWithToken(currentToken)
        } else {
          console.warn('🟡 登录成功但未获取到token，清空待重试请求')
          clearPendingRetryRequests()
        }
      }

      // 4. 处理其他待执行的操作
      if (pendingAuthAction.value) {
        const action = pendingAuthAction.value
        pendingAuthAction.value = null
        try {
          await action()
        } catch (actionError) {
          console.error('🔴 执行认证后操作失败:', actionError)
          // 不抛出错误，避免影响其他逻辑
        }
      }

      // 5. 发出全局登录成功事件
      emitter.emit('login-success')

      // 6. 关闭弹窗
      hideAll()

    } catch (error) {
      console.error('登录成功后处理操作时出错:', error)
      // 出错时清空所有待重试请求
      clearPendingRetryRequests()
      throw error // 重新抛出错误，让调用方知道处理失败
    } finally {
      // 确保状态重置
      pendingAuthAction.value = null
    }
  }

  // 认证失败时的清理工作
  function onAuthFailed() {
    // 🔥 防止重复执行
    if (isAuthFailed.value) {
      console.log('🟡 认证失败处理已执行，跳过')
      return
    }

    console.log('🔴 认证失败，清理状态')
    isAuthFailed.value = true

    // 🔥 延迟清空，避免正在进行的请求立即失败
    setTimeout(() => {
      clearPendingRetryRequests()
    }, 100)

    pendingAuthAction.value = null
    clearToken()
  }

  // 🔥 重置认证状态（用于登录成功后或手动重置）
  function resetAuthState() {
    console.log('🟢 重置认证状态')
    isAuthFailed.value = false
    pendingAuthAction.value = null
    hideAll()
  }

  // 检查是否应该拒绝请求
  function shouldRejectRequest() {
    return isAuthFailed.value
  }

  // 🔥 获取待重试请求的统计信息
  function getRetryRequestsStats() {
    return {
      count: pendingRetryRequests.value.length,
      oldestTimestamp: pendingRetryRequests.value.length > 0
          ? Math.min(...pendingRetryRequests.value.map(req => req.timestamp))
          : null
    }
  }

  return {
    // 用户状态
    isLoggedIn,
    token,
    user,
    userRoles,
    hasRole,
    clearToken,

    // 弹窗状态
    isShowingLoginModal,
    isShowingRegisterModal,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    hideAll,
    handleSwitchToLogin,
    handleSwitchToRegister,

    // 请求重试
    pendingRetryRequests: computed(() => pendingRetryRequests.value), // 只读访问
    addPendingRetryRequest,
    clearPendingRetryRequests,
    retryAllPendingRequestsWithToken,
    shouldRejectRequest,

    // 认证后操作
    pendingAuthAction: computed(() => pendingAuthAction.value), // 只读访问
    onLoginSuccess,
    onAuthFailed,
    resetAuthState, // 🔥 新增

    // 🔥 工具方法
    getRetryRequestsStats,
    cleanup: cleanup, // 🔥 暴露清理方法供必要时手动调用
    clearBrowserMemory, // 🔥 新增：暴露清理浏览器记忆的方法
  }
}

// 🔥 导出清理函数，供应用关闭时调用
export function cleanupAuth() {
  cleanup()
}