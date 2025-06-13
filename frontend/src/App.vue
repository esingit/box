<template>
  <n-config-provider :theme-overrides="naiveBlackWhiteTheme">
    <div :class="['app-container']" id="app">
      <!-- 侧边栏 -->
      <Sidebar v-if="shouldShowSidebar" :isLoggedIn="isLoggedIn" />

      <div class="content-wrapper">
        <main class="main-content">
          <!-- 页面顶部用户菜单 -->
          <div class="main-header">
            <nav class="user-menu-container">
              <!-- 登录后菜单 -->
              <UserMenuAuthenticated
                  v-if="isLoggedIn"
                  :username="user?.username"
                  :showMenu="showMenu"
                  @toggle="toggleMenu"
                  @open-profile="openProfile"
                  @logout="logout"
                  ref="userMenuRef"
              />

              <!-- 未登录菜单 -->
              <UserMenuGuest v-else @login="showLogin" @register="showRegister" />
            </nav>
          </div>

          <!-- 主要内容区 -->
          <router-view />
        </main>
      </div>

      <!-- 全局弹窗组件 -->
      <GlobalModals
          ref="profileRef"
          :show-login="isShowingLoginModal"
          :show-register="isShowingRegisterModal"
          @login-close="hideLogin"
          @login-success="handleLoginSuccess"
          @register-close="hideRegister"
          @register-success="handleRegisterSuccess"
      />

      <!-- 全局通知组件 -->
      <Notification />
      <ConfirmDialog />
    </div>
  </n-config-provider>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from './stores/userStore'
import { useRouter } from 'vue-router'
import { initializeAuth, useAuth } from './composables/useAuth'
import { useLogout } from '@/composables/useLogout'
import Sidebar from './components/Sidebar.vue'
import GlobalModals from './components/GlobalModals.vue'
import Notification from './components/Notification.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import UserMenuAuthenticated from './components/user/UserMenuAuthenticated.vue'
import UserMenuGuest from './components/user/UserMenuGuest.vue'
import emitter from './utils/eventBus'

// 这里引入你提取的主题配置文件
import { naiveBlackWhiteTheme } from './naiveTheme'

// 状态管理
const userStore = useUserStore()
const router = useRouter()
const { isShowingLoginModal, isShowingRegisterModal, showLogin, hideLogin, showRegister, hideRegister } = useAuth()

// 计算属性
const shouldShowSidebar = computed(() =>
    !['/', '/home'].includes(router.currentRoute.value.path) || isLoggedIn.value
)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const user = computed(() => userStore.user)

// 响应式数据
const showMenu = ref(false)
const profileRef = ref(null)
const userMenuRef = ref(null)

// 用户菜单相关函数
function toggleMenu() {
  showMenu.value = !showMenu.value
}
function closeMenu() {
  showMenu.value = false
}

// 用 ref 监听点击外部关闭菜单
function handleClickOutside(event) {
  const path = event.composedPath()
  const menuEl = userMenuRef.value?.$el || null
  if (showMenu.value && menuEl && !path.includes(menuEl)) {
    closeMenu()
  }
}

function openProfile() {
  if (profileRef.value?.openModal) {
    profileRef.value.openModal()
    closeMenu()
  }
}

async function logout() {
  closeMenu()
  const { handleLogout } = useLogout()
  try {
    await handleLogout(true)
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}

// 认证相关函数
function handleLoginSuccess() {
  closeMenu()
  hideLogin()
  emitter.emit('login-success')
  emitter.emit('notify', '登录成功', 'success')
}

function handleRegisterSuccess() {
  hideRegister()
}

// 生命周期钩子
function onShowAuth(type, message) {
  if (type === 'login') showLogin(message)
  else if (type === 'register') showRegister()
}
function onShowLoginModal(message) {
  showLogin(message)
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside)
  emitter.on('show-auth', onShowAuth)
  emitter.on('show-login-modal', onShowLoginModal)
  initializeAuth().catch(error => {
    console.error('认证初始化失败:', error)
    userStore.logout(false)
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside)
  emitter.off('show-auth', onShowAuth)
  emitter.off('show-login-modal', onShowLoginModal)
})
</script>
