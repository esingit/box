import { createRouter, createWebHistory } from 'vue-router'
import Home from './views/Home.vue'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Fitness from './views/Fitness.vue'
import { useUserStore } from './stores/userStore'

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/fitness', component: Fitness },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  // 需要登录的页面路径
  const protectedPaths = ['/fitness']
  if (protectedPaths.includes(to.path) && !userStore.isLoggedIn) {
    // 未登录访问受保护页面，跳转到主页
    next('/')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
  } else {
    next()
  }
})

export default router