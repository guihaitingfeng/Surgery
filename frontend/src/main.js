import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import axios from 'axios'
import './assets/styles/main.scss'

// 配置axios默认设置
axios.defaults.baseURL = 'http://localhost:8080'
axios.defaults.timeout = 10000

// 请求拦截器 - 添加token
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理token过期
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      // Token过期，清除本地存储并跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      store.commit('auth/CLEAR_AUTH')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局属性
app.config.globalProperties.$http = axios

app.use(store)
app.use(router)
app.use(ElementPlus)
app.mount('#app') 