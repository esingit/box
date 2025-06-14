export const axiosConfig = {
    baseURL: 'http://127.0.0.1:8091/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
}

export const ALLOWED_DUPLICATE_ENDPOINTS = [
    '/some/endpoint/allow-duplicate',
    // 可根据项目需求添加允许重复请求的接口
]
