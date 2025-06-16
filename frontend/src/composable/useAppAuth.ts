import { useUserStore } from '@/store/userStore'
import { useAuth } from '@/composable/useAuth'
import { useAuthModal } from '@/composable/useAuthModal'
import { useUserMenu } from '@/composable/useUserMenu'
import emitter from '@/utils/eventBus'

export function useAppAuth() {
    const userStore = useUserStore()
    const auth = useAuth()
    const authModal = useAuthModal()
    const userMenu = useUserMenu()

    // 登录成功处理逻辑
    function handleLoginSuccess() {
        userMenu.closeMenu()
        authModal.hideLogin()
        emitter.emit('login-success')
        emitter.emit('notify', '登录成功', 'success')
    }

    // 注册成功处理逻辑
    function handleRegisterSuccess() {
        authModal.hideRegister()
    }

    // 登出逻辑
    async function logout() {
        userMenu.closeMenu()
        try {
            await userStore.logout(true)
        } catch (error) {
            console.error('退出登录失败:', error)
        }
    }

    return {
        handleLoginSuccess,
        handleRegisterSuccess,
        logout
    }
}
