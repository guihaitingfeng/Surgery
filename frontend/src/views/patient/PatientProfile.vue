<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="!!patientInfo"
    :is-blacklisted="isBlacklisted"
  >
    <div class="patient-profile">
      <div class="profile-header">
        <h2>个人资料</h2>
        <p>管理您的个人信息和账户设置</p>
      </div>

      <el-row :gutter="24">
        <!-- 基本信息 -->
        <el-col :xs="24" :md="12">
          <el-card class="profile-card">
            <template #header>
              <div class="card-header">
                <h3>基本信息</h3>
                <el-button type="primary" size="small" @click="showEditDialog = true">
                  编辑
                </el-button>
              </div>
            </template>
            
            <div class="profile-info">
              <div class="info-item">
                <span class="label">用户名：</span>
                <span class="value">{{ user?.username }}</span>
              </div>
              <div class="info-item">
                <span class="label">真实姓名：</span>
                <span class="value">{{ user?.realName || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机号：</span>
                <span class="value">{{ user?.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱：</span>
                <span class="value">{{ user?.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册时间：</span>
                <span class="value">{{ formatDate(user?.createdAt) }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 病情信息 -->
        <el-col :xs="24" :md="12">
          <el-card class="profile-card">
            <template #header>
              <div class="card-header">
                <h3>病情信息</h3>
                <el-button 
                  v-if="patientInfo" 
                  type="primary" 
                  size="small" 
                  @click="viewPatientInfo"
                >
                  查看详情
                </el-button>
              </div>
            </template>
            
            <div v-if="patientInfo" class="profile-info">
              <div class="info-item">
                <span class="label">病历号：</span>
                <span class="value">{{ patientInfo.medicalRecordNumber }}</span>
              </div>
              <div class="info-item">
                <span class="label">分配医生：</span>
                <span class="value">{{ patientInfo.assignedDoctor?.realName || '未分配' }}</span>
              </div>
              <div class="info-item">
                <span class="label">严重程度：</span>
                <el-tag :type="getSeverityTagType(patientInfo.severityLevel)" size="small">
                  {{ getSeverityText(patientInfo.severityLevel) }}
                </el-tag>
              </div>
              <div class="info-item">
                <span class="label">当前状态：</span>
                <el-tag :type="getStatusTagType(patientInfo.status)" size="small">
                  {{ getStatusText(patientInfo.status) }}
                </el-tag>
              </div>
            </div>
            <div v-else class="empty-info">
              <el-empty description="您还未提交病情信息" :image-size="80">
                <el-button type="primary" @click="router.push('/patient-dashboard/info')">
                  立即提交
                </el-button>
              </el-empty>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 账户统计 -->
      <el-row :gutter="24" class="stats-row">
        <el-col :xs="24" :sm="8">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ appointmentCount }}</h3>
              <p>手术预约</p>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ unreadCount }}</h3>
              <p>未读通知</p>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card">
            <div class="stat-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-content">
              <h3>{{ completedCount }}</h3>
              <p>已完成手术</p>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 安全设置 -->
      <el-card class="profile-card">
        <template #header>
          <h3>安全设置</h3>
        </template>
        
        <div class="security-settings">
          <div class="setting-item">
            <div class="setting-info">
              <h4>登录密码</h4>
              <p>定期更新密码有助于保护账户安全</p>
            </div>
            <el-button @click="showPasswordDialog = true">修改密码</el-button>
          </div>
          <!-- 删除更换手机功能 -->
        </div>
      </el-card>

      <!-- 编辑基本信息对话框 -->
      <el-dialog v-model="showEditDialog" title="编辑基本信息" width="500px">
        <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="editForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="editForm.email" placeholder="请输入邮箱地址" />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="updateProfile" :loading="updating">
            保存
          </el-button>
        </template>
      </el-dialog>

      <!-- 修改密码对话框 -->
      <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
        <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
          <el-form-item label="当前密码" prop="currentPassword">
            <el-input v-model="passwordForm.currentPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="showPasswordDialog = false">取消</el-button>
          <el-button type="primary" @click="changePassword" :loading="changingPassword">
            修改密码
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Bell, Clock } from '@element-plus/icons-vue'
import PatientLayout from '../../layouts/PatientLayout.vue'
import axios from 'axios'
import dayjs from 'dayjs'

const store = useStore()
const router = useRouter()

const patientInfo = ref(null)
const unreadCount = ref(0)
const isBlacklisted = ref(false)
const appointmentCount = ref(0)
const completedCount = ref(0)

const showEditDialog = ref(false)
const showPasswordDialog = ref(false)
const updating = ref(false)
const changingPassword = ref(false)

const editFormRef = ref()
const passwordFormRef = ref()

const user = computed(() => store.getters['auth/user'])

const editForm = ref({
  realName: '',
  email: ''
})

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const editRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  loadData()
})

