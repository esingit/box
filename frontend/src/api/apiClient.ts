// src/api/apiClient.ts
import axios from 'axios';
import { tokenService } from './tokenService';
import { useUserStore } from '@/store/userStore';
import router from '@/router/routes';

export const apiClient = axios.create({
    baseURL: '/api',
    timeout: 10000,
});

// 请求拦截器自动加 token
apiClient.interceptors.request.use(config => {
    const token = tokenService.getToken();
    if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// 响应拦截器处理 401 刷新 token
apiClient.interceptors.response.use(
    res => res,
    async error => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const userStore = useUserStore();
                const refreshed = await userStore.refreshToken();

                if (refreshed) {
                    const newToken = tokenService.getToken();
                    if (newToken) {
                        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                    }
                    return apiClient(originalRequest);
                }
            } catch (e) {
                await useUserStore().clearAuth(false);
                router.push('/home'); // 跳回首页或登录页
                return Promise.reject(e);
            }
        }
        return Promise.reject(error);
    }
);

