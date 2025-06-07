import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import { useUserStore } from '@/stores/userStore'
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
})

// 记录是否已经显示登录框
let isShowingLoginModal = false;

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  // 不需要登录的页面路径
  const publicPaths = ['/login', '/register', '/home']
  
  if (!userStore.isLoggedIn && !publicPaths.includes(to.path)) {
    // 如果是直接访问需要登录的页面，重定向到首页
    if (!from.name) {
      next('/home')
    } else {
      next()
    }
    
    // 避免重复显示登录框
    if (!isShowingLoginModal) {
      isShowingLoginModal = true;
      // 设置延迟以确保组件已经挂载
      setTimeout(() => {
        emitter.emit('show-auth', 'login', '请先登录')
      }, 100)
    }
  } else {
    // 如果已经登录或访问公共页面，重置标记
    isShowingLoginModal = false;
    next()
  }
})

export default router