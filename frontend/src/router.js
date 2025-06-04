import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import { useUserStore } from '@/stores/userStore'
import emitter from '@/utils/eventBus'

const routes = [
  { path: '/', redirect: '/home' },  // 将根路径重定向到主页
  { path: '/home', component: Home },
  { path: '/fitness', component: Fitness },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  // 不需要登录的页面路径
  const publicPaths = ['/login', '/register', '/home']
  if (!userStore.isLoggedIn && !publicPaths.includes(to.path)) {
    // 未登录访问受保护页面，显示登录弹窗并保持在当前页面
    next(false)
    emitter.emit('show-auth', 'login')
  } else {
    next()
  }
})

export default router