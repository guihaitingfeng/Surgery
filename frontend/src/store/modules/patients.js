import axios from 'axios'

const state = {
  patients: [],
  currentPatient: null,
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
  
  SET_PATIENTS(state, data) {
    state.patients = data.content
    state.pagination = {
      page: data.number,
      size: data.size,
      total: data.totalElements
    }
  },
  
  SET_CURRENT_PATIENT(state, patient) {
    state.currentPatient = patient
  },
  
  ADD_PATIENT(state, patient) {
    state.patients.unshift(patient)
  },
  
  UPDATE_PATIENT(state, updatedPatient) {
    const index = state.patients.findIndex(p => p.id === updatedPatient.id)
    if (index !== -1) {
      state.patients.splice(index, 1, updatedPatient)
    }
  },
  
  REMOVE_PATIENT(state, patientId) {
    state.patients = state.patients.filter(p => p.id !== patientId)
  }
}

const actions = {
  async fetchPatients({ commit }, params = {}) {
    commit('SET_LOADING', true)
    try {
      const response = await axios.get('/api/patients', { params })
      commit('SET_PATIENTS', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取病人列表失败' 
      }
    } finally {
      commit('SET_LOADING', false)
    }
  },
  
  async fetchPatient({ commit }, id) {
    try {
      const response = await axios.get(`/api/patients/${id}`)
      commit('SET_CURRENT_PATIENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取病人信息失败' 
      }
    }
  },
  
  async createPatient({ commit }, patientData) {
    try {
      const response = await axios.post('/api/patients', patientData)
      commit('ADD_PATIENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '创建病人信息失败' 
      }
    }
  },
  
  async updatePatient({ commit }, { id, patientData }) {
    try {
      const response = await axios.put(`/api/patients/${id}`, patientData)
      commit('UPDATE_PATIENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '更新病人信息失败' 
      }
    }
  },
  
  async deletePatient({ commit }, id) {
    try {
      await axios.delete(`/api/patients/${id}`)
      commit('REMOVE_PATIENT', id)
      return { success: true }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '删除病人信息失败' 
      }
    }
  },
  
  async getMyPatientInfo({ commit }) {
    try {
      const response = await axios.get('/api/patients/my-info')
      commit('SET_CURRENT_PATIENT', response.data)
      return { success: true, data: response.data }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.error || '获取个人病人信息失败' 
      }
    }
  }
}

const getters = {
  patients: state => state.patients,
  currentPatient: state => state.currentPatient,
  loading: state => state.loading,
  pagination: state => state.pagination
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
} 