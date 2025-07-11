<template>
  <div class="patient-detail-container">
    <div class="page-header">
      <h2>病人详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="patient" class="patient-detail">
      <!-- 基本信息卡片 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <h3>基本信息</h3>
            <el-tag :type="getSeverityTagType(patient.severityLevel)" size="large">
              {{ getSeverityText(patient.severityLevel) }}
            </el-tag>
          </div>
        </template>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">
            {{ patient.user?.realName }}
          </el-descriptions-item>
          <el-descriptions-item label="性别">
            {{ getGenderText(patient.user?.gender) }}
          </el-descriptions-item>
          <el-descriptions-item label="病历号">
            {{ patient.medicalRecordNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="身份证号">
            {{ patient.idCard }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ patient.user?.phone }}
          </el-descriptions-item>
          <el-descriptions-item label="紧急联系人">
            {{ patient.emergencyContact }}
          </el-descriptions-item>
          <el-descriptions-item label="紧急联系电话">
            {{ patient.emergencyPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="入院日期">
            {{ formatDate(patient.admissionDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="病房号">
            {{ patient.wardNumber || '未分配' }}
          </el-descriptions-item>
          <el-descriptions-item label="床位号">
            {{ patient.bedNumber || '未分配' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusTagType(patient.status)">
              {{ getStatusText(patient.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(patient.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 医疗信息卡片 -->
      <el-card class="info-card">
        <template #header>
          <h3>医疗信息</h3>
        </template>
        
        <div class="medical-info">
          <div class="info-section">
            <h4>病情描述</h4>
            <p class="content">{{ patient.diseaseDescription || '无' }}</p>
          </div>
          
          <div class="info-section">
            <h4>既往病史</h4>
            <p class="content">{{ patient.medicalHistory || '无' }}</p>
          </div>
          
          <div class="info-section">
            <h4>过敏史</h4>
            <p class="content">{{ patient.allergies || '无' }}</p>
          </div>
          
          <div class="info-section">
            <h4>当前用药</h4>
            <p class="content">{{ patient.currentMedications || '无' }}</p>
          </div>
        </div>
      </el-card>

      <!-- 医疗团队卡片 -->
      <el-card class="info-card">
        <template #header>
          <h3>医疗团队</h3>
        </template>
        
        <div class="team-info">
          <div class="team-member">
            <el-avatar :size="60">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <div class="member-info">
              <h4>主治医生</h4>
              <p>{{ patient.assignedDoctor?.realName || '待分配' }}</p>
              <p class="sub-info">{{ patient.assignedDoctor?.department || '' }}</p>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" @click="editPatient">
          编辑信息
        </el-button>
        <el-button 
          v-if="userRole === 'DOCTOR'" 
          type="info" 
          @click="viewHistory"
        >
          查看历史
        </el-button>
        <el-button 
          v-if="userRole === 'DOCTOR'"
          type="success" 
          @click="createAppointment"
        >
          安排手术
        </el-button>
      </div>
    </div>

    <div v-else class="error-container">
      <el-empty description="病人信息不存在或已被删除">
        <el-button type="primary" @click="$router.back()">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const store = useStore()

const loading = ref(true)
const patient = ref(null)

const userRole = computed(() => store.getters['auth/userRole'])

onMounted(() => {
  loadPatientDetail()
})

const loadPatientDetail = async () => {
  try {
    loading.value = true
    const response = await axios.get(`/api/patients/${route.params.id}`)
    patient.value = response.data
  } catch (error) {
    console.error('加载病人详情失败:', error)
    ElMessage.error('加载病人详情失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

const editPatient = () => {
  router.push(`/patients/${route.params.id}/edit`)
}

const viewHistory = async () => {
  try {
    const response = await axios.get(`/api/patients/patient-history/${patient.value.user.id}`)
    const history = response.data
    
    if (history.length === 0) {
      ElMessage.info('该病人暂无历史记录')
      return
    }
    
    ElMessage.success(`该病人共有 ${history.length} 条病情记录`)
    // 这里可以打开一个对话框显示历史记录
    console.log('病人历史记录:', history)
  } catch (error) {
    console.error('获取病人历史记录失败:', error)
    ElMessage.error('获取病人历史记录失败: ' + (error.response?.data?.error || error.message))
  }
}

const createAppointment = () => {
  router.push(`/appointments/create?patientId=${route.params.id}`)
}

const getGenderText = (gender) => {
  const map = { 'MALE': '男', 'FEMALE': '女' }
  return map[gender] || gender
}

const getSeverityText = (severity) => {
  const map = {
    'EMERGENCY': '紧急',
    'URGENT': '急诊', 
    'NORMAL': '普通',
    'LOW': '低级'
  }
  return map[severity] || severity
}

const getSeverityTagType = (severity) => {
  const map = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary', 
    'LOW': 'success'
  }
  return map[severity] || ''
}

const getStatusText = (status) => {
  const map = {
    'WAITING': '等待中',
    'SCHEDULED': '已安排',
    'COMPLETED': '已完成'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = {
    'WAITING': 'warning',
    'SCHEDULED': 'primary',
    'COMPLETED': 'success'
  }
  return map[status] || 'info'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : ''
}
</script>

<style lang="scss" scoped>
.patient-detail-container {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
    }
  }

  .loading-container {
    padding: 20px;
  }

  .patient-detail {
    .info-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        h3 {
          margin: 0;
        }
      }
    }

    .medical-info {
      .info-section {
        margin-bottom: 20px;

        h4 {
          color: #409eff;
          margin-bottom: 8px;
          font-size: 16px;
        }

        .content {
          background-color: #f8f9fa;
          padding: 12px;
          border-radius: 4px;
          border-left: 4px solid #409eff;
          margin: 0;
          line-height: 1.6;
          min-height: 40px;
        }
      }
    }

    .team-info {
      .team-member {
        display: flex;
        align-items: center;
        gap: 16px;

        .member-info {
          h4 {
            margin: 0 0 4px 0;
            color: #2c3e50;
          }

          p {
            margin: 0;
            color: #606266;
          }

          .sub-info {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }

    .action-buttons {
      display: flex;
      gap: 12px;
      margin-top: 20px;
      padding: 20px;
      background-color: #f8f9fa;
      border-radius: 8px;
    }
  }

  .error-container {
    padding: 40px;
    text-align: center;
  }
}
</style> 