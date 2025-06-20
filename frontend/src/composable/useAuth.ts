import { computed, ref } from 'vue'
import { useUserStore } from '@/store/userStore'

export function useAuth() {
  const userStore = useUserStore()

  const isLoggedIn = computed(() => !!userStore.token)
  const token = computed(() => userStore.token)
  const user = computed(() => userStore.user)
  const userRoles = computed(() => user.value?.roles || [])

  const hasRole = (role: string) => userRoles.value.includes(role)

  function clearToken() {
    userStore.clearAuth()
  }

  const pendingAuthAction = ref<null | (() => Promise<void>)>(null)

  const showLogin = () => {
    import('@/utils/eventBus').then(({ default: emitter }) => {
      emitter.emit('show-login')
    })
  }

  return {
    isLoggedIn,
    token,
    user,
    hasRole,
    clearToken,
    pendingAuthAction,
    showLogin,
  }
}
