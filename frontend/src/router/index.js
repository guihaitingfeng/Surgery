import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { 
      title: '登录',
      requiresAuth: false 
    }
  },
  {
    path: '/admin-login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue'),
    meta: { 
      title: '管理员登录',
      requiresAuth: false 
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { 
      title: '注册',
      requiresAuth: false 
    }
  },
  {
    path: '/patient-dashboard',
    name: 'PatientDashboard',
    component: () => import('../views/PatientDashboard.vue'),
    meta: { 
      title: '病人面板',
      requiresAuth: true,
      roles: ['PATIENT']
    }
  },
  {
    path: '/patient-dashboard/info',
    component: () => import('../views/patient/PatientInfo.vue'),
    meta: { requiresAuth: true, role: 'PATIENT' }
  },
  {
    path: '/patient-dashboard/appointments',
    component: () => import('../views/patient/PatientAppointments.vue'),
    meta: { requiresAuth: true, role: 'PATIENT' }
  },
  {
    path: '/patient-dashboard/notifications',
    component: () => import('../views/patient/PatientNotifications.vue'),
    meta: { requiresAuth: true, role: 'PATIENT' }
  },
  {
    path: '/patient-dashboard/help',
    component: () => import('../views/patient/PatientHelp.vue'),
    meta: { requiresAuth: true, role: 'PATIENT' }
  },
  {
    path: '/patient-profile',
    name: 'PatientProfile',
    component: () => import('../views/patient/PatientProfile.vue'),
    meta: { 
      title: '个人资料',
      requiresAuth: true,
      roles: ['PATIENT']
    }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { 
      title: '仪表盘',
      requiresAuth: true,
      roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'ADMIN']
    },
    children: [
      {
        path: '',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: '/patients',
        name: 'Patients',
        component: () => import('../views/patients/PatientList.vue'),
        meta: { 
          title: '病人管理',
          roles: ['DOCTOR', 'ADMIN']
        }
      },
      {
        path: '/patients/create',
        name: 'CreatePatient',
        component: () => import('../views/patients/PatientForm.vue'),
        meta: { 
          title: '添加病人',
          roles: ['ADMIN']
        }
      },
      {
        path: '/patients/:id/edit',
        name: 'EditPatient',
        component: () => import('../views/patients/PatientForm.vue'),
        meta: { 
          title: '编辑病人信息',
          roles: ['DOCTOR', 'ADMIN']
        }
      },
      {
        path: '/patients/:id',
        name: 'PatientDetail',
        component: () => import('../views/patients/PatientDetail.vue'),
        meta: { 
          title: '病人详情',
          roles: ['DOCTOR', 'ADMIN']
        }
      },
      {
        path: '/appointments',
        name: 'Appointments',
        component: () => import('../views/appointments/AppointmentList.vue'),
        meta: { 
          title: '手术预约',
          roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'PATIENT', 'ADMIN']
        }
      },
      {
        path: '/appointments/create',
        name: 'CreateAppointment',
        component: () => import('../views/appointments/AppointmentForm.vue'),
        meta: { 
          title: '创建预约',
          roles: ['DOCTOR', 'ADMIN']
        }
      },
      {
        path: '/appointments/calendar',
        name: 'AppointmentCalendar',
        component: () => import('../views/appointments/AppointmentCalendar.vue'),
        meta: { 
          title: '手术日历',
          roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'PATIENT', 'ADMIN']
        }
      },
      {
        path: '/appointments/:id',
        name: 'AppointmentDetail',
        component: () => import('../views/appointments/AppointmentDetail.vue'),
        meta: { 
          title: '预约详情',
          roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'PATIENT', 'ADMIN']
        }
      },
      {
        path: '/appointments/:id/edit',
        name: 'EditAppointment',
        component: () => import('../views/appointments/AppointmentForm.vue'),
        meta: { 
          title: '编辑预约',
          roles: ['DOCTOR', 'ADMIN']
        }
      },
      {
        path: '/schedule',
        name: 'Schedule',
        component: () => import('../views/schedule/ScheduleList.vue'),
        meta: { title: '排班管理' }
      },
      {
        path: '/rooms',
        name: 'Rooms',
        component: () => import('../views/rooms/RoomList.vue'),
        meta: { title: '手术室管理' }
      },
      {
        path: '/user-management',
        name: 'UserManagement',
        component: () => import('../views/UserManagement.vue'),
        meta: { 
          title: '用户管理',
          roles: ['ADMIN']
        }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人信息' }
      },
      {
        path: '/notifications',
        name: 'Notifications',
        component: () => import('../views/NotificationList.vue'),
        meta: { 
          title: '消息通知',
          roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'PATIENT', 'ADMIN']
        }
      },
      {
        path: '/notifications/:id',
        name: 'NotificationDetail',
        component: () => import('../views/NotificationDetail.vue'),
        meta: { 
          title: '通知详情',
          roles: ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'PATIENT', 'ADMIN']
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 手术预约与排班管理系统` : '手术预约与排班管理系统'
  
  // 检查是否需要认证
  if (to.meta.requiresAuth !== false) {
    const token = localStorage.getItem('token')
    if (!token) {
      next('/login')
      return
    }
    
    // 检查用户信息是否存在
    if (!store.state.auth.user) {
      // 尝试从localStorage获取用户信息
      const userInfo = localStorage.getItem('userInfo')
      if (userInfo) {
        store.commit('auth/SET_USER', JSON.parse(userInfo))
        store.commit('auth/SET_TOKEN', token)
      } else {
        // 获取当前用户信息
        try {
          await store.dispatch('auth/getCurrentUser')
        } catch {
          next('/login')
          return
        }
      }
    }
    
    // 检查角色权限
    const user = store.state.auth.user
    if (user && to.meta.roles) {
      if (!to.meta.roles.includes(user.role)) {
        // 根据用户角色重定向到相应的页面
        if (user.role === 'PATIENT') {
          next('/patient-dashboard')
        } else {
          next('/dashboard')
        }
        return
      }
    }
    
    // 根路径重定向
    if (to.path === '/' && user) {
      if (user.role === 'PATIENT') {
        next('/patient-dashboard')
      } else {
        next('/dashboard')
      }
      return
    }
  }
  
  next()
})

export default router 