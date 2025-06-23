import { ref } from 'vue'

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

function handleSwitchToLogin() {
    isShowingRegisterModal.value = false
    isShowingLoginModal.value = true
}

export const useAuthModal = () => ({
    isShowingLoginModal,
    isShowingRegisterModal,
    showLogin,
    hideLogin,
    showRegister,
    hideRegister,
    handleSwitchToLogin
})
