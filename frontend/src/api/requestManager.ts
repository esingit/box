interface AbortControllerMap {
    [key: string]: AbortController
}

class RequestManager {
    private requests: AbortControllerMap = {}

    has(key: string): boolean {
        return key in this.requests
    }

    get(key: string): AbortController | undefined {
        return this.requests[key]
    }

    add(key: string, controller: AbortController): void {
        this.requests[key] = controller
    }

    delete(key: string): void {
        delete this.requests[key]
    }
}

export const requestManager = new RequestManager()

// 生成请求唯一key，避免重复请求
export function generateRequestKey(config: { method?: string; url?: string; params?: any; data?: any }): string {
    const { method = 'get', url = '', params, data } = config
    const paramsString = params ? JSON.stringify(params) : ''
    const dataString = data ? JSON.stringify(data) : ''
    return [method.toUpperCase(), url, paramsString, dataString].join('&')
}
