// src/router/routes.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Home from '@/views/Home.vue'
import Fitness from '@/views/Fitness.vue'
import Asset from '@/views/Asset.vue'
import NotFound from '@/views/NotFound.vue'

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
      showSidebar: false,
    },
  },
  {
    path: '/fitness',
    name: 'fitness',
    component: Fitness,
    meta: {
      requiresAuth: true,
      title: '健身记录',
      showSidebar: true,
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

export default router
