<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="!!patientInfo"
    :is-blacklisted="cancellationStatus.isBlacklisted"
    @show-submit-dialog="showSubmitDialog = true"
  >
    <div class="patient-dashboard">
      <!-- 欢迎卡片 -->
      <div class="welcome-card" :class="{ 'blacklisted': cancellationStatus.isBlacklisted }">
        <div class="welcome-content">
          <div class="welcome-text">
            <h1>欢迎回来，{{ user?.realName || user?.username }}！</h1>
            <p class="welcome-subtitle" v-if="!cancellationStatus.isBlacklisted">
              今天是 {{ currentDate }}，您有 {{ upcomingAppointments }} 个待进行的预约
            </p>
            <p class="welcome-subtitle error" v-else>
              您的账户已被限制使用，解除时间：{{ formatDate(cancellationStatus.blacklistEndDate) }}
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
              <h3>{{ appointments.length }}</h3>
              <p>我的预约</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-success">
            <div class="stat-icon">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ completedCount }}</h3>
              <p>已完成</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card stat-warning">
            <div class="stat-icon">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ unreadCount }}</h3>
              <p>未读通知</p>
            </div>
          </div>
        </el-col>
        
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card" :class="cancellationStatus.thisMonthCancellations >= 1 ? 'stat-danger' : 'stat-info'">
            <div class="stat-icon">
              <el-icon><Close /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ cancellationStatus.remainingCancellations || 0 }}</h3>
              <p>剩余取消次数</p>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 内容区域 -->
      <el-row :gutter="24" class="content-row">
        <!-- 左侧：病情信息和手术预约 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="16">
          <!-- 病情信息卡片 -->
          <div class="card">
            <div class="card-header">
              <h3>我的病情信息</h3>
              <el-button 
                v-if="!patientInfo" 
                type="primary" 
                size="small"
                @click="showSubmitDialog = true"
                :disabled="cancellationStatus.isBlacklisted"
              >
                提交病情信息
              </el-button>
            </div>
            <div class="card-content">
              <div v-if="patientInfo" class="patient-info-modern">
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">病历号</span>
                    <span class="info-value">{{ patientInfo.medicalRecordNumber }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">分配医生</span>
                    <span class="info-value">{{ patientInfo.assignedDoctor?.realName || '未分配' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">严重程度</span>
                    <el-tag :type="getSeverityTagType(patientInfo.severityLevel)" size="small">
                      {{ getSeverityText(patientInfo.severityLevel) }}
                    </el-tag>
                  </div>
                  <div class="info-item">
                    <span class="info-label">当前状态</span>
                    <el-tag :type="getStatusTagType(patientInfo.status)" size="small">
                      {{ getStatusText(patientInfo.status) }}
                    </el-tag>
                  </div>
                </div>
                <div class="info-description">
                  <span class="info-label">病情描述</span>
                  <p class="description-text">{{ patientInfo.diseaseDescription }}</p>
                </div>
              </div>
              
              <div v-else class="empty-state">
                <el-empty description="您还未提交病情信息" :image-size="80">
                  <el-button 
                    type="primary" 
                    @click="showSubmitDialog = true"
                    :disabled="cancellationStatus.isBlacklisted"
                  >
                    立即提交
                  </el-button>
                </el-empty>
              </div>
            </div>
          </div>

          <!-- 手术预约列表 -->
          <div class="card mt-3">
            <div class="card-header">
              <h3>我的手术预约</h3>
              <el-tag v-if="upcomingAppointments > 0" type="primary" size="small">
                {{ upcomingAppointments }} 个待进行
              </el-tag>
            </div>
            <div class="card-content">
              <div v-if="loading" class="loading-container">
                <el-loading :spinning="true" />
              </div>
              <div v-else-if="appointments.length === 0" class="empty-state">
                <el-empty description="暂无手术预约" :image-size="80" />
              </div>
              <div v-else class="appointment-list">
                <div 
                  v-for="appointment in appointments" 
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
                    <p>日期：{{ appointment.plannedDate }}</p>
                    <p>医生：{{ appointment.doctor?.realName }}</p>
                    <p>手术室：{{ appointment.room?.roomName }}</p>
                  </div>
                  <div class="appointment-actions">
                    <el-tag :type="getAppointmentStatusTagType(appointment.status)" size="small">
                      {{ getStatusText(appointment.status) }}
                    </el-tag>
                    <el-button 
                      size="small"
                      @click="viewAppointmentDetail(appointment)"
                    >
                      查看详情
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右侧：快捷操作和通知 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="8">
          <!-- 快捷操作 -->
          <div class="card">
            <div class="card-header">
              <h3>快捷操作</h3>
              <span class="card-subtitle">常用功能入口</span>
            </div>
            <div class="card-content">
              <div class="patient-actions-grid">
                <div 
                  v-if="!patientInfo"
                  class="patient-action-item" 
                  @click="router.push('/patient-dashboard/info')"
                  :class="{ 'disabled': cancellationStatus.isBlacklisted }"
                >
                  <div class="action-icon submit-icon">
                    <el-icon><DocumentAdd /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>提交病情信息</h4>
                    <p>提交您的基本病情</p>
                  </div>
                </div>
                
                <div class="patient-action-item" @click="viewAllNotifications">
                  <div class="action-icon notification-icon">
                    <el-icon><Bell /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>通知消息</h4>
                    <p>查看系统通知</p>
                  </div>
                </div>
                
                <div class="patient-action-item" @click="viewProfile">
                  <div class="action-icon profile-icon">
                    <el-icon><User /></el-icon>
                  </div>
                  <div class="action-content">
                    <h4>个人信息</h4>
                    <p>管理个人资料</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 通知消息 -->
          <div class="card mt-3">
            <div class="card-header">
              <h3>最新通知</h3>
              <el-button 
                v-if="unreadCount > 0"
                type="text" 
                size="small"
                @click="markAllAsRead"
              >
                全部已读
              </el-button>
            </div>
            <div class="card-content">
              <div v-if="notifications.length > 0" class="notification-list">
                <div 
                  v-for="notification in notifications.slice(0, 3)" 
                  :key="notification.id" 
                  class="notification-item"
                  :class="{ 'unread': !notification.isRead }"
                  @click="viewNotificationDetail(notification)"
                >
                  <el-icon class="notification-icon" :class="getNotificationIconClass(notification.type)">
                    <component :is="getNotificationIcon(notification.type)" />
                  </el-icon>
                  <div class="notification-content">
                    <p>{{ notification.title }}</p>
                    <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="empty-state">
                <el-empty description="暂无通知消息" :image-size="60" />
              </div>
            </div>
          </div>


        </el-col>
      </el-row>

      <!-- 提交病情信息对话框 -->
      <el-dialog 
        v-model="showSubmitDialog" 
        title="提交病情信息" 
        width="600px"
        :before-close="handleDialogClose"
      >
        <el-form :model="submitForm" :rules="submitRules" ref="submitFormRef" label-width="120px">
          <el-form-item label="病历号" prop="medicalRecordNumber">
            <el-input v-model="submitForm.medicalRecordNumber" placeholder="请输入病历号" />
          </el-form-item>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="submitForm.idCard" placeholder="请输入身份证号" />
          </el-form-item>
          <el-form-item label="紧急联系人" prop="emergencyContact">
            <el-input v-model="submitForm.emergencyContact" placeholder="请输入紧急联系人姓名" />
          </el-form-item>
          <el-form-item label="紧急联系电话" prop="emergencyPhone">
            <el-input v-model="submitForm.emergencyPhone" placeholder="请输入紧急联系电话" />
          </el-form-item>
          <el-form-item label="病情描述" prop="diseaseDescription">
            <el-input 
              v-model="submitForm.diseaseDescription" 
              type="textarea" 
              :rows="4"
              placeholder="请详细描述您的病情"
            />
          </el-form-item>
          <el-form-item label="严重程度" prop="severityLevel">
            <el-select v-model="submitForm.severityLevel" placeholder="请选择病情严重程度">
              <el-option label="紧急" value="EMERGENCY" />
              <el-option label="急迫" value="URGENT" />
              <el-option label="普通" value="NORMAL" />
              <el-option label="轻微" value="LOW" />
            </el-select>
          </el-form-item>
          <el-form-item label="过敏史">
            <el-input 
              v-model="submitForm.allergies" 
              type="textarea" 
              :rows="2"
              placeholder="请填写过敏史（如无请填无）"
            />
          </el-form-item>
          <el-form-item label="当前用药">
            <el-input 
              v-model="submitForm.currentMedications" 
              type="textarea" 
              :rows="2"
              placeholder="请填写当前用药情况（如无请填无）"
            />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="showSubmitDialog = false">取消</el-button>
          <el-button type="primary" @click="submitPatientInfo" :loading="submitting">
            提交
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  UserFilled, Calendar, CircleCheck, Clock, User, Plus, View, Bell,
  InfoFilled, SuccessFilled, WarningFilled, Close, Location, DocumentAdd
} from '@element-plus/icons-vue'
import PatientLayout from '../layouts/PatientLayout.vue'
import axios from 'axios'
import dayjs from 'dayjs'

const store = useStore()
const router = useRouter()

const loading = ref(false)
const patientInfo = ref(null)
const appointments = ref([])
const notifications = ref([])
const unreadCount = ref(0)
const cancellationStatus = ref({})
const showSubmitDialog = ref(false)
const submitting = ref(false)
const submitFormRef = ref()

const user = computed(() => store.getters['auth/user'])
const currentDate = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))
const upcomingAppointments = computed(() => 
  appointments.value.filter(apt => 
    ['SCHEDULED', 'CONFIRMED'].includes(apt.status)
  ).length
)
const completedCount = computed(() => 
  appointments.value.filter(apt => apt.status === 'COMPLETED').length
)

