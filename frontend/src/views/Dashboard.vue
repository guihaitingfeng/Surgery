<template>
  <div class="dashboard">
    <!-- 管理员专用仪表盘 -->
    <template v-if="isAdmin">
      <!-- 欢迎卡片 -->
      <div class="welcome-card admin-welcome">
        <div class="welcome-content">
          <div class="welcome-text">
            <h1>系统管理员控制台</h1>
            <p class="welcome-subtitle">
              欢迎回来，{{ user?.realName }}！今天是 {{ currentDate }}
            </p>
          </div>
          <div class="welcome-icon">
            <el-icon><Setting /></el-icon>
          </div>
        </div>
      </div>

      <!-- 系统统计卡片 -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-primary">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ adminStats.totalUsers }}</h3>
              <p>总用户数</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-success">
            <div class="stat-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ adminStats.todayAppointments }}</h3>
              <p>今日预约</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-warning">
            <div class="stat-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ adminStats.totalAppointments }}</h3>
              <p>总预约数</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-info">
            <div class="stat-icon">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ adminStats.totalRooms }}</h3>
              <p>手术室数</p>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 管理员内容区域 -->
      <el-row :gutter="24" class="content-row">
        <!-- 用户角色分布 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="16">
          <div class="card">
            <div class="card-header">
              <h3>用户角色分布</h3>
              <el-button type="primary" size="small" @click="refreshStats">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
            <div class="card-content">
              <div class="user-stats-grid">
                <div class="user-stat-item">
                  <div class="user-stat-icon doctor">
                    <el-icon><Avatar /></el-icon>
                  </div>
                  <div class="user-stat-info">
                    <h4>{{ adminStats.doctorCount }}</h4>
                    <p>医生</p>
                  </div>
                </div>
                <div class="user-stat-item">
                  <div class="user-stat-icon patient">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="user-stat-info">
                    <h4>{{ adminStats.patientCount }}</h4>
                    <p>病人</p>
                  </div>
                </div>
                <div class="user-stat-item">
                  <div class="user-stat-icon nurse">
                    <el-icon><FirstAidKit /></el-icon>
                  </div>
                  <div class="user-stat-info">
                    <h4>{{ adminStats.nurseCount }}</h4>
                    <p>护士</p>
                  </div>
                </div>
                <div class="user-stat-item">
                  <div class="user-stat-icon anesthesiologist">
                    <el-icon><Operation /></el-icon>
                  </div>
                  <div class="user-stat-info">
                    <h4>{{ adminStats.anesthesiologistCount }}</h4>
                    <p>麻醉师</p>
                  </div>
                </div>
              </div>
              
              <!-- 删除系统状态栏 -->
            </div>
          </div>
        </el-col>

        <!-- 系统管理 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="8">
          <div class="card">
            <div class="card-header">
              <h3>系统管理</h3>
              <span class="card-subtitle">快捷管理操作</span>
            </div>
            <div class="card-content">
              <div class="admin-actions-grid">
                <div class="admin-action-item" @click="$router.push('/user-management')">
                  <div class="action-icon user-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>用户管理</h4>
                    <p>管理系统用户</p>
                  </div>
                </div>
                
                <div class="admin-action-item" @click="$router.push('/rooms')">
                  <div class="action-icon room-icon">
                    <el-icon><OfficeBuilding /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>手术室管理</h4>
                    <p>管理手术室资源</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 删除最近活动框 -->
        </el-col>
      </el-row>
    </template>

    <!-- 非管理员用户的原有仪表盘 -->
    <template v-else>
      <!-- 欢迎卡片 -->
      <div class="welcome-card">
        <div class="welcome-content">
          <div class="welcome-text">
            <h1>欢迎回来，{{ user?.realName || user?.username }}！</h1>
            <p class="welcome-subtitle">
              今天是 {{ currentDate }}，{{ isPatient ? `您有 ${todayAppointments.length} 个预约` : `您有 ${todayAppointments.length} 台手术安排` }}
            </p>
          </div>
          <div class="welcome-icon">
            <el-icon><UserFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-primary">
            <div class="stat-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ stats.todayAppointments }}</h3>
              <p>{{ isPatient ? '我的预约' : '今日手术' }}</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-success">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ stats.completedAppointments }}</h3>
              <p>已完成</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-warning">
            <div class="stat-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ stats.pendingAppointments }}</h3>
              <p>待确认</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-info">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ stats.totalPatients }}</h3>
              <p>{{ isPatient ? '历史预约' : '病人总数' }}</p>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 内容区域 -->
      <el-row :gutter="24" class="content-row">
        <!-- 今日手术安排 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="16">
          <div class="card">
            <div class="card-header">
              <h3>{{ isPatient ? '我的预约' : '今日手术安排' }}</h3>
              <el-button type="primary" size="small" @click="viewAllAppointments">
                查看全部
              </el-button>
            </div>
            <div class="card-content">
              <div v-if="loading" class="loading-container">
                <el-loading :spinning="true" />
              </div>
              <div v-else-if="todayAppointments.length === 0" class="empty-state">
                <el-empty :description="isPatient ? '暂无预约信息' : '今日没有手术安排'" :image-size="80" />
              </div>
              <div v-else class="appointment-list">
                <div 
                  v-for="appointment in todayAppointments.slice(0, 5)" 
                  :key="appointment.id"
                  class="appointment-item"
                >
                  <div class="appointment-time">
                    <el-tag :type="getTimeTagType(appointment)" size="small">
                      {{ appointment.plannedStartTime }} - {{ appointment.plannedEndTime }}
                    </el-tag>
                  </div>
                  <div class="appointment-info">
                    <h4>{{ appointment.surgeryName }}</h4>
                    <p>病人：{{ appointment.patient?.user?.realName }}</p>
                    <p>医生：{{ appointment.doctor?.realName }}</p>
                    <p>手术室：{{ appointment.room?.roomName }}</p>
                  </div>
                  <div class="appointment-status">
                    <el-tag :type="getStatusTagType(appointment.status)" size="small">
                      {{ getStatusText(appointment.status) }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 快捷操作 -->
        <el-col v-if="isDoctor || isPatient || isAdmin" :xs="24" :sm="24" :md="12" :lg="8">
          <div class="card">
            <div class="card-header">
              <h3>快捷操作</h3>
              <span class="card-subtitle">常用功能入口</span>
            </div>
            <div class="card-content">
              <div class="user-actions-grid">
                <div 
                  v-if="isPatient"
                  class="user-action-item"
                  @click="$router.push('/patients/create')"
                >
                  <div class="action-icon submit-icon">
                    <el-icon><Plus /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>提交病情信息</h4>
                    <p>提交您的基本病情</p>
                  </div>
                </div>
                
                <div 
                  v-if="isDoctor"
                  class="user-action-item"
                  @click="$router.push('/patients')"
                >
                  <div class="action-icon patient-icon">
                    <el-icon><View /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>病人列表</h4>
                    <p>查看所有病人信息</p>
                  </div>
                </div>
                
                <div 
                  v-if="isNurse || isAnesthesiologist"
                  class="user-action-item"
                  @click="viewPendingConfirmations"
                >
                  <div class="action-icon confirm-icon">
                    <el-icon><Bell /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>待确认预约</h4>
                    <p>查看需要确认的预约</p>
                  </div>
                </div>
                
                <div 
                  v-if="isDoctor"
                  class="user-action-item"
                  @click="$router.push('/appointments')"
                >
                  <div class="action-icon appointment-icon">
                    <el-icon><Calendar /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>预约管理</h4>
                    <p>管理手术预约</p>
                  </div>
                </div>
              </div>
            </div>
          </div>


        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import {
  UserFilled, Calendar, CircleCheck, Clock, User, Plus, View, Bell,
  Setting, OfficeBuilding, DataAnalysis, Warning, Avatar, FirstAidKit, 
  Operation, Refresh
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const store = useStore()
const router = useRouter()

const loading = ref(false)
const stats = ref({
  todayAppointments: 0,
  completedAppointments: 0,
  pendingAppointments: 0,
  totalPatients: 0
})

const user = computed(() => store.getters['auth/user'])
const isDoctor = computed(() => store.getters['auth/isDoctor'])
const isPatient = computed(() => store.getters['auth/isPatient'])
const isNurse = computed(() => store.getters['auth/isNurse'])
const isAnesthesiologist = computed(() => store.getters['auth/isAnesthesiologist'])
const isAdmin = computed(() => store.getters['auth/isAdmin'])

const todayAppointments = computed(() => store.getters['appointments/todayAppointments'])

const currentDate = computed(() => {
  return dayjs().format('YYYY年MM月DD日 dddd')
})

onMounted(async () => {
  await loadDashboardData()
})

const loadDashboardData = async () => {
  loading.value = true
  try {
    if (isAdmin.value) {
      // 管理员加载系统统计数据
      await loadAdminStats()
    } else if (isPatient.value) {
      // 病人查看自己的预约
      await store.dispatch('appointments/getPatientUpcomingAppointments')
      // 病人使用简单的统计数据
      stats.value = {
        todayAppointments: todayAppointments.value.length,
        completedAppointments: todayAppointments.value.filter(apt => apt.status === 'COMPLETED').length,
        pendingAppointments: todayAppointments.value.filter(apt => apt.status === 'SCHEDULED').length,
        totalPatients: 0 // 病人不需要看到病人总数
      }
    } else {
      // 医生/护士/麻醉师从数据库加载真实统计数据
      await loadMedicalStaffStats()
    }
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
    ElMessage.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

const getStatusTagType = (status) => {
  const statusMap = {
    'SCHEDULED': '',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'primary',
    'DOCTOR_FINAL_CONFIRMED': 'primary',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '等待医疗团队确认',
    'TEAM_CONFIRMED': '医疗团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生最终确认',
    'NOTIFIED': '已通知病人',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getTimeTagType = (appointment) => {
  const now = dayjs()
  const startTime = dayjs(`${appointment.plannedDate} ${appointment.plannedStartTime}`)
  
  if (startTime.isBefore(now)) {
    return 'danger'
  } else if (startTime.diff(now, 'hour') <= 2) {
    return 'warning'
  }
  return 'primary'
}

const viewPendingConfirmations = () => {
  router.push('/appointments?status=pending')
}

const viewAllAppointments = () => {
  if (isPatient.value) {
    // 病人跳转到病人专用的预约页面
    router.push('/patient-dashboard/appointments')
  } else if (isDoctor.value) {
    // 医生跳转到预约管理页面
    router.push('/appointments')
  } else if (isNurse.value || isAnesthesiologist.value) {
    // 护士和麻醉师跳转到预约列表页面，显示分配给自己的手术
    router.push('/appointments?view=my-assignments')
  } else {
    // 其他角色默认跳转到预约页面
    router.push('/appointments')
  }
}

const adminStats = ref({
  totalUsers: 0,
  todayAppointments: 0,
  totalAppointments: 0,
  totalRooms: 0,
  doctorCount: 0,
  patientCount: 0,
  nurseCount: 0,
  anesthesiologistCount: 0
})

const refreshStats = async () => {
  if (isAdmin.value) {
    await loadAdminStats()
  } else if (!isPatient.value) {
    await loadMedicalStaffStats()
  }
}

const loadAdminStats = async () => {
  try {
    // 调用真实的管理员统计API
    const response = await axios.get('/api/appointments/admin-stats')
    adminStats.value = response.data
  } catch (error) {
    console.error('获取管理员统计数据失败:', error)
    ElMessage.error('加载管理员统计数据失败')
    // 如果API调用失败，使用模拟数据作为备用
    adminStats.value = {
      totalUsers: 0,
      todayAppointments: 0,
      totalAppointments: 0,
      totalRooms: 0,
      doctorCount: 0,
      patientCount: 0,
      nurseCount: 0,
      anesthesiologistCount: 0
    }
  }
}

const loadMedicalStaffStats = async () => {
  try {
    // 先加载今日预约列表，确保有数据用于显示
    await store.dispatch('appointments/getTodayAppointments')
    
    // 调用医护人员统计API
    const response = await axios.get('/api/appointments/dashboard-stats')
    stats.value = response.data
  } catch (error) {
    console.error('获取医护人员统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
    // 如果API调用失败，确保已经加载了今日预约数据，然后使用备用统计方案
    if (todayAppointments.value.length === 0) {
      await store.dispatch('appointments/getTodayAppointments')
    }
    stats.value = {
      todayAppointments: todayAppointments.value.length,
      completedAppointments: todayAppointments.value.filter(apt => apt.status === 'COMPLETED').length,
      pendingAppointments: todayAppointments.value.filter(apt => apt.status === 'PENDING_CONFIRMATION' || apt.status === 'TEAM_CONFIRMED').length,
      totalPatients: 0
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard {
  .welcome-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 32px;
    margin-bottom: 24px;
    color: white;
    
    &.admin-welcome {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
    }
    
    .welcome-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .welcome-text {
        h1 {
          font-size: 28px;
          font-weight: 600;
          margin-bottom: 8px;
        }
        
        .welcome-subtitle {
          font-size: 16px;
          opacity: 0.9;
        }
      }
      
      .welcome-icon {
        .el-icon {
          font-size: 80px;
          opacity: 0.3;
        }
      }
    }
  }
  
  .stats-row {
    margin-bottom: 24px;
  }
  
  .stat-card {
    background: white;
    border-radius: 8px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    }
    
    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16px;
      
      .el-icon {
        font-size: 24px;
        color: white;
      }
    }
    
    .stat-content {
      h3 {
        font-size: 32px;
        font-weight: 700;
        margin-bottom: 4px;
        color: #2c3e50;
      }
      
      p {
        font-size: 14px;
        color: #7f8c8d;
        margin: 0;
      }
    }
    
    &.stat-primary .stat-icon {
      background: #409eff;
    }
    
    &.stat-success .stat-icon {
      background: #67c23a;
    }
    
    &.stat-warning .stat-icon {
      background: #e6a23c;
    }
    
    &.stat-info .stat-icon {
      background: #909399;
    }
  }
  
  .content-row {
    .appointment-list {
      .appointment-item {
        display: flex;
        align-items: center;
        padding: 16px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
        }
        
        .appointment-time {
          width: 140px;
          flex-shrink: 0;
        }
        
        .appointment-info {
          flex: 1;
          margin: 0 16px;
          
          h4 {
            font-size: 16px;
            font-weight: 600;
            margin-bottom: 4px;
            color: #2c3e50;
          }
          
          p {
            font-size: 13px;
            color: #7f8c8d;
            margin: 2px 0;
          }
        }
        
        .appointment-status {
          flex-shrink: 0;
        }
      }
    }
    
    .quick-actions {
      .quick-action-btn {
        width: 100%;
        margin-bottom: 12px;
        height: 40px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }
    }
    
    .user-actions-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      
      .user-action-item {
        display: flex;
        align-items: center;
        padding: 12px;
        border-radius: 8px;
        background: #f8f9fa;
        cursor: pointer;
        transition: all 0.3s ease;
        border: 2px solid transparent;
        min-width: 0;
        
        &:hover {
          background: #e9ecef;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          border-color: #e6f7ff;
        }
        
        .action-icon {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 10px;
          flex-shrink: 0;
          
          .el-icon {
            font-size: 18px;
            color: white;
          }
          
          &.submit-icon {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          }
          
          &.patient-icon {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
          }
          
          &.calendar-icon {
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
          }
          
          &.confirm-icon {
            background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
          }
          
          &.appointment-icon {
            background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
          }
          
          &.schedule-icon {
            background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
          }
        }
        
        .action-content {
          flex: 1;
          min-width: 0;
          
          h4 {
            font-size: 13px;
            font-weight: 600;
            color: #2c3e50;
            margin: 0 0 3px 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          
          p {
            font-size: 11px;
            color: #7f8c8d;
            margin: 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }
    }
    

  }
  
  .loading-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }
  
  .empty-state {
    text-align: center;
    padding: 40px 0;
  }
  
  // 管理员专用样式
  .card {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    
    .card-header {
      padding: 20px 24px 16px;
      border-bottom: 1px solid #f0f0f0;
      
      h3 {
        font-size: 18px;
        font-weight: 600;
        color: #2c3e50;
        margin: 0 0 4px 0;
      }
      
      .card-subtitle {
        font-size: 13px;
        color: #7f8c8d;
      }
      
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .el-button {
        padding: 6px 12px;
        font-size: 12px;
      }
    }
    
    .card-content {
      padding: 20px 24px 24px;
    }
  }
  
  .admin-actions-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    
    .admin-action-item {
      display: flex;
      align-items: center;
      padding: 12px;
      border-radius: 8px;
      background: #f8f9fa;
      cursor: pointer;
      transition: all 0.3s ease;
      border: 2px solid transparent;
      min-width: 0;
      
      &:hover {
        background: #e9ecef;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        border-color: #e6f7ff;
      }
      
      .action-icon {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 10px;
        flex-shrink: 0;
        
        .el-icon {
          font-size: 18px;
          color: white;
        }
        
        &.user-icon {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        
        &.room-icon {
          background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        }
      }
      
      .action-content {
        flex: 1;
        min-width: 0;
        
        h4 {
          font-size: 13px;
          font-weight: 600;
          color: #2c3e50;
          margin: 0 0 3px 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
        
        p {
          font-size: 11px;
          color: #7f8c8d;
          margin: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }
  
  .user-stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-bottom: 24px;
    
    .user-stat-item {
      display: flex;
      align-items: center;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 8px;
      transition: all 0.3s ease;
      
      &:hover {
        background: #e9ecef;
        transform: translateY(-2px);
      }
      
      .user-stat-icon {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        
        .el-icon {
          font-size: 20px;
          color: white;
        }
        
        &.doctor {
          background: #409eff;
        }
        
        &.patient {
          background: #67c23a;
        }
        
        &.nurse {
          background: #e6a23c;
        }
        
        &.anesthesiologist {
          background: #f56c6c;
        }
      }
      
      .user-stat-info {
        h4 {
          font-size: 24px;
          font-weight: 700;
          margin-bottom: 4px;
          color: #2c3e50;
        }
        
        p {
          font-size: 14px;
          color: #7f8c8d;
          margin: 0;
        }
      }
    }
  }
  
  // 删除系统状态和活动列表相关样式
}

@media (max-width: 768px) {
  .dashboard {
    .welcome-card {
      padding: 20px;
      
      .welcome-content {
        flex-direction: column;
        text-align: center;
        
        .welcome-icon {
          margin-top: 16px;
          
          .el-icon {
            font-size: 60px;
          }
        }
      }
    }
    
    .stat-card {
      padding: 16px;
      
      .stat-icon {
        width: 48px;
        height: 48px;
        margin-right: 12px;
        
        .el-icon {
          font-size: 20px;
        }
      }
      
      .stat-content {
        h3 {
          font-size: 24px;
        }
      }
    }
    
    .admin-actions-grid {
      grid-template-columns: 1fr;
      gap: 12px;
      
      .admin-action-item {
        padding: 12px;
        
        .action-icon {
          width: 40px;
          height: 40px;
          margin-right: 10px;
          
          .el-icon {
            font-size: 18px;
          }
        }
        
        .action-content {
          h4 {
            font-size: 13px;
          }
          
          p {
            font-size: 11px;
          }
        }
      }
    }
    
    .user-stats-grid {
      grid-template-columns: 1fr;
      gap: 12px;
    }
  }
}
</style> 