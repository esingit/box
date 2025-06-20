// src/composable/useAuthModal.ts
import { ref } from 'vue'

export function useAuthModal() {
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

    return {
        isShowingLoginModal,
        isShowingRegisterModal,
        showLogin,
        hideLogin,
        showRegister,
        hideRegister,
        handleSwitchToLogin
    }
}
