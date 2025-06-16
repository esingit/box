// 用于存储正在进行的请求的Map
const pendingRequests = new Map<string, AbortController>()

// 生成请求的唯一key
export const generateRequestKey = (config: {
  url?: string
  method?: string
  params?: Record<string, any>
  data?: any
}): string => {
  const { url = '', method = 'GET', params, data } = config
  const queryStr = params ? `?${new URLSearchParams(params).toString()}` : ''
  const dataStr = data ? `:${JSON.stringify(data)}` : ''
  return `${method.toUpperCase()}:${url}${queryStr}${dataStr}`
}

// 取消重复的请求
export function cancelPendingRequests(message = '取消重复请求'): void {
  for (const [, controller] of pendingRequests) {
    controller.abort(message)
  }
  pendingRequests.clear()
}

// 管理请求
export const requestManager = {
  add(requestKey: string, controller: AbortController): void {
    pendingRequests.set(requestKey, controller)
  },

  delete(requestKey: string): void {
    pendingRequests.delete(requestKey)
  },

  has(requestKey: string): boolean {
    return pendingRequests.has(requestKey)
  },

  get(requestKey: string): AbortController | undefined {
    return pendingRequests.get(requestKey)
  }
}