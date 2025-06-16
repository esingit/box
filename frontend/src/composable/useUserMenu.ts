import { ref } from 'vue'

export function useUserMenu() {
    const showMenu = ref(false)

    function toggleMenu() {
        showMenu.value = !showMenu.value
    }

    function closeMenu() {
        showMenu.value = false
    }

    function openProfile() {
        // 这里你可以触发打开个人资料弹窗的逻辑，比如通过事件总线或状态管理
    }

    // 点击外部关闭菜单
    function handleClickOutside(event) {
        // 这里最好接收 menu 的 ref，判断点击是否在菜单内
        // 简单示例，直接关闭菜单
        closeMenu()
    }

    return {
        showMenu,
        toggleMenu,
        closeMenu,
        openProfile,
        handleClickOutside
    }
}
