import { onMounted, onBeforeUnmount } from 'vue'
import emitter from '@/utils/eventBus'
import { initializeAuth } from '@/composable/useAuth'
import { useUserStore } from '@/store/userStore'
import { useUserMenu } from '@/composable/useUserMenu'
import { useAuthModal } from '@/composable/useAuthModal'

export function useAppLifecycle() {
    const userStore = useUserStore()
    const userMenu = useUserMenu()
    const authModal = useAuthModal()

    // 响应外部事件显示登录/注册弹窗
    function onShowAuth(type: 'login' | 'register') {
        if (type === 'login') authModal.showLogin()
        if (type === 'register') authModal.showRegister()
    }

    onMounted(() => {
        document.addEventListener('mousedown', userMenu.handleClickOutside)
        emitter.on('show-auth', onShowAuth)

        initializeAuth().catch(error => {
            console.error('认证初始化失败:', error)
            userStore.logout(false)
        })
    })

    onBeforeUnmount(() => {
        document.removeEventListener('mousedown', userMenu.handleClickOutside)
        emitter.off('show-auth', onShowAuth)
    })
}
