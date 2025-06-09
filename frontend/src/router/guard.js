import { useUserStore } from '@/stores/userStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus'

export async function setupRouterGuard(router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const { showLogin } = useAuth()
    
    // 如果路由不存在，重定向到当前页面或首页
    if (!to.matched.length) {
      const fallbackPath = from.path || '/home'
      next(fallbackPath)
      return
    }
    
    // 处理登出路由
    if (to.name === 'logout') {
      await userStore.logout(true)
      next('/home')
      return
    }
    
    // 验证需要登录的页面
    if (to.meta.requiresAuth) {
      if (!userStore.isLoggedIn) {
        const targetPath = to.fullPath
        showLogin('请先登录')
        emitter.once('login-success', () => {
          router.push(targetPath)
        })
        
        if (!from.name) {
          next('/home')
        } else {
          next(false)
        }
        return
      }
    }
    
    // 允许导航
    next()
  })
}
