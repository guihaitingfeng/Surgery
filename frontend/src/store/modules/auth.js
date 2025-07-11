import axios from 'axios'

const state = {
  token: localStorage.getItem('token') || null,
  user: (() => {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
  })(),
  isAuthenticated: !!localStorage.getItem('token')
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
    state.isAuthenticated = !!token
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  },
  
  SET_USER(state, user) {
    state.user = user
    if (user) {
      localStorage.setItem('userInfo', JSON.stringify(user))
    } else {
      localStorage.removeItem('userInfo')
    }
  },
  
  CLEAR_AUTH(state) {
    state.token = null
    state.user = null
    state.isAuthenticated = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }
}

const actions = {
  async login({ commit }, credentials) {
    try {
      const response = await axios.post('/api/auth/login', credentials)
      const { token, user } = response.data
      
      commit('SET_TOKEN', token)
      commit('SET_USER', user)
      
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '登录失败' 
      }
    }
  },
  
  async register({ commit }, userData) {
    try {
      const response = await axios.post('/api/auth/register', userData)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '注册失败' 
      }
    }
  },
  
  async getCurrentUser({ commit }) {
    try {
      const response = await axios.get('/api/auth/me')
      commit('SET_USER', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      commit('CLEAR_AUTH')
      return { success: false }
    }
  },
  
  logout({ commit }) {
    commit('CLEAR_AUTH')
  }
}

const getters = {
  isAuthenticated: state => state.isAuthenticated,
  user: state => state.user,
  userRole: state => state.user?.role,
  isDoctor: state => state.user?.role === 'DOCTOR',
  isPatient: state => state.user?.role === 'PATIENT',
  isNurse: state => state.user?.role === 'NURSE',
  isAnesthesiologist: state => state.user?.role === 'ANESTHESIOLOGIST',
  isAdmin: state => state.user?.role === 'ADMIN'
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
} 