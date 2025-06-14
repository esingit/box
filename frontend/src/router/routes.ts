import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import Asset from '@/views/asset/Asset.vue'
import NotFound from '@/views/exception/NotFound.vue'

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
    },
  },
  {
    path: '/fitness',
    name: 'fitness',
    component: Fitness,
    meta: {
      requiresAuth: true,
      title: '健身记录',
    },
  },
  {
    path: '/asset',
    name: 'asset',
    component: Asset,
    meta: {
      requiresAuth: true,
      title: '资产管理',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: {
      requiresAuth: false,
      title: '页面未找到',
    },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 导航守卫：简单鉴权示例
router.beforeEach((to, from, next) => {
  const isLoggedIn = !!localStorage.getItem('token') // 简单校验token
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/home') // 或重定向登录页
  } else {
    next()
  }
})

export default router