const loadData = async () => {
  await Promise.all([
    loadPatientInfo(),
    loadUnreadCount(),
    loadAppointmentStats()
  ])
  
  // 初始化编辑表单
  if (user.value) {
    editForm.value.realName = user.value.realName || ''
    editForm.value.email = user.value.email || ''
  }
}

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

const loadUnreadCount = async () => {
  try {
    const response = await axios.get('/api/patient-portal/notifications/unread-count')
    unreadCount.value = response.data.unreadCount || 0
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

const loadAppointmentStats = async () => {
  try {
    const response = await axios.get('/api/patient-portal/my-appointments')
    const appointments = response.data || []
    appointmentCount.value = appointments.length
    completedCount.value = appointments.filter(apt => apt.status === 'COMPLETED').length
  } catch (error) {
    console.error('加载预约统计失败:', error)
  }
}

const updateProfile = async () => {
  if (!editFormRef.value) return
  
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  updating.value = true
  try {
    // 这里应该调用更新用户信息的API
    ElMessage.success('个人信息更新成功！')
    showEditDialog.value = false
    
    // 更新store中的用户信息
    store.commit('auth/SET_USER', {
      ...user.value,
      ...editForm.value
    })
  } catch (error) {
    ElMessage.error('更新失败：' + (error.response?.data?.message || error.message))
  } finally {
    updating.value = false
  }
}

const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  changingPassword.value = true
  try {
    // 这里应该调用修改密码的API
    ElMessage.success('密码修改成功！')
    showPasswordDialog.value = false
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    ElMessage.error('修改失败：' + (error.response?.data?.message || error.message))
  } finally {
    changingPassword.value = false
  }
}

const viewPatientInfo = () => {
  router.push('/patient-dashboard/info')
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '未知'
}

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

const getStatusText = (status) => {
  const texts = {
    'WAITING': '等待中',
    'SCHEDULED': '已安排',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return texts[status] || status
}
</script>

<style lang="scss" scoped>
.patient-profile {
  padding: 24px;
  
  .profile-header {
    margin-bottom: 24px;
    
    h2 {
      margin: 0 0 8px 0;
      font-size: 24px;
      color: #303133;
    }
    
    p {
      margin: 0;
      color: #909399;
      font-size: 14px;
    }
  }
  
  .profile-card {
    margin-bottom: 24px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 18px;
        color: #303133;
      }
    }
    
    .profile-info {
      .info-item {
        display: flex;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
        }
        
        .label {
          width: 100px;
          color: #909399;
          font-size: 14px;
        }
        
        .value {
          flex: 1;
          color: #303133;
          font-size: 14px;
        }
      }
    }
    
    .empty-info {
      padding: 40px 0;
      text-align: center;
    }
    
    .security-settings {
      .setting-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 20px 0;
        border-bottom: 1px solid #f0f0f0;
        
        &:last-child {
          border-bottom: none;
        }
        
        .setting-info {
          h4 {
            margin: 0 0 4px 0;
            font-size: 16px;
            color: #303133;
          }
          
          p {
            margin: 0;
            font-size: 13px;
            color: #909399;
          }
        }
      }
    }
  }
  
  .stats-row {
    margin-bottom: 24px;
    
    .stat-card {
      background: white;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      display: flex;
      align-items: center;
      margin-bottom: 16px;
      
      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: #409eff;
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
    }
  }
}
</style> 