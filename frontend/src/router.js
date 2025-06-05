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
    // 如果是直接访问需要登录的页面（不是从其他页面跳转来的）
    if (!from.name) {
      // 重定向到首页并显示登录框
      next('/home')
    } else {
      // 如果是从其他页面跳转，允许访问
      next()
    }
    // 通知需要登录，设置延迟以确保组件已经挂载
    setTimeout(() => {
      emitter.emit('show-auth', 'login', '请先登录')
    }, 100)
  } else {
    next()
  }
})

export default router