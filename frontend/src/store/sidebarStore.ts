import { defineStore } from 'pinia'

export const useSidebarStore = defineStore('sidebar', {
    state: () => ({
        isExpanded: true // 默认展开状态
    }),
    actions: {
        toggleSidebar() {
            this.isExpanded = !this.isExpanded
        },
        setSidebarState(state: boolean) {
            this.isExpanded = state
        }
    }
})