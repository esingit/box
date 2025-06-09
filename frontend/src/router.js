import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './router/routes'
import { setupRouterGuard } from './router/guard'

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

setupRouterGuard(router)

export default router