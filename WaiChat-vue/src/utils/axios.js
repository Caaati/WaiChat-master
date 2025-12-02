import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://localhost:8080', // 后端接口基础路径
  timeout: 5000
});

// 请求拦截器（添加 Token）
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default instance;