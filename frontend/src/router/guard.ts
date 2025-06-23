import {useUserStore} from '@/store/userStore'
import {useAuth} from '@/composable/useAuth'
import emitter from '@/utils/eventBus'
import type {Router, NavigationGuardNext, RouteLocationNormalized} from 'vue-router'

export function setupRouterGuard(router: Router) {
    const userStore = useUserStore()
    const {showLogin} = useAuth()
    router.beforeEach(
        async (
            to: RouteLocationNormalized,
            from: RouteLocationNormalized,
            next: NavigationGuardNext
        ) => {
            if (!userStore.isInitialized) {
                await userStore.hydrate()
            }
            if (!userStore.isInitialized) {
                await userStore.hydrate()
            }

            if (to.meta.title) {
                document.title = String(to.meta.title)
            }

            if (to.meta.requiresAuth && !userStore.isLoggedIn) {
                showLogin()
                emitter.once('login-success', () => {
                    router.push(to.fullPath)
                })
                next(false)
                return
            }

            next()
        })
}
