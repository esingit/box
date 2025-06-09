// 用于存储正在进行的请求的Map
const pendingRequests = new Map()

// 生成请求的唯一key
export const generateRequestKey = (config) => {
  const { url, method, params, data } = config
  const queryStr = params ? `?${new URLSearchParams(params).toString()}` : ''
  return `${method}:${url}${queryStr}${data ? ':' + JSON.stringify(data) : ''}`
}

// 取消重复的请求
export function cancelPendingRequests(message = '取消重复请求') {
  for (const [, controller] of pendingRequests) {
    controller.abort(message)
  }
  pendingRequests.clear()
}

// 管理请求
export const requestManager = {
  add(requestKey, controller) {
    pendingRequests.set(requestKey, controller)
  },
  
  delete(requestKey) {
    pendingRequests.delete(requestKey)
  },
  
  has(requestKey) {
    return pendingRequests.has(requestKey)
  },
  
  get(requestKey) {
    return pendingRequests.get(requestKey)
  }
}
