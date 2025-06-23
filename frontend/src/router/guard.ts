// src/router/guard.ts
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus'
import type { Router, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'

export function setupRouterGuard(router: Router) {
    const userStore = useUserStore()
    const { showLogin } = useAuth()

    router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
        if (!userStore.isInitialized) {
            await userStore.hydrate()
        }

        if (to.meta.title) {
            document.title = String(to.meta.title) + ' - Box'
        }

        if (to.meta.requiresAuth && !userStore.isLoggedIn) {
            showLogin()

            // 监听登录成功事件，跳转目标页面，避免内存泄漏
            const loginSuccessHandler = () => {
                emitter.off('login-success', loginSuccessHandler)
                router.push(to.fullPath)
            }
            emitter.on('login-success', loginSuccessHandler)

            // 跳登录页，而非仅弹登录框
            next({ path: '/home', query: { redirect: to.fullPath } })
            return
        }

        next()
    })
}
