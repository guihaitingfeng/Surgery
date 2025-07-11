<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="!!patientInfo"
    :is-blacklisted="isBlacklisted"
  >
    <div class="patient-appointments-page">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>我的手术预约</h2>
        <div class="header-actions">
          <el-tag type="info">
            本月已取消：{{ cancellationStatus.thisMonthCancellations || 0 }}/1
          </el-tag>
        </div>
      </div>

      <!-- 筛选栏 -->
      <el-card class="filter-card">
        <el-form :inline="true">
          <el-form-item label="预约状态">
            <el-select 
              v-model="filterStatus" 
              placeholder="全部状态" 
              clearable 
              @change="handleFilterChange"
              style="width: 200px"
            >
              <el-option label="已安排" value="SCHEDULED" />
              <el-option label="等待医疗团队确认" value="PENDING_CONFIRMATION" />
              <el-option label="医疗团队已确认" value="TEAM_CONFIRMED" />
              <el-option label="医生最终确认" value="DOCTOR_FINAL_CONFIRMED" />
              <el-option label="已通知病人" value="NOTIFIED" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              @change="handleFilterChange"
              style="width: 280px"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 预约列表 -->
      <div class="appointments-container">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>
        
        <div v-else-if="appointments.length === 0" class="empty-container">
          <el-empty description="暂无手术预约记录" />
        </div>
        
        <div v-else class="appointments-list">
          <el-card 
            v-for="appointment in appointments" 
            :key="appointment.id" 
            class="appointment-card"
            :class="{ 'cancelled': appointment.status === 'CANCELLED' }"
          >
            <div class="appointment-header">
              <div class="header-left">
                <h3>{{ appointment.surgeryName }}</h3>
                <el-tag :type="getStatusTagType(appointment.status)" size="small">
                  {{ getStatusText(appointment.status) }}
                </el-tag>
              </div>
              <div class="header-right">
                <el-button
                  v-if="canCancel(appointment)"
                  type="danger"
                  size="small"
                  plain
                  @click="showCancelDialog(appointment)"
                >
                  取消预约
                </el-button>
              </div>
            </div>
            
            <div class="appointment-body">
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><Calendar /></el-icon>
                    <div class="info-content">
                      <span class="label">手术日期</span>
                      <span class="value">{{ appointment.plannedDate }}</span>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><Clock /></el-icon>
                    <div class="info-content">
                      <span class="label">手术时间</span>
                      <span class="value">{{ appointment.plannedStartTime }} - {{ appointment.plannedEndTime }}</span>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><Location /></el-icon>
                    <div class="info-content">
                      <span class="label">手术室</span>
                      <span class="value">{{ appointment.room?.roomName || '-' }}</span>
                    </div>
                  </div>
                </el-col>
              </el-row>
              
              <el-row :gutter="20" style="margin-top: 16px">
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><User /></el-icon>
                    <div class="info-content">
                      <span class="label">主刀医生</span>
                      <span class="value">{{ appointment.doctor?.realName || '-' }}</span>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><User /></el-icon>
                    <div class="info-content">
                      <span class="label">麻醉师</span>
                      <span class="value">{{ appointment.anesthesiologist?.realName || '待确认' }}</span>
                    </div>
                  </div>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8">
                  <div class="info-item">
                    <el-icon><User /></el-icon>
                    <div class="info-content">
                      <span class="label">护士</span>
                      <span class="value">{{ appointment.nurse?.realName || '待确认' }}</span>
                    </div>
                  </div>
                </el-col>
              </el-row>
              
              <div v-if="appointment.surgeryDescription" class="description">
                <span class="label">手术说明：</span>
                <p>{{ appointment.surgeryDescription }}</p>
              </div>
              
              <div v-if="appointment.status === 'CANCELLED'" class="cancel-info">
                <el-alert
                  title="取消原因"
                  :description="appointment.cancelReason || '未说明'"
                  type="warning"
                  :closable="false"
                />
              </div>
            </div>
            
            <div class="appointment-footer">
              <span class="create-time">
                创建时间：{{ formatDateTime(appointment.createdAt) }}
              </span>
              <el-tag 
                v-if="isUrgent(appointment)" 
                type="danger" 
                size="small"
                effect="dark"
              >
                即将开始
              </el-tag>
            </div>
          </el-card>
        </div>
        
        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination-container">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <!-- 取消预约对话框 -->
      <el-dialog
        v-model="cancelDialogVisible"
        title="取消预约"
        width="500px"
      >
        <el-alert
          title="注意事项"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        >
          <template #default>
            <ul style="margin: 0; padding-left: 20px">
              <li>每月只能取消1次预约</li>
              <li>必须提前48小时取消</li>
              <li>违规取消将被列入黑名单1个月</li>
            </ul>
          </template>
        </el-alert>
        
        <el-form ref="cancelFormRef" :model="cancelForm" :rules="cancelRules">
          <el-form-item label="取消原因" prop="reason">
            <el-input
              v-model="cancelForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入取消原因"
            />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="cancelDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmCancel" :loading="cancelling">
            确认取消
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, Location, User } from '@element-plus/icons-vue'
import PatientLayout from '../../layouts/PatientLayout.vue'
import axios from 'axios'
import dayjs from 'dayjs'

const loading = ref(false)
const appointments = ref([])
const patientInfo = ref(null)
const unreadCount = ref(0)
const isBlacklisted = ref(false)
const cancellationStatus = ref({})

const filterStatus = ref('')
const dateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const cancelDialogVisible = ref(false)
const selectedAppointment = ref(null)
const cancelling = ref(false)
const cancelFormRef = ref()

const cancelForm = ref({
  reason: ''
})

const cancelRules = {
  reason: [
    { required: true, message: '请输入取消原因', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadPatientInfo()
  loadAppointments()
  loadCancellationStatus()
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
    const response = await axios.get('/api/patient-portal/my-appointments')
    let allAppointments = response.data || []
    
    // 前端筛选逻辑
    let filteredAppointments = allAppointments
    
    // 按状态筛选
    if (filterStatus.value) {
      filteredAppointments = filteredAppointments.filter(appointment => 
        appointment.status === filterStatus.value
      )
    }
    
    // 按日期范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
      const [startDate, endDate] = dateRange.value
      filteredAppointments = filteredAppointments.filter(appointment => {
        const appointmentDate = appointment.plannedDate
        return appointmentDate >= startDate && appointmentDate <= endDate
      })
    }
    
    // 分页处理
    const startIndex = (currentPage.value - 1) * pageSize.value
    const endIndex = startIndex + pageSize.value
    
    appointments.value = filteredAppointments.slice(startIndex, endIndex)
    total.value = filteredAppointments.length
    
  } catch (error) {
    ElMessage.error('加载预约列表失败')
  } finally {
    loading.value = false
  }
}

const loadCancellationStatus = async () => {
  try {
    const response = await axios.get('/api/patient-portal/cancellation-status')
    cancellationStatus.value = response.data || {}
    isBlacklisted.value = response.data.isBlacklisted || false
  } catch (error) {
    console.error('加载取消状态失败:', error)
  }
}

const canCancel = (appointment) => {
  // 检查是否在黑名单中
  if (isBlacklisted.value) return false
  
  // 检查本月取消次数
  if (cancellationStatus.value.thisMonthCancellations >= 1) return false
  
  // 检查预约状态是否可以取消
  const cancellableStatuses = ['SCHEDULED', 'PENDING_CONFIRMATION', 'TEAM_CONFIRMED', 'DOCTOR_FINAL_CONFIRMED', 'NOTIFIED']
  if (!cancellableStatuses.includes(appointment.status)) return false
  
  // 检查是否提前48小时
  const surgeryTime = dayjs(`${appointment.plannedDate} ${appointment.plannedStartTime}`)
  const hoursUntil = surgeryTime.diff(dayjs(), 'hour')
  return hoursUntil >= 48
}

const showCancelDialog = (appointment) => {
  selectedAppointment.value = appointment
  cancelDialogVisible.value = true
}

const confirmCancel = async () => {
  if (!cancelFormRef.value) return
  
  const valid = await cancelFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  cancelling.value = true
  try {
    await axios.post(`/api/patient-portal/cancel-appointment/${selectedAppointment.value.id}`, {
      reason: cancelForm.value.reason
    })
    
    ElMessage.success('预约取消成功')
    cancelDialogVisible.value = false
    cancelForm.value.reason = ''
    loadAppointments()
    loadCancellationStatus()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '取消失败')
  } finally {
    cancelling.value = false
  }
}

const isUrgent = (appointment) => {
  if (appointment.status !== 'SCHEDULED' && appointment.status !== 'CONFIRMED') return false
  const surgeryTime = dayjs(`${appointment.plannedDate} ${appointment.plannedStartTime}`)
  const hoursUntil = surgeryTime.diff(dayjs(), 'hour')
  return hoursUntil <= 24 && hoursUntil >= 0
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadAppointments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadAppointments()
}

const resetFilters = () => {
  filterStatus.value = ''
  dateRange.value = null
  currentPage.value = 1
  loadAppointments()
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadAppointments()
}

// 工具函数
const getStatusTagType = (status) => {
  const types = {
    'SCHEDULED': 'warning',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'primary',
    'DOCTOR_FINAL_CONFIRMED': 'primary',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
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

const formatDateTime = (datetime) => {
  return dayjs(datetime).format('YYYY-MM-DD HH:mm')
}
</script>

<style lang="scss" scoped>
.patient-appointments-page {
  padding: 24px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }
  
  .filter-card {
    margin-bottom: 24px;
    
    .el-form {
      .el-form-item {
        margin-bottom: 16px;
        margin-right: 20px;
        
        &:last-child {
          margin-right: 0;
        }
        
        .el-form-item__label {
          font-weight: 500;
          color: #606266;
        }
      }
    }
    
    // 响应式布局
    @media (max-width: 768px) {
      .el-form {
        .el-form-item {
          display: block;
          margin-right: 0;
          margin-bottom: 16px;
          
          .el-select,
          .el-date-editor {
            width: 100% !important;
          }
        }
      }
    }
  }
  
  .appointments-container {
    .loading-container {
      background: white;
      padding: 24px;
      border-radius: 8px;
    }
    
    .empty-container {
      background: white;
      padding: 60px;
      border-radius: 8px;
      text-align: center;
    }
    
    .appointments-list {
      display: grid;
      gap: 16px;
    }
    
    .appointment-card {
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
      }
      
      &.cancelled {
        opacity: 0.8;
        
        .appointment-header h3 {
          text-decoration: line-through;
        }
      }
      
      .appointment-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 20px;
        padding-bottom: 16px;
        border-bottom: 1px solid #ebeef5;
        
        .header-left {
          display: flex;
          align-items: center;
          gap: 12px;
          
          h3 {
            margin: 0;
            font-size: 18px;
            color: #303133;
          }
        }
      }
      
      .appointment-body {
        .info-item {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          margin-bottom: 12px;
          
          .el-icon {
            font-size: 16px;
            color: #909399;
            margin-top: 2px;
          }
          
          .info-content {
            display: flex;
            flex-direction: column;
            
            .label {
              font-size: 12px;
              color: #909399;
              margin-bottom: 2px;
            }
            
            .value {
              font-size: 14px;
              color: #303133;
              font-weight: 500;
            }
          }
        }
        
        .description {
          margin-top: 16px;
          padding-top: 16px;
          border-top: 1px solid #ebeef5;
          
          .label {
            font-size: 14px;
            color: #606266;
            font-weight: 500;
          }
          
          p {
            margin: 8px 0 0 0;
            font-size: 14px;
            color: #303133;
            line-height: 1.6;
          }
        }
        
        .cancel-info {
          margin-top: 16px;
        }
      }
      
      .appointment-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 20px;
        padding-top: 16px;
        border-top: 1px solid #ebeef5;
        
        .create-time {
          font-size: 12px;
          color: #909399;
        }
      }
    }
    
    .pagination-container {
      margin-top: 24px;
      display: flex;
      justify-content: center;
    }
  }
}
</style> 