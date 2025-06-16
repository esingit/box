import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import Asset from '@/views/asset/Asset.vue'
import NotFound from '@/components/common/NotFound.vue'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/home',
    name: 'home',
    component: Home,
    meta: {
      requiresAuth: false,
      title: '首页',
      showSidebar: false,  // 首页不显示侧边栏
    },
  },
  {
    path: '/fitness',
    name: 'fitness',
    component: Fitness,
    meta: {
      requiresAuth: true,
      title: '健身记录',
      showSidebar: true,   // 需要登录且显示侧边栏
    },
  },
  {
    path: '/asset',
    name: 'asset',
    component: Asset,
    meta: {
      requiresAuth: true,
      title: '资产管理',
      showSidebar: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: {
      requiresAuth: false,
      title: '页面未找到',
      showSidebar: false,
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 导航守卫：鉴权+标题动态设置
router.beforeEach((to, from, next) => {
  const isLoggedIn = !!localStorage.getItem('token')

  // 设置文档标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - Box'
  }

  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/home') // 没登录强制回首页或者登录页
  } else {
    next()
  }
})

export default router
