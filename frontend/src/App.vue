<!-- src/App.vue -->
<template>
  <div class="app-wrapper">
    <div v-if="isUserLoading" class="loading">加载中...</div>
    <div v-else>
      <template v-if="isLoggedIn">
        <div class="layout flex h-screen relative">
          <Sidebar :isLoggedIn="isLoggedIn" />
          <div
              class="flex-1 flex flex-col transition-all duration-200 ease-in-out"
              :class="{ 'ml-0': sidebarCollapsed, 'ml-32': !sidebarCollapsed }"
          >
            <div class="menu-container">
              <UserMenuAuthenticated
                  :user="user"
                  @logout="handleLogout"
                  @open-profile="handleOpenProfile"
              />
            </div>
            <div class="app-content">
              <RouterView />
            </div>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="flex flex-col min-h-screen">
          <div class="menu-container">
            <UserMenuGuest
                @show-login="showLogin()"
                @show-register="showRegister()"
            />
          </div>
          <div class="app-content">
            <RouterView />
          </div>
        </div>
      </template>
    </div>

    <!-- 统一管理登录注册弹窗 -->
    <AuthModals
        v-model:showLogin="isShowingLoginModal"
        v-model:showRegister="isShowingRegisterModal"
        @loginSuccess="handleLoginSuccess"
        @registerSuccess="handleLoginSuccess"
    />

    <Profile ref="profileSettingsRef" />
    <BaseNotice />
    <BaseDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, provide, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus'

import Sidebar from '@/components/layout/Sidebar.vue'
import AuthModals from '@/components/auth/AuthModals.vue'
import BaseNotice from '@/components/base/BaseNotice.vue'
import BaseDialog from '@/components/base/BaseDialog.vue'
import UserMenuGuest from '@/components/layout/UserMenuGuest.vue'
import UserMenuAuthenticated from '@/components/layout/UserMenuAuthenticated.vue'
import Profile from '@/components/layout/Profile.vue'

const userStore = useUserStore()
const auth = useAuth()

// 从 userStore 获取用户信息
const { user } = storeToRefs(userStore)

// 从合并后的 useAuth 获取所有需要的状态和方法
const {
  isLoggedIn,
  isShowingLoginModal,
  isShowingRegisterModal,
  showLogin,
  showRegister,
  hideLogin,
  hideRegister,
  onLoginSuccess,
  clearToken
} = auth

// 本地状态
const isUserLoading = ref(true)
const profileSettingsRef = ref<InstanceType<typeof Profile> | null>(null)

// 侧边栏状态
const sidebarCollapsed = ref(false)
provide('setSidebarCollapsed', (collapsed: boolean) => {
  sidebarCollapsed.value = collapsed
})

// 监听事件总线弹出登录弹窗
function onShowLogin() {
  showLogin()
}

onMounted(() => {
  emitter.on('show-login', onShowLogin)
})

onUnmounted(() => {
  emitter.off('show-login', onShowLogin)
})

// 通知方法
function notify(type: 'success' | 'error' | 'info' | 'warning', msg: string) {
  emitter.emit('notify', { message: msg, type })
}

// 错误处理
function handleError(error: any, defaultMessage: string) {
  const message = error?.message || defaultMessage
  notify('error', message)
  console.error(error)
}

// 登录成功处理
async function handleLoginSuccess() {
  try {
    // 使用 useAuth 中的完整处理逻辑
    await onLoginSuccess()
    notify('success', '登录成功')
  } catch (error) {
    handleError(error, '登录后操作失败')
  }
}

// 退出登录处理
async function handleLogout() {
  try {
    await userStore.logout()
    clearToken()
    notify('success', '已退出登录')
  } catch (error) {
    handleError(error, '退出登录失败')
  }
}

// 初始化用户数据
async function initializeUser(retryCount = 3) {
  try {
    await userStore.hydrate()
  } catch (error) {
    if (retryCount > 0) {
      notify('error', '用户数据加载失败，正在重试...')
      setTimeout(() => initializeUser(retryCount - 1), 2000)
    } else {
      handleError(error, '无法加载用户数据，请检查网络连接或稍后再试')
    }
  } finally {
    isUserLoading.value = false
  }
}

// 打开个人设置
function handleOpenProfile() {
  profileSettingsRef.value?.openModal()
}

// 启动时初始化用户数据
initializeUser()
</script>