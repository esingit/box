// src/composable/useAuth.ts
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/userStore'
import emitter from '@/utils/eventBus'

export function useAuth() {
  const userStore = useUserStore()

  const isLoggedIn = computed(() => userStore.isLoggedIn && !!userStore.token)
  const token = computed(() => userStore.token)
  const user = computed(() => userStore.user)
  const userRoles = computed(() => user.value?.roles || [])

  const hasRole = (role: string) => userRoles.value.includes(role)

  function clearToken() {
    userStore.clearAuth(false) // 不跳转
  }

  // 登录后要继续的操作
  const pendingAuthAction = ref<null | (() => Promise<void>)>(null)

  // 触发登录弹窗
  function showLogin(action?: () => Promise<void>) {
    if (action) {
      pendingAuthAction.value = action
    }
    emitter.emit('show-login')
  }

  // 登录成功后继续之前的操作，并发出全局成功事件
  async function onLoginSuccess() {
    try {
      if (pendingAuthAction.value) {
        const action = pendingAuthAction.value
        pendingAuthAction.value = null
        await action()
      }
      emitter.emit('login-success')
    } finally {
      // 确保状态重置，防止逻辑异常
      pendingAuthAction.value = null
    }
  }

  return {
    isLoggedIn,
    token,
    user,
    hasRole,
    clearToken,
    pendingAuthAction,
    showLogin,
    onLoginSuccess,
  }
}
