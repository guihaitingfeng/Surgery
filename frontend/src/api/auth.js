import axios from 'axios'

// 用户认证相关API
export const authAPI = {
  // 登录
  login(credentials) {
    return axios.post('/api/auth/login', credentials)
  },

  // 注册
  register(userData) {
    return axios.post('/api/auth/register', userData)
  },

  // 获取当前用户信息
  getCurrentUser() {
    return axios.get('/api/auth/me')
  },

  // 退出登录
  logout() {
    return axios.post('/api/auth/logout')
  },

  // 修改密码
  changePassword(passwordData) {
    return axios.put('/api/auth/change-password', passwordData)
  }
}

// 导出单个函数以便于使用
export const login = authAPI.login
export const register = authAPI.register
export const getCurrentUser = authAPI.getCurrentUser
export const logout = authAPI.logout
export const changePassword = authAPI.changePassword

export default authAPI 