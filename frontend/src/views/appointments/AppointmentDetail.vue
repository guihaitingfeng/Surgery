<template>
  <div class="appointment-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button type="text" @click="$router.back()" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h2>手术预约详情</h2>
      </div>
      <div class="header-right" v-if="appointment">
        <el-tag :type="getStatusTagType(appointment.status)" size="large">
          {{ getStatusText(appointment.status) }}
        </el-tag>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="!appointment" class="error-container">
      <el-empty description="未找到预约信息" />
    </div>

    <div v-else class="detail-content">
      <!-- 基本信息 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-button 
              v-if="canEdit" 
              type="primary" 
              size="small"
              @click="editAppointment"
            >
              编辑
            </el-button>
          </div>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <label>手术名称：</label>
              <span>{{ appointment.surgeryName || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>手术类型：</label>
              <span>{{ appointment.surgeryType || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>手术日期：</label>
              <span>{{ appointment.plannedDate || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>手术时间：</label>
              <span>{{ appointment.plannedStartTime || '未设置' }} - {{ appointment.plannedEndTime || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>预估时长：</label>
              <span>{{ appointment.estimatedDuration || 0 }}分钟</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>优先级：</label>
              <el-tag :type="getPriorityTagType(appointment.priorityLevel)">
                {{ getPriorityText(appointment.priorityLevel) }}
              </el-tag>
            </div>
          </el-col>
        </el-row>

        <div v-if="appointment.surgeryDescription" class="description-section">
          <label>手术描述：</label>
          <p>{{ appointment.surgeryDescription }}</p>
        </div>
      </el-card>

      <!-- 病人信息 -->
      <el-card class="info-card">
        <template #header>
          <span>病人信息</span>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <label>姓名：</label>
              <span>{{ appointment.patient?.user?.realName || '未知' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>病历号：</label>
              <span>{{ appointment.patient?.medicalRecordNumber || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>病情描述：</label>
              <span>{{ appointment.patient?.diseaseDescription || '未设置' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>严重程度：</label>
              <el-tag :type="getSeverityTagType(appointment.patient?.severityLevel)">
                {{ getSeverityText(appointment.patient?.severityLevel) }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 医疗团队 -->
      <el-card class="info-card">
        <template #header>
          <span>医疗团队</span>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="info-item">
              <label>主刀医生：</label>
              <span>{{ appointment.doctor?.realName || '未分配' }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>麻醉师：</label>
              <span>{{ appointment.anesthesiologist?.realName || '待确认' }}</span>
              <el-tag 
                v-if="appointment.anesthesiologist && !anesthesiologistConfirmed" 
                type="warning" 
                size="small"
              >
                待确认
              </el-tag>
              <el-tag 
                v-if="anesthesiologistConfirmed" 
                type="success" 
                size="small"
              >
                已确认
              </el-tag>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="info-item">
              <label>手术护士：</label>
              <span>{{ appointment.nurse?.realName || '待确认' }}</span>
              <el-tag 
                v-if="appointment.nurse && !nurseConfirmed" 
                type="warning" 
                size="small"
              >
                待确认
              </el-tag>
              <el-tag 
                v-if="nurseConfirmed" 
                type="success" 
                size="small"
              >
                已确认
              </el-tag>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 手术室信息 -->
      <el-card class="info-card">
        <template #header>
          <span>手术室信息</span>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <label>手术室：</label>
              <span>{{ appointment.room?.roomName || '未分配' }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label>手术床：</label>
              <span>{{ appointment.bed?.bedNumber || '未分配' }}</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 状态流程 -->
      <el-card class="info-card">
        <template #header>
          <span>状态流程</span>
        </template>
        
        <el-steps :active="getActiveStep()" align-center>
          <el-step title="医生安排" description="创建手术预约" />
          <el-step title="团队确认" description="麻醉师和护士确认" />
          <el-step title="通知病人" description="发送手术通知" />
          <el-step title="手术进行" description="手术执行中" />
          <el-step title="手术完成" description="手术结束" />
        </el-steps>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <!-- 发送确认通知给医疗团队 -->
          <el-button 
            v-if="canSendConfirmation" 
            type="primary"
            @click="sendConfirmationToTeam"
            :loading="confirming"
          >
            发送确认通知给医疗团队
          </el-button>

          <!-- 通知病人 -->
          <el-button 
            v-if="canNotifyPatient" 
            type="success"
            @click="notifyPatient"
            :loading="notifying"
          >
            通知病人
          </el-button>

          <!-- 开始手术 -->
          <el-button 
            v-if="canStartSurgery" 
            type="warning"
            @click="startSurgery"
            :loading="starting"
          >
            开始手术
          </el-button>

          <!-- 完成手术 -->
          <el-button 
            v-if="canCompleteSurgery" 
            type="success"
            @click="showCompleteDialog"
          >
            完成手术
          </el-button>

          <!-- 取消预约 -->
          <el-button 
            v-if="canCancel" 
            type="danger"
            @click="showCancelDialog"
          >
            取消预约
          </el-button>
        </div>
      </el-card>

      <!-- 备注信息 -->
      <el-card v-if="appointment.preSurgeryNotes || appointment.postSurgeryNotes" class="info-card">
        <template #header>
          <span>备注信息</span>
        </template>
        
        <div v-if="appointment.preSurgeryNotes" class="notes-section">
          <label>术前备注：</label>
          <p>{{ appointment.preSurgeryNotes }}</p>
        </div>
        
        <div v-if="appointment.postSurgeryNotes" class="notes-section">
          <label>术后备注：</label>
          <p>{{ appointment.postSurgeryNotes }}</p>
        </div>
      </el-card>
    </div>

    <!-- 完成手术对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成手术" width="500px">
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules">
        <el-form-item label="术后备注" prop="postSurgeryNotes">
          <el-input 
            v-model="completeForm.postSurgeryNotes"
            type="textarea"
            :rows="4"
            placeholder="请输入术后备注信息"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="completeSurgery" :loading="completing">
          确认完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 取消预约对话框 -->
    <el-dialog v-model="cancelDialogVisible" title="取消预约" width="500px">
      <el-form ref="cancelFormRef" :model="cancelForm" :rules="cancelRules">
        <el-form-item label="取消原因" prop="reason">
          <el-input 
            v-model="cancelForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="cancelAppointment" :loading="cancelling">
          确认取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const store = useStore()

const loading = ref(false)
const appointment = ref(null)
const confirming = ref(false)
const notifying = ref(false)
const starting = ref(false)
const completing = ref(false)
const cancelling = ref(false)

const completeDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const completeFormRef = ref()
const cancelFormRef = ref()

const completeForm = ref({
  postSurgeryNotes: ''
})

const cancelForm = ref({
  reason: ''
})

const completeRules = {
  postSurgeryNotes: [
    { required: true, message: '请输入术后备注', trigger: 'blur' }
  ]
}

const cancelRules = {
  reason: [
    { required: true, message: '请输入取消原因', trigger: 'blur' }
  ]
}

// 计算属性
const userRole = computed(() => store.getters['auth/userRole'])
const canEdit = computed(() => 
  ['DOCTOR', 'ADMIN'].includes(userRole.value) && 
  appointment.value?.status !== 'COMPLETED'
)

const anesthesiologistConfirmed = computed(() => {
  if (!appointment.value) return false
  return appointment.value.anesthesiologistConfirmed === true
})

const nurseConfirmed = computed(() => {
  if (!appointment.value) return false
  // 直接使用后端返回的确认状态字段
  return appointment.value.nurseConfirmed === true
})

const canSendConfirmation = computed(() => 
  userRole.value === 'DOCTOR' && appointment.value?.status === 'SCHEDULED'
)

const canNotifyPatient = computed(() => 
  userRole.value === 'DOCTOR' && appointment.value?.status === 'DOCTOR_FINAL_CONFIRMED'
)

const canStartSurgery = computed(() => 
  userRole.value === 'DOCTOR' && appointment.value?.status === 'NOTIFIED'
)

const canCompleteSurgery = computed(() => 
  userRole.value === 'DOCTOR' && appointment.value?.status === 'IN_PROGRESS'
)

const canCancel = computed(() => 
  false // 医生不能取消预约，只有病人可以在病人端取消
)

// 实时刷新相关
let refreshTimer = null

const startRealTimeRefresh = () => {
  // 每30秒刷新一次预约详情
  refreshTimer = setInterval(() => {
    loadAppointmentDetail()
  }, 30000)
}

const stopRealTimeRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadAppointmentDetail()
  // 启动实时刷新
  startRealTimeRefresh()
})

onUnmounted(() => {
  // 清理定时器
  stopRealTimeRefresh()
})

const loadAppointmentDetail = async () => {
  loading.value = true
  try {
    console.log('正在加载预约详情，ID:', route.params.id)
    const response = await axios.get(`/api/appointments/${route.params.id}`, {
      // 添加缓存控制头，避免304问题
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      }
    })
    console.log('预约详情加载成功:', response.data)
    appointment.value = response.data
  } catch (error) {
    console.error('加载预约详情失败:', error)
    ElMessage.error('加载预约详情失败: ' + (error.response?.data?.error || error.message))
    router.back()
  } finally {
    loading.value = false
  }
}

const getActiveStep = () => {
  if (!appointment.value) return 0
  const statusStepMap = {
    'SCHEDULED': 0,
    'PENDING_CONFIRMATION': 1,
    'TEAM_CONFIRMED': 1,
    'DOCTOR_FINAL_CONFIRMED': 2,
    'NOTIFIED': 2,
    'IN_PROGRESS': 3,
    'COMPLETED': 4
  }
  return statusStepMap[appointment.value.status] || 0
}

const sendConfirmationToTeam = async () => {
  confirming.value = true
  try {
    await axios.post(`/api/appointments/${appointment.value.id}/send-confirmation`)
    ElMessage.success('确认通知已发送给医疗团队')
    loadAppointmentDetail()
  } catch (error) {
    ElMessage.error('发送确认通知失败')
  } finally {
    confirming.value = false
  }
}

const notifyPatient = async () => {
  notifying.value = true
  try {
    await axios.post(`/api/appointments/${appointment.value.id}/notify-patient`)
    ElMessage.success('已通知病人')
    loadAppointmentDetail()
  } catch (error) {
    ElMessage.error('通知病人失败')
  } finally {
    notifying.value = false
  }
}

const startSurgery = async () => {
  starting.value = true
  try {
    await axios.post(`/api/appointments/${appointment.value.id}/start`)
    ElMessage.success('手术已开始')
    loadAppointmentDetail()
  } catch (error) {
    ElMessage.error('开始手术失败')
  } finally {
    starting.value = false
  }
}

const showCompleteDialog = () => {
  completeDialogVisible.value = true
}

const completeSurgery = async () => {
  if (!completeFormRef.value) return
  
  const valid = await completeFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  completing.value = true
  try {
    await axios.post(`/api/appointments/${appointment.value.id}/complete`, {
      postSurgeryNotes: completeForm.value.postSurgeryNotes
    })
    ElMessage.success('手术已完成')
    completeDialogVisible.value = false
    loadAppointmentDetail()
  } catch (error) {
    ElMessage.error('完成手术失败')
  } finally {
    completing.value = false
  }
}

const showCancelDialog = () => {
  cancelDialogVisible.value = true
}

const cancelAppointment = async () => {
  if (!cancelFormRef.value) return
  
  const valid = await cancelFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  cancelling.value = true
  try {
    await axios.post(`/api/appointments/${appointment.value.id}/cancel`, {
      reason: cancelForm.value.reason
    })
    ElMessage.success('预约已取消')
    cancelDialogVisible.value = false
    router.back()
  } catch (error) {
    ElMessage.error('取消预约失败')
  } finally {
    cancelling.value = false
  }
}

const editAppointment = () => {
  router.push(`/appointments/${appointment.value.id}/edit`)
}

// 状态相关的辅助函数
const getStatusText = (status) => {
  const map = {
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '等待医疗团队确认',
    'TEAM_CONFIRMED': '医疗团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生最终确认',
    'NOTIFIED': '已通知病人',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'POSTPONED': '已延期'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = {
    'SCHEDULED': 'warning',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'primary',
    'DOCTOR_FINAL_CONFIRMED': 'primary',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'POSTPONED': 'info'
  }
  return map[status] || 'info'
}

const getPriorityText = (priority) => {
  const map = {
    'EMERGENCY': '紧急',
    'URGENT': '急迫',
    'NORMAL': '普通',
    'LOW': '轻微'
  }
  return map[priority] || priority || '普通'
}

const getPriorityTagType = (priority) => {
  const map = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary',
    'LOW': 'success'
  }
  return map[priority] || 'primary'
}

const getSeverityText = (severity) => {
  const map = {
    'EMERGENCY': '紧急',
    'URGENT': '急迫',
    'NORMAL': '普通',
    'LOW': '轻微'
  }
  return map[severity] || severity || '普通'
}

const getSeverityTagType = (severity) => {
  const map = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary',
    'LOW': 'success'
  }
  return map[severity] || 'primary'
}
</script>

<style lang="scss" scoped>
.appointment-detail-container {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .back-btn {
        padding: 8px 12px;
        font-size: 14px;
        
        .el-icon {
          margin-right: 4px;
        }
      }
      
      h2 {
        margin: 0;
        color: #2c3e50;
      }
    }
  }
  
  .loading-container {
    padding: 20px;
  }
  
  .error-container {
    padding: 40px;
    text-align: center;
  }
  
  .detail-content {
    .info-card {
      margin-bottom: 20px;
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: bold;
      }
      
      .info-item {
        margin-bottom: 12px;
        
        label {
          font-weight: 500;
          color: #606266;
          margin-right: 8px;
        }
        
        span {
          color: #2c3e50;
        }
      }
      
      .description-section, .notes-section {
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px solid #ebeef5;
        
        label {
          font-weight: 500;
          color: #606266;
          display: block;
          margin-bottom: 8px;
        }
        
        p {
          margin: 0;
          color: #2c3e50;
          line-height: 1.6;
        }
      }
      
      .action-buttons {
        margin-top: 20px;
        padding-top: 20px;
        border-top: 1px solid #ebeef5;
        display: flex;
        gap: 12px;
        flex-wrap: wrap;
      }
    }
  }
}
</style> 