import { createStore } from 'vuex'
import auth from './modules/auth'
import patients from './modules/patients'
import appointments from './modules/appointments'

export default createStore({
  modules: {
    auth,
    patients,
    appointments
  }
}) 