<template>
  <div class="app-wrapper">
    <!-- 加载中提示 -->
    <div v-if="isUserLoading" class="loading">加载中...</div>

    <!-- 登录状态判断 -->
    <div v-else>
      <!-- 已登录 -->
      <template v-if="isLoggedIn">
        <div class="layout flex h-screen">
          <Sidebar :isLoggedIn="isLoggedIn" />
          <div class="flex-1 flex flex-col">
            <!-- 顶部用户信息栏 -->
            <div class="menu-container">
              <UserMenuAuthenticated
                  :user="user"
                  @logout="handleLogout"
                  @open-profile="handleOpenProfile"
              />
            </div>
            <!-- 主内容区域 -->
            <div class="app-content">
              <RouterView />
            </div>
          </div>
        </div>
      </template>

      <!-- 未登录 -->
      <template v-else>
        <div class="flex flex-col min-h-screen">
          <!-- 顶部访客菜单 -->
          <div class="menu-container">
            <UserMenuGuest
                @show-login="showLogin()"
                @show-register="showRegister()"
            />
          </div>
          <!-- 路由内容区域 -->
          <div class="app-content">
            <RouterView />
          </div>
        </div>
      </template>
    </div>

    <!-- 登录 / 注册弹窗 -->
    <AuthModals
        v-model:showLogin="isShowingLoginModal"
        v-model:showRegister="isShowingRegisterModal"
        @login-success="handleLoginSuccess"
        @register-success="handleLoginSuccess"
    />

    <!-- 个人资料弹窗 -->
    <Profile ref="profileSettingsRef" />

    <!-- 全局通知和确认弹窗 -->
    <Notification />
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composable/useAuth'
import { useAuthModal } from '@/composable/useAuthModal'
import emitter from '@/utils/eventBus'

import Sidebar from '@/components/layout/Sidebar.vue'
import AuthModals from '@/components/auth/AuthModals.vue'
import Notification from '@/components/base/Notification.vue'
import ConfirmDialog from '@/components/base/ConfirmDialog.vue'
import UserMenuGuest from '@/components/layout/UserMenuGuest.vue'
import UserMenuAuthenticated from '@/components/layout/UserMenuAuthenticated.vue'
import Profile from '@/components/layout/Profile.vue'

const userStore = useUserStore()
const auth = useAuth()
const authModal = useAuthModal()

const { user } = storeToRefs(userStore)
const { isLoggedIn, pendingAuthAction, clearToken } = auth
const {
  isShowingLoginModal,
  isShowingRegisterModal,
  showLogin,
  showRegister,
  hideLogin,
  hideRegister,
} = authModal

const isUserLoading = ref(true)
const profileSettingsRef = ref<InstanceType<typeof Profile> | null>(null)

function notify(type: 'success' | 'error' | 'info' | 'warning', msg: string) {
  emitter.emit('notify', { message: msg, type })
}

function handleError(error: any, defaultMessage: string) {
  const message = error?.message || defaultMessage
  notify('error', message)
  console.error(error)
}

async function handleLoginSuccess() {
  try {
    await userStore.fetchUser()
    notify('success', '登录成功')
    if (typeof pendingAuthAction.value === 'function') {
      await pendingAuthAction.value()
    }
  } catch (error) {
    handleError(error, '登录后操作失败')
  } finally {
    pendingAuthAction.value = null
    hideLogin()
    hideRegister()
  }
}

async function handleLogout() {
  try {
    await userStore.logout()
    clearToken()
  } catch (error) {
    handleError(error, '退出登录失败')
  }
}

async function initializeUser(retryCount = 3) {
  try {
    await userStore.hydrate()
  } catch (error) {
    if (retryCount > 0) {
      notify('error', '用户数据加载失败，正在重试...')
      setTimeout(() => initializeUser(retryCount - 1), 2000)
    } else {
      handleError(error, '无法加载用户数据，请检查您的网络连接或稍后再试')
    }
  } finally {
    isUserLoading.value = false
  }
}

function handleOpenProfile() {
  profileSettingsRef.value?.openModal()
}

initializeUser()
</script>

