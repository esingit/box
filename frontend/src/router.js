import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import { useUserStore } from '@/stores/userStore'

const routes = [
  { path: '/', component: Home },
  { path: '/fitness', component: Fitness },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  // 不需要登录的页面路径
  const publicPaths = ['/login', '/register','/']
  if (!userStore.isLoggedIn && !publicPaths.includes(to.path)) {
    // 未登录访问受保护页面，跳转到登录页
    next('/login')
  } else {
    next()
  }
})

export default router