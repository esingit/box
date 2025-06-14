import { ref, computed, Ref } from 'vue'
import { useUserStore } from '@/store/userStore'
import emitter from '@/utils/eventBus'

interface TokenPayload {
  exp: number
      [key: string]: any
}

type AuthCallback = (() => void) | null

export function useAuth() {
  const userStore = useUserStore()

  const isShowingLoginModal = ref(false)
  const isShowingRegisterModal = ref(false)
  const pendingAuthAction: Ref<AuthCallback> = ref(null)
  const pendingAuthMessage = ref<string | null>(null)
  const tokenCheckTimer = ref<number | null>(null)

  const isLoggedIn = computed(() => userStore.isLoggedIn)

  async function checkTokenStatus(): Promise<boolean> {
    try {
      const token = localStorage.getItem('token')
      if (!token) {
        await userStore.logout(false)
        return false
      }
      return await userStore.verifyToken()
    } catch (error) {
      console.error('Token验证失败:', error)
      await userStore.logout(false)
      return false
    }
  }

  function startTokenCheck(): void {
    if (tokenCheckTimer.value) {
      clearTimeout(tokenCheckTimer.value)
    }

    const token = localStorage.getItem('token')
    if (token) {
      try {
        const payload: TokenPayload = JSON.parse(atob(token.split('.')[1]))
        const expirationTime = payload.exp * 1000
        const currentTime = Date.now()
        const timeUntilExpiry = expirationTime - currentTime
        const refreshThreshold = 10 * 60 * 1000
        const nextCheckTime = Math.max(60000, Math.min(timeUntilExpiry - refreshThreshold, 300000))

        tokenCheckTimer.value = window.setTimeout(async () => {
          const isValid = await checkTokenStatus()
          if (isValid) startTokenCheck()
        }, nextCheckTime)
      } catch (error) {
        console.error('Token解析失败:', error)
        userStore.logout(false)
      }
    }
  }

  function stopTokenCheck(): void {
    if (tokenCheckTimer.value) {
      clearTimeout(tokenCheckTimer.value)
      tokenCheckTimer.value = null
    }
  }

  async function checkAuthState(): Promise<boolean> {
    const isValid = await checkTokenStatus()
    if (isValid) startTokenCheck()
    return isValid
  }

  const showLogin = (message = '', callback: AuthCallback = null): void => {
    if (!isShowingLoginModal.value) {
      pendingAuthMessage.value = message
      pendingAuthAction.value = callback

      if (message) emitter.emit('login-error', message)

      isShowingLoginModal.value = true
      emitter.emit('auth-state-changed', false)
    }
  }

  const hideLogin = (): void => {
    isShowingLoginModal.value = false
    pendingAuthAction.value = null
    pendingAuthMessage.value = null
  }

  const showRegister = (): void => {
    if (!isShowingRegisterModal.value) {
      isShowingRegisterModal.value = true
    }
  }

  const hideRegister = (): void => {
    isShowingRegisterModal.value = false
  }

  async function initAuth(): Promise<boolean> {
    return await checkAuthState()
  }

  function cleanup(): void {
    stopTokenCheck()
  }

  return {
    isShowingLoginModal,
    isShowingRegisterModal,
    pendingAuthAction,
    pendingAuthMessage,
    isLoggedIn,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    checkAuthState,
    initAuth,
    cleanup
  }
}

export function initializeAuth(): Promise<boolean> {
  const auth = useAuth()
  return auth.initAuth()
}
