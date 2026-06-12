import axios from 'axios';
import { useUserStore } from '@/store/user';

const NOT_LOGIN_CODE = 40100;

interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  message?: string;
}

function redirectToLogin() {
  useUserStore.getState().setUser(null);
  if (window.location.pathname !== '/login') {
    window.location.replace('/login');
  }
}

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true,
});

request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResponse;
    if (result?.code !== 0) {
      if (result?.code === NOT_LOGIN_CODE) {
        redirectToLogin();
      }
      return Promise.reject(new Error(result?.message || '请求失败'));
    }
    return response.data;
  },
  (error) => {
    const result = error.response?.data as ApiResponse | undefined;
    if (error.response?.status === 401 || result?.code === NOT_LOGIN_CODE) {
      redirectToLogin();
    }
    return Promise.reject(new Error(result?.message || '请求失败'));
  }
);

export default request;