const submitForm = ref({
  medicalRecordNumber: '',
  idCard: '',
  emergencyContact: '',
  emergencyPhone: '',
  diseaseDescription: '',
  severityLevel: '',
  allergies: '',
  currentMedications: ''
})

const submitRules = {
  medicalRecordNumber: [
    { required: true, message: '请输入病历号', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/, 
      message: '请输入有效的身份证号', trigger: 'blur' }
  ],
  emergencyContact: [
    { required: true, message: '请输入紧急联系人', trigger: 'blur' }
  ],
  emergencyPhone: [
    { required: true, message: '请输入紧急联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  diseaseDescription: [
    { required: true, message: '请描述病情', trigger: 'blur' }
  ],
  severityLevel: [
    { required: true, message: '请选择严重程度', trigger: 'change' }
  ]
}

onMounted(async () => {
  // 确保token已经正确设置到axios中
  await nextTick()
  
  // 检查是否有token
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')
  
  console.log('PatientDashboard onMounted - Debug Info:')
  console.log('Token:', token ? 'exists' : 'missing')
  console.log('UserInfo:', userInfo ? JSON.parse(userInfo) : 'missing')
  console.log('Axios default headers:', axios.defaults.headers.common)
  
  if (!token) {
    ElMessage.error('未找到登录信息，请重新登录')
    router.push('/login')
    return
  }
  
  // 确保axios headers中有token
  if (!axios.defaults.headers.common['Authorization']) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    console.log('Set axios default Authorization header')
  }
  
  // 延迟一点再加载数据，确保所有设置都完成
  setTimeout(() => {
    loadPatientInfo()
    loadAppointments()
    loadNotifications()
    loadCancellationStatus()
    loadUnreadCount()
  }, 200)
})

const loadPatientInfo = async () => {
  try {
    const response = await axios.get('/api/patient-portal/my-info')
    if (response.data && !response.data.message) {
      patientInfo.value = response.data
    }
  } catch (error) {
    console.error('加载病情信息失败:', error)
  }
}

const loadAppointments = async () => {
  loading.value = true
  try {
    // 确保请求头中有token
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('登录信息已过期，请重新登录')
      router.push('/login')
      return
    }
    
    const response = await axios.get('/api/patient-portal/my-appointments', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    appointments.value = response.data || []
  } catch (error) {
    console.error('加载预约信息失败:', error)
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    } else {
      ElMessage.error('加载预约信息失败: ' + (error.response?.data?.error || error.message))
    }
  } finally {
    loading.value = false
  }
}

const loadNotifications = async () => {
  try {
    const response = await axios.get('/api/patient-portal/notifications', {
      params: { size: 10 }
    })
    notifications.value = response.data.content || []
  } catch (error) {
    console.error('加载通知失败:', error)
  }
}

const loadUnreadCount = async () => {
  try {
    const response = await axios.get('/api/patient-portal/notifications/unread-count')
    unreadCount.value = response.data.unreadCount || 0
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

const loadCancellationStatus = async () => {
  try {
    const response = await axios.get('/api/patient-portal/cancellation-status')
    cancellationStatus.value = response.data || {}
  } catch (error) {
    console.error('加载取消状态失败:', error)
  }
}

const submitPatientInfo = async () => {
  if (!submitFormRef.value) return
  
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    await axios.post('/api/patient-portal/submit-info', submitForm.value)
    ElMessage.success('病情信息提交成功！')
    showSubmitDialog.value = false
    loadPatientInfo()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '提交失败')
  } finally {
    submitting.value = false
  }
}

// 取消预约功能已移至专门的预约管理页面

const markAsRead = async (notification) => {
  if (notification.isRead) return
  
  try {
    await axios.put(`/api/patient-portal/notifications/${notification.id}/read`)
    notification.isRead = true
    loadUnreadCount()
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const markAllAsRead = async () => {
  // 这里可以实现批量标记已读的逻辑
  for (const notification of notifications.value.filter(n => !n.isRead)) {
    await markAsRead(notification)
  }
}

// 取消预约判断逻辑已移至专门的预约管理页面

const handleDialogClose = (done) => {
  submitFormRef.value?.resetFields()
  done()
}

// 工具函数
const getSeverityTagType = (severity) => {
  const types = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary',
    'LOW': 'info'
  }
  return types[severity] || 'info'
}

const getSeverityText = (severity) => {
  const texts = {
    'EMERGENCY': '紧急',
    'URGENT': '急迫',
    'NORMAL': '普通',
    'LOW': '轻微'
  }
  return texts[severity] || severity
}

const getStatusTagType = (status) => {
  const types = {
    'WAITING': 'info',
    'SCHEDULED': 'warning',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

const getAppointmentStatusTagType = (status) => {
  const types = {
    'SCHEDULED': 'warning',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'primary',
    'DOCTOR_FINAL_CONFIRMED': 'primary',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

const formatDateTime = (appointment) => {
  return `${appointment.plannedDate} ${appointment.plannedStartTime}`
}

const formatTime = (time) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 新增方法
const getTimeTagType = (appointment) => {
  const now = dayjs()
  const startTime = dayjs(`${appointment.plannedDate} ${appointment.plannedStartTime}`)
  
  if (startTime.isBefore(now)) {
    return 'danger'
  } else if (startTime.diff(now, 'hour') <= 48) {
    return 'warning'
  }
  return ''
}

// 取消按钮显示逻辑已移至专门的预约管理页面

const getNotificationIcon = (type) => {
  const iconMap = {
    'SURGERY_SCHEDULED': 'Calendar',
    'SURGERY_CONFIRMED': 'CircleCheck',
    'SURGERY_CANCELLED': 'Close',
    'SURGERY_UPDATED': 'InfoFilled'
  }
  return iconMap[type] || 'Bell'
}

const getNotificationIconClass = (type) => {
  const classMap = {
    'SURGERY_SCHEDULED': 'primary',
    'SURGERY_CONFIRMED': 'success',
    'SURGERY_CANCELLED': 'danger',
    'SURGERY_UPDATED': 'warning'
  }
  return classMap[type] || 'info'
}

const viewAppointments = () => {
  // 跳转到病人预约页面
  router.push('/patient-dashboard/appointments')
}

const viewAllNotifications = () => {
  // 跳转到病人通知页面
  router.push('/patient-dashboard/notifications')
}

const viewProfile = () => {
  router.push('/patient-profile')
}

const viewNotificationDetail = async (notification) => {
  // 标记为已读
  if (!notification.isRead) {
    await markAsRead(notification)
  }
  // 跳转到通知详情页面
  router.push(`/notifications/${notification.id}`)
}

const viewAppointmentDetail = (appointment) => {
  // 跳转到病人的手术预约页面
  router.push('/patient-dashboard/appointments')
}

const formatDate = (date) => {
  return dayjs(date).format('YYYY-MM-DD')
}

const getStatusText = (status) => {
  const texts = {
    'WAITING': '等待中',
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '等待医疗团队确认',
    'TEAM_CONFIRMED': '医疗团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生最终确认',
    'NOTIFIED': '已通知病人',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}
</script>

<style lang="scss" scoped>
.patient-dashboard {
  padding: 24px;
  
  .welcome-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 32px;
    margin-bottom: 24px;
    color: white;
    transition: all 0.3s ease;
    
    &.blacklisted {
      background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%);
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
          
          &.error {
            font-weight: 500;
          }
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
    
    &.stat-danger .stat-icon {
      background: #f56c6c;
    }
  }
  
  .content-row {
    .card {
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      overflow: hidden;
      
      &.mt-3 {
        margin-top: 24px;
      }
      
      .card-header {
        padding: 20px 24px;
        border-bottom: 1px solid #f0f0f0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
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
      }
      
      .card-content {
        padding: 24px;
        
        .loading-container {
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 200px;
        }
        
        .empty-state {
          padding: 40px 0;
          text-align: center;
        }
      }
    }
    
    .patient-info-modern {
      .info-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
        margin-bottom: 24px;
      }
      
      .info-item {
        display: flex;
        flex-direction: column;
        gap: 8px;
        
        .info-label {
          font-size: 13px;
          color: #7f8c8d;
          font-weight: 500;
        }
        
        .info-value {
          font-size: 15px;
          color: #2c3e50;
          font-weight: 600;
        }
      }
      
      .info-description {
        margin-top: 20px;
        padding-top: 20px;
        border-top: 1px solid #f0f0f0;
        
        .info-label {
          font-size: 13px;
          color: #7f8c8d;
          font-weight: 500;
          display: block;
          margin-bottom: 8px;
        }
        
        .description-text {
          font-size: 14px;
          color: #2c3e50;
          line-height: 1.6;
          margin: 0;
        }
      }
    }
    
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
        
        .appointment-actions {
          display: flex;
          align-items: center;
          gap: 10px;
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
    
    .patient-actions-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      
      .patient-action-item {
        display: flex;
        align-items: center;
        padding: 12px;
        border-radius: 8px;
        background: #f8f9fa;
        cursor: pointer;
        transition: all 0.3s ease;
        border: 2px solid transparent;
        min-width: 0;
        
        &:hover:not(.disabled) {
          background: #e9ecef;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          border-color: #e6f7ff;
        }
        
        &.disabled {
          opacity: 0.5;
          cursor: not-allowed;
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
          
          &.appointment-icon {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
          }
          
          &.notification-icon {
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
          }
          
          &.profile-icon {
            background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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
    
    .notification-list {
      .notification-item {
        display: flex;
        align-items: flex-start;
        padding: 12px 0;
        border-bottom: 1px solid #f0f0f0;
        cursor: pointer;
        transition: background-color 0.3s ease;
        
        &:hover {
          background-color: #f5f7fa;
        }
        
        &.unread {
          background-color: #ecf5ff;
          margin: 0 -24px;
          padding: 12px 24px;
          border-left: 3px solid #409eff;
        }
        
        &:last-child {
          border-bottom: none;
        }
        
        .notification-icon {
          font-size: 18px;
          margin-right: 12px;
          margin-top: 2px;
          
          &.primary {
            color: #409eff;
          }
          
          &.success {
            color: #67c23a;
          }
          
          &.warning {
            color: #e6a23c;
          }
          
          &.danger {
            color: #f56c6c;
          }
          
          &.info {
            color: #909399;
          }
        }
        
        .notification-content {
          flex: 1;
          
          p {
            font-size: 14px;
            color: #2c3e50;
            margin-bottom: 4px;
          }
          
          .notification-time {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
    
    .notice-list {
      .notice-item {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 12px 0;
        
        &:not(:last-child) {
          border-bottom: 1px solid #f0f0f0;
        }
        
        .notice-icon {
          font-size: 20px;
          margin-top: 2px;
          
          &.warning {
            color: #e6a23c;
          }
          
          &.info {
            color: #409eff;
          }
          
          &.success {
            color: #67c23a;
          }
        }
        
        p {
          flex: 1;
          font-size: 14px;
          color: #2c3e50;
          line-height: 1.6;
          margin: 0;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .patient-dashboard {
    padding: 16px;
    
    .welcome-card {
      padding: 24px;
      
      .welcome-content {
        .welcome-text h1 {
          font-size: 22px;
        }
        
        .welcome-icon {
          display: none;
        }
      }
    }
    
    .stat-card {
      padding: 16px;
      
      .stat-icon {
        width: 48px;
        height: 48px;
        
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
    
    .content-row {
      .patient-info-modern {
        .info-grid {
          grid-template-columns: 1fr;
        }
      }
    }
  }
}
</style> 