// src/composables/useAuthModal.ts
import { ref } from 'vue'

// 全局单例状态，保证所有地方共享登录注册弹窗状态
const isShowingLoginModal = ref(false)
const isShowingRegisterModal = ref(false)

function showLogin() {
    isShowingLoginModal.value = true
    isShowingRegisterModal.value = false
}

function hideLogin() {
    isShowingLoginModal.value = false
}

function showRegister() {
    isShowingRegisterModal.value = true
    isShowingLoginModal.value = false
}

function hideRegister() {
    isShowingRegisterModal.value = false
}

function hideAll() {
    isShowingLoginModal.value = false
    isShowingRegisterModal.value = false
}

function handleSwitchToLogin() {
    showLogin()
}

function handleSwitchToRegister() {
    showRegister()
}

export const useAuthModal = () => ({
    isShowingLoginModal,
    isShowingRegisterModal,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    hideAll,
    handleSwitchToLogin,
    handleSwitchToRegister
})
