import type { Router } from 'vue-router'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus'

let isLoggingOut = false // 防止重复触发登出

export async function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const { showLogin } = useAuth()

    // 设置页面标题
    if (to.meta.title) {
      document.title = String(to.meta.title)
    }

    // 路由不存在：重定向回上一个路径或首页
    if (!to.matched.length) {
      const fallbackPath = from.path || '/home'
      next(fallbackPath)
      return
    }

    // 处理退出登录路由
    if (to.name === 'logout') {
      if (!isLoggingOut) {
        isLoggingOut = true
        await userStore.logout(true)
        isLoggingOut = false
      }
      next('/home')
      return
    }

    // 需要登录的页面验证
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
          next(false) // 中断跳转等待登录
        }
        return
      }
    }

    // 正常放行
    next()
  })
}
