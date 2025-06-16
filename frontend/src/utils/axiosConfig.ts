import axios, { AxiosError } from 'axios';

interface AxiosRetryConfig {
  baseURL: string;
  timeout: number;
  withCredentials: boolean;
  retry: number;
  retryDelay: number;
  shouldRetry: (error: AxiosError) => boolean;
}

export const axiosConfig: AxiosRetryConfig = {
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 15000,
  withCredentials: true,
  retry: 3,
  retryDelay: 1500,
  shouldRetry: (error: AxiosError): boolean => {
  const url = error.config?.url || '';

  // 登录、注册接口或请求被取消，不重试
  if (url.includes('/login') ||
      url.includes('/register') ||
      axios.isCancel(error)) {
    return false;
  }

  // 以下情况可以重试：超时、无响应、网络错误、服务错误
  return error.code === 'ECONNABORTED' ||
      !error.response ||
      error.code === 'ERR_NETWORK' ||
      [500, 502, 503, 504].includes(error.response?.status || 0);
}
};

export const ALLOWED_DUPLICATE_ENDPOINTS: string[] = [
  '/login',
  '/register',
  '/refresh-token',
  '/verify-token'
];