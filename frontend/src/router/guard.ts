import type { Router } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composable/useAuth'
import emitter from '@/utils/eventBus'

let logoutPromise: Promise<void> | null = null

export function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    // 设置页面标题
    if (to.meta.title) {
      document.title = String(to.meta.title) + ' - Box'
    }

    // 404处理
    if (!to.matched.length) {
      next('/home')
      return
    }

    // 退出登录路由（如果你有的话）
    if (to.name === 'logout') {
      const userStore = useUserStore()
      if (!logoutPromise) {
        logoutPromise = userStore.logout(true).finally(() => {
          logoutPromise = null
        })
      }
      await logoutPromise
      next('/home')
      return
    }

    // 需要登录权限
    if (to.meta.requiresAuth) {
      const userStore = useUserStore()
      const { showLogin } = useAuth()

      if (!userStore.isLoggedIn) {
        // 触发登录弹窗
        showLogin()

        // 等待登录成功事件，登录成功后跳转回原页面
        emitter.once('login-success', () => {
          router.push(to.fullPath)
        })

        next(false) // 中断当前导航，等待登录
        return
      }
    }

    next()
  })
}
