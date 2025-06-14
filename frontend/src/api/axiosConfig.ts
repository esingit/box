export const axiosConfig = {
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
}

export const ALLOWED_DUPLICATE_ENDPOINTS = [
    '/some/endpoint/allow-duplicate',
]
