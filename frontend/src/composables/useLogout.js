import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import emitter from '../utils/eventBus'

export function useLogout() {
  const router = useRouter()
  const userStore = useUserStore()

  const clearUserState = () => {
    // 清除 token
    localStorage.removeItem('token')
    userStore.token = null
    // 清除用户数据
    localStorage.removeItem('user')
    userStore.user = null
    // 更新登录状态
    userStore.isLoggedIn = false
    // 清除 axios 默认 header
    userStore.clearAuthHeader()
  }

  const handleLogout = async (clearUI = true) => {
    try {
      // 先清理前端状态，确保用户体验
      clearUserState()
      
      let logoutSuccess = true
      
      if (clearUI) {
        try {
          // 调用后端登出接口
          await userStore.callLogoutAPI()
        } catch (error) {
          console.error('登出请求失败:', error)
          logoutSuccess = false
        }
      }
      
      // 无论后端请求是否成功，都确保跳转到首页
      try {
        await router.push('/home')
      } catch (routerError) {
        console.error('页面跳转失败:', routerError)
        // 如果路由跳转失败，尝试强制刷新页面
        window.location.href = '/'
      }
      
      if (clearUI) {
        // 根据后端请求结果发送不同的通知
        if (logoutSuccess) {
          emitter.emit('notify', '已成功注销', 'success')
        } else {
          emitter.emit('notify', '已清理本地登录状态，但后端注销可能未完成', 'info')
        }
      }
    } catch (error) {
      console.error('注销过程发生错误:', error)
      // 如果发生任何错误，确保：
      // 1. 状态被清理
      clearUserState()
      // 2. 用户被重定向到首页
      window.location.href = '/'
      // 3. 显示适当的错误消息
      emitter.emit('notify', '注销过程遇到问题，但已确保安全退出', 'warning')
    }
  }

  return {
    handleLogout
  }
}
