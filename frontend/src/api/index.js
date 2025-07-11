import axios from 'axios'
import router from '../router'
import { ElMessage } from 'element-plus'
import { authAPI } from './auth'

// 配置axios默认设置
axios.defaults.baseURL = 'http://localhost:8080'
axios.defaults.timeout = 10000

// 请求拦截器 - 自动添加JWT token
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('发送请求:', config.url, '携带token:', token.substring(0, 20) + '...')
    } else {
      console.warn('发送请求:', config.url, '未找到token')
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理401未授权错误
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response?.status === 401) {
      console.error('401未授权错误:', error.config.url, error.response.data)
      
      // 清除本地存储的token和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      
      // 显示错误消息
      ElMessage.error('登录已过期，请重新登录')
      
      // 跳转到登录页面
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

// 统一导出所有API
export const API = {
  auth: authAPI
}

// 默认导出axios实例
export default axios 