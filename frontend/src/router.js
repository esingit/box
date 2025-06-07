import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import { useUserStore } from '@/stores/userStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus'

const routes = [
  { path: '/', redirect: '/home' },  // 将根路径重定向到主页
  { path: '/home', component: Home },
  { path: '/fitness', component: Fitness },
  { path: '/asset', component: () => import('@/views/asset/Asset.vue') },
]

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

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const { showLogin } = useAuth()
  // 不需要登录的页面路径
  const publicPaths = ['/login', '/register', '/home']
  
  // 如果路由不存在，重定向到当前页面或首页
  if (!to.matched.length) {
    const fallbackPath = from.path || '/home'
    next(fallbackPath)
    return
  }
  
  // 优先处理登出路由
  if (to.path === '/logout') {
    await userStore.logout(true)
    next('/home')
    return
  }
  
  // 如果是非公开页面且用户已登录，验证token有效性
  if (!publicPaths.includes(to.path) && userStore.isLoggedIn) {
    try {
      // 通过API验证token
      const axios = (await import('@/utils/axios')).default
      await axios.get('/api/user/verify-token')
    } catch (error) {
      // token无效，清理登录状态并显示登录弹窗
      await userStore.logout(false)
      showLogin('登录已过期，请重新登录')
      if (!from.name) {
        next('/home')
      } else {
        next(false)
      }
      return
    }
  }

  // 如果需要登录但未登录
  if (!userStore.isLoggedIn && !publicPaths.includes(to.path)) {
    // 记录要访问的页面
    const targetPath = to.fullPath
    
    // 显示登录框，并在登录成功后跳转
    showLogin('请先登录')
    emitter.once('login-success', () => {
      router.push(targetPath)
    })
    
    // 如果是直接访问（刷新或直接输入URL），先跳转到主页
    if (!from.name) {
      next('/home')
    } else {
      // 如果是从其他页面跳转，阻止导航
      next(false)
    }
    return
  }
  
  // 已登录或访问公开页面，允许导航
  next()
})

export default router