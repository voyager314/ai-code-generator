import axios from 'axios';

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  withCredentials: true,
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error.response?.data?.message || '请求失败';
    return Promise.reject(new Error(message));
  }
);

export default request;
