<!-- App.vue -->
<template>
  <n-config-provider :theme-overrides="theme">
    <n-notification-provider>
      <n-message-provider>
        <template v-if="isUserLoading">
          <div class="loading">加载中...</div>
        </template>

        <template v-else-if="isLoggedIn">
          <n-layout has-sider>
            <Sidebar/>
            <n-layout>
              <div class="top-right-user-menu">
                <UserMenuAuthenticated
                    :user="user"
                    @logout="handleLogout"
                    @open-profile="handleOpenProfile"
                />
              </div>
              <n-layout-content>
                <RouterView/>
              </n-layout-content>
            </n-layout>
          </n-layout>
        </template>

        <template v-else>
          <div class="top-right-user-menu">
            <UserMenuGuest
                @show-login="showLogin"
                @show-register="showRegister"
            />
          </div>
          <RouterView/>
        </template>

        <AuthModals
            v-model:showLogin="isShowingLoginModal"
            v-model:showRegister="isShowingRegisterModal"
            @login-success="handleLoginSuccess"
            @register-success="handleLoginSuccess"
        />

        <Profile ref="profileSettingsRef"/>

        <Notification/>
        <ConfirmDialog/>
      </n-message-provider>
    </n-notification-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {storeToRefs} from 'pinia'
import {createDynamicNaiveTheme} from '@/plugins/naive'
import {useUserStore} from '@/store/userStore'
import {useAuth} from '@/composable/useAuth'
import {useAuthModal} from '@/composable/useAuthModal'

import Sidebar from '@/components/layout/Sidebar.vue'
import AuthModals from '@/components/common/AuthModals.vue'
import Notification from '@/components/common/Notification.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import UserMenuGuest from '@/components/layout/UserMenuGuest.vue'
import UserMenuAuthenticated from '@/components/layout/UserMenuAuthenticated.vue'
import Profile from '@/components/layout/Profile.vue'

const theme = createDynamicNaiveTheme()
const userStore = useUserStore()
const auth = useAuth()
const authModal = useAuthModal()

const {user} = storeToRefs(userStore)
const {isLoggedIn, pendingAuthAction, clearToken} = auth
const {
  isShowingLoginModal,
  isShowingRegisterModal,
  showLogin,
  showRegister,
  hideLogin,
  hideRegister
} = authModal

const isUserLoading = ref(true)
const profileSettingsRef = ref<InstanceType<typeof Profile> | null>(null)

function notify(type: 'success' | 'error', msg: string) {
  window.$message?.[type](msg)
}

// 统一错误处理
function handleError(error: any, defaultMessage: string) {
  const message = error.message || defaultMessage
  notify('error', message)
  console.error(error) // 记录错误堆栈以便调试
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
    notify('success', '已退出登录')
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
      setTimeout(() => initializeUser(retryCount - 1), 2000) // 每两秒尝试一次，最多三次
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
