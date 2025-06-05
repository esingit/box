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
    // 无论是直接访问还是页面跳转，都允许访问但显示登录框
    next()
    // 通知需要登录
    emitter.emit('show-auth', 'login', '请先登录')
  } else {
    next()
  }
})

export default router