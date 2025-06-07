import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import { useUserStore } from '@/stores/userStore'
import { useAuth } from '@/composables/useAuth'

const routes = [
  { path: '/', redirect: '/home' },  // 将根路径重定向到主页
  { path: '/home', component: Home },
  { path: '/fitness', component: Fitness },
  { path: '/asset', component: () => import('@/views/asset/Asset.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const { showLogin } = useAuth()
  // 不需要登录的页面路径
  const publicPaths = ['/login', '/register', '/home']
  
  // 优先处理登出路由
  if (to.path === '/logout') {
    await userStore.logout(true)
    next('/home')
    return
  }
  
  // 如果需要登录但未登录
  if (!userStore.isLoggedIn && !publicPaths.includes(to.path)) {
    // 保存原始目标路由
    const targetPath = to.fullPath
    
    if (!from.name) {
      // 如果是直接访问，先跳转到首页
      next('/home')
      // 等待组件挂载
      await nextTick()
      showLogin('请先登录')
      // 登录成功后重定向到原始目标
      emitter.once('login-success', () => {
        router.push(targetPath)
      })
    } else {
      // 如果是从其他页面跳转
      next(false) // 阻止导航
      showLogin('请先登录')
      // 登录成功后重定向到目标页面
      emitter.once('login-success', () => {
        router.push(targetPath)
      })
    }
  } else {
    next()
  }
})

export default router