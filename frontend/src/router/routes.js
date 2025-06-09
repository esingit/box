import Home from '@/views/Home.vue'
import Fitness from '@/views/fitness/Fitness.vue'
import Asset from '@/views/asset/Asset.vue'

export const routes = [
  { 
    path: '/', 
    redirect: '/home' 
  },
  { 
    path: '/home', 
    name: 'home',
    component: Home,
    meta: {
      requiresAuth: false,
      title: '首页'
    }
  },
  { 
    path: '/fitness', 
    name: 'fitness',
    component: Fitness,
    meta: {
      requiresAuth: true,
      title: '健身记录'
    }
  },
  { 
    path: '/asset', 
    name: 'asset',
    component: Asset,
    meta: {
      requiresAuth: true,
      title: '资产管理'
    }
  },
  {
    path: '/logout',
    name: 'logout',
    meta: {
      requiresAuth: true,
      title: '退出登录'
    }
  }
]
