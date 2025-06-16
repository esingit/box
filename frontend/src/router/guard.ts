import type { Router } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composable/useAuth'
import emitter from '@/utils/eventBus'

let logoutPromise: Promise<void> | null = null

export function setupRouterGuard(router: Router) {
  const userStore = useUserStore()
  const { showLogin } = useAuth()

  router.beforeEach(async (to, from, next) => {
    // 设置页面标题
    if (to.meta.title) {
      document.title = String(to.meta.title)
    }

    // 404重定向
    if (!to.matched.length) {
      next('/home')
      return
    }

    // 退出登录路由
    if (to.name === 'logout') {
      if (!logoutPromise) {
        logoutPromise = userStore.logout(true).finally(() => {
          logoutPromise = null
        })
      }
      await logoutPromise
      next('/home')
      return
    }

    // 需要登录权限页面
    if (to.meta.requiresAuth) {
      if (!userStore.isLoggedIn) {
        showLogin('请先登录')
        // 等待登录事件触发后跳转，暂时中断导航
        emitter.once('login-success', () => {
          router.push(to.fullPath)
        })
        // 这里直接取消当前导航，弹窗会显示，等待用户操作
        next(false)
        return
      }
    }

    next()
  })
}
