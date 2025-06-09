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
        // 记录要访问的页面
        const targetPath = to.fullPath
        
        // 显示登录框，并在登录成功后跳转
        showLogin('请先登录')
        emitter.once('login-success', () => {
          router.push(targetPath)
        })
        
        // 如果是直接访问，先跳转到主页
        if (!from.name) {
          next('/home')
        } else {
          next(false)
        }
        return
      }
      
      // 验证token有效性
      try {
        const axios = (await import('@/utils/axios')).default
        await axios.get('/api/user/verify-token')
      } catch (error) {
        await userStore.logout(false)
        showLogin('登录已过期，请重新登录')
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
