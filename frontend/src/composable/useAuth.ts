// useAuth.ts
import { computed, ref } from 'vue'
import { useUserStore } from '@/store/userStore'

export function useAuth() {
  const userStore = useUserStore()

  const isLoggedIn = computed(() => !!userStore.token)  // 推荐直接判断 token

  const token = computed(() => userStore.token)
  const user = computed(() => userStore.user)
  const userRoles = computed(() => user.value?.roles || [])

  const hasRole = (role: string) => userRoles.value.includes(role)

  function clearToken() {
    userStore.clearAuth()
  }

  const pendingAuthAction = ref<null | (() => Promise<void>)>(null)

  return {
    isLoggedIn,
    token,
    user,
    hasRole,
    clearToken,
    pendingAuthAction,
  }
}
