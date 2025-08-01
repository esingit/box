// 请求管理相关类型定义

// 请求管理器类
export class RequestManager {
    private controllers = new Map<string, AbortController>()
    private isDev = import.meta.env.DEV

    abort(key: string, reason = '新请求开始'): void {
        const controller = this.controllers.get(key)
        if (controller) {
            if (this.isDev) {
                console.log(`🟡 [请求管理] ${reason}，取消 ${key} 请求`)
            }
            controller.abort(reason)
            this.controllers.delete(key)
        }
    }

    create(key: string): AbortController {
        this.abort(key)
        const controller = new AbortController()
        this.controllers.set(key, controller)
        return controller
    }

    cleanup(): void {
        this.controllers.forEach((controller, key) => {
            controller.abort('Store cleanup')
        })
        this.controllers.clear()
        if (this.isDev) {
            console.log('🟡 [请求管理] 已清理所有请求')
        }
    }
}