import axios from 'axios'

const state = {
  appointments: [],
  currentAppointment: null,
  loading: false,
  pagination: {
    page: 0,
    size: 10,
    total: 0
  }
}

const mutations = {
  SET_LOADING(state, loading) {
    state.loading = loading
  },
  
  SET_APPOINTMENTS(state, data) {
    if (Array.isArray(data)) {
      state.appointments = data
    } else {
      state.appointments = data.content
      state.pagination = {
        page: data.number,
        size: data.size,
        total: data.totalElements
      }
    }
  },
  
  SET_CURRENT_APPOINTMENT(state, appointment) {
    state.currentAppointment = appointment
  },
  
  ADD_APPOINTMENT(state, appointment) {
    state.appointments.unshift(appointment)
  },
  
  UPDATE_APPOINTMENT(state, updatedAppointment) {
    const index = state.appointments.findIndex(a => a.id === updatedAppointment.id)
    if (index !== -1) {
      state.appointments.splice(index, 1, updatedAppointment)
    }
  },
  
  REMOVE_APPOINTMENT(state, appointmentId) {
    state.appointments = state.appointments.filter(a => a.id !== appointmentId)
  }
}

const actions = {
  async fetchAppointments({ commit }, params = {}) {
    commit('SET_LOADING', true)
    try {
      const response = await axios.get('/api/appointments', { params })
      commit('SET_APPOINTMENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取预约列表失败' 
      }
    } finally {
      commit('SET_LOADING', false)
    }
  },
  
  async fetchAppointment({ commit }, id) {
    try {
      const response = await axios.get(`/api/appointments/${id}`)
      commit('SET_CURRENT_APPOINTMENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取预约信息失败' 
      }
    }
  },
  
  async createAppointment({ commit }, appointmentData) {
    try {
      const response = await axios.post('/api/appointments', appointmentData)
      commit('ADD_APPOINTMENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '创建预约失败' 
      }
    }
  },
  
  async updateAppointment({ commit }, { id, appointmentData }) {
    try {
      const response = await axios.put(`/api/appointments/${id}`, appointmentData)
      commit('UPDATE_APPOINTMENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '更新预约失败' 
      }
    }
  },
  
  async confirmAppointment({ commit }, id) {
    try {
      const response = await axios.post(`/api/appointments/${id}/confirm`)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '确认预约失败' 
      }
    }
  },
  
  async cancelAppointment({ commit }, { id, reason }) {
    try {
      const response = await axios.post(`/api/appointments/${id}/cancel`, { reason })
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '取消预约失败' 
      }
    }
  },
  
  async getTodayAppointments({ commit }) {
    try {
      const response = await axios.get('/api/appointments/today')
      commit('SET_APPOINTMENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取今日预约失败' 
      }
    }
  },
  
  async getAppointmentsForDate({ commit }, date) {
    try {
      const response = await axios.get(`/api/appointments/date/${date}`)
      commit('SET_APPOINTMENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取指定日期预约失败' 
      }
    }
  },

  async getPatientAppointments({ commit }) {
    try {
      const response = await axios.get('/api/appointments/patient/my-appointments')
      commit('SET_APPOINTMENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取病人预约失败' 
      }
    }
  },

  async getPatientUpcomingAppointments({ commit }) {
    try {
      const response = await axios.get('/api/appointments/patient/upcoming')
      commit('SET_APPOINTMENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取即将到来的预约失败' 
      }
    }
  }
}

const getters = {
  appointments: state => state.appointments,
  currentAppointment: state => state.currentAppointment,
  loading: state => state.loading,
  pagination: state => state.pagination,
  todayAppointments: state => state.appointments.filter(apt => {
    const today = new Date()
    const year = today.getFullYear()
    const month = String(today.getMonth() + 1).padStart(2, '0')
    const day = String(today.getDate()).padStart(2, '0')
    const todayStr = `${year}-${month}-${day}`
    return apt.plannedDate === todayStr
  })
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
} 