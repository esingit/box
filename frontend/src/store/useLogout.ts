import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import emitter from '@/utils/eventBus'

export const useLogoutStore = defineStore('logout', () => {
  const router = useRouter()
  const userStore = useUserStore()

  const clearUserState = () => {
    localStorage.removeItem('token')
    userStore.token = null
    localStorage.removeItem('user')
    userStore.user = null
    userStore.isLoggedIn = false
    userStore.clearAuthHeader()
  }

  const handleLogout = async (clearUI = true) => {
    try {
      clearUserState()

      let logoutSuccess = true

      if (clearUI) {
        try {
          await userStore.callLogoutAPI()
        } catch (error) {
          console.error('登出请求失败:', error)
          logoutSuccess = false
        }
      }

      try {
        await router.push('/home')
      } catch (routerError) {
        console.error('页面跳转失败:', routerError)
        window.location.href = '/'
      }

      if (clearUI) {
        emitter.emit('notify', {
          message: logoutSuccess ? '已成功注销' : '已清理本地登录状态，但后端注销可能未完成',
          type: logoutSuccess ? 'success' : 'info'
        })
      }
    } catch (error) {
      console.error('注销过程发生错误:', error)
      clearUserState()
      window.location.href = '/'
      emitter.emit('notify', {
        message: '注销过程遇到问题，但已确保安全退出',
        type: 'warning'
      })
    }
  }

  return {
    handleLogout
  }
})
