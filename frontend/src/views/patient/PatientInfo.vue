<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="patientHistory.length > 0"
    :is-blacklisted="isBlacklisted"
    @show-submit-dialog="showSubmitDialog = true"
  >
    <div class="patient-info-page">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>我的病情信息</h2>
        <div class="header-buttons">
          <el-button 
            type="primary"
            @click="showSubmitDialog = true"
            :disabled="isBlacklisted"
          >
            <el-icon><Plus /></el-icon>
            新建病情
          </el-button>
          <el-button 
            v-if="patientHistory.length > 0"
            type="success"
            @click="showNewFromHistoryDialog = true"
            :disabled="isBlacklisted"
          >
            <el-icon><DocumentCopy /></el-icon>
            复诊新建
          </el-button>
        </div>
      </div>

      <!-- 病情信息列表 -->
      <div v-if="patientHistory.length > 0" class="info-list-container">
        <el-card class="list-card">
          <template #header>
            <div class="list-header">
              <h3>病情记录列表</h3>
              <el-text type="info">共 {{ patientHistory.length }} 条记录</el-text>
            </div>
          </template>
          
          <div class="info-list">
            <div 
              v-for="(record, index) in patientHistory" 
              :key="record.id"
              class="info-item"
              :class="{ current: index === 0 }"
              @click="openDetailDialog(record)"
            >
              <div class="item-header">
                <div class="item-title">
                  <span class="record-date">{{ formatDate(record.createdAt) }}</span>
                  <div class="record-tags">
                    <el-tag v-if="index === 0" type="success" size="small">当前</el-tag>
                    <el-tag :type="getSeverityTagType(record.severityLevel)" size="small">
                      {{ getSeverityText(record.severityLevel) }}
                    </el-tag>
                  </div>
                </div>
                <div class="item-actions">
                  <el-button 
                    type="primary" 
                    size="small" 
                    link
                    @click.stop="openDetailDialog(record)"
                  >
                    查看详情
                  </el-button>
                </div>
              </div>
              
              <div class="item-content">
                <div class="content-row">
                  <span class="label">病历号：</span>
                  <span class="value">{{ record.medicalRecordNumber }}</span>
                </div>
                <div class="content-row">
                  <span class="label">主治医生：</span>
                  <span class="value">{{ record.assignedDoctor?.realName || '未分配' }}</span>
                </div>
                <div class="content-row">
                  <span class="label">病情描述：</span>
                  <span class="value description">{{ record.diseaseDescription }}</span>
                </div>
                <div class="content-row">
                  <span class="label">状态：</span>
                  <el-tag :type="getStatusTagType(record.status)" size="small">
                    {{ getStatusText(record.status) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-container">
        <el-empty description="您还未提交病情信息">
          <el-button 
            type="primary" 
            @click="showSubmitDialog = true"
            :disabled="isBlacklisted"
          >
            立即提交
          </el-button>
        </el-empty>
      </div>

      <!-- 新建病情信息对话框 -->
      <el-dialog 
        v-model="showSubmitDialog" 
        title="新建病情信息" 
        width="600px"
        :before-close="handleSubmitDialogClose"
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
          <el-form-item label="既往病史">
            <el-input 
              v-model="submitForm.medicalHistory" 
              type="textarea" 
              :rows="2"
              placeholder="请填写既往病史（如无请填无）"
            />
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

      <!-- 复诊新建对话框 -->
      <el-dialog 
        v-model="showNewFromHistoryDialog" 
        title="复诊新建" 
        width="700px"
        :before-close="handleNewFromHistoryDialogClose"
      >
        <div class="new-from-history-content">
          <!-- 选择历史记录 -->
          <div class="history-selection">
            <h4>选择要参考的历史病情记录</h4>
            <div class="history-options">
              <div 
                v-for="record in patientHistory" 
                :key="record.id"
                class="history-option"
                :class="{ active: selectedHistoryId === record.id }"
                @click="selectHistoryRecord(record)"
              >
                <div class="option-header">
                  <span class="option-date">{{ formatDate(record.createdAt) }}</span>
                  <el-tag :type="getSeverityTagType(record.severityLevel)" size="small">
                    {{ getSeverityText(record.severityLevel) }}
                  </el-tag>
                </div>
                <div class="option-content">
                  <p>{{ record.diseaseDescription }}</p>
                  <p class="option-doctor">主治医生：{{ record.assignedDoctor?.realName || '未分配' }}</p>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 新建表单 -->
          <div class="new-form-section" v-if="selectedHistoryId">
            <h4>新建病情信息</h4>
            <el-alert 
              title="提示" 
              type="info" 
              description="以下信息已根据您选择的历史记录自动填充，请根据当前病情进行修改" 
              show-icon
              :closable="false"
              style="margin-bottom: 20px"
            />
            
            <el-form :model="newFromHistoryForm" :rules="submitRules" ref="newFromHistoryFormRef" label-width="120px">
              <el-form-item label="病历号" prop="medicalRecordNumber">
                <el-input v-model="newFromHistoryForm.medicalRecordNumber" placeholder="请输入病历号" />
              </el-form-item>
              <el-form-item label="身份证号" prop="idCard">
                <el-input v-model="newFromHistoryForm.idCard" placeholder="请输入身份证号" />
              </el-form-item>
              <el-form-item label="紧急联系人" prop="emergencyContact">
                <el-input v-model="newFromHistoryForm.emergencyContact" placeholder="请输入紧急联系人姓名" />
              </el-form-item>
              <el-form-item label="紧急联系电话" prop="emergencyPhone">
                <el-input v-model="newFromHistoryForm.emergencyPhone" placeholder="请输入紧急联系电话" />
              </el-form-item>
              <el-form-item label="病情描述" prop="diseaseDescription">
                <el-input 
                  v-model="newFromHistoryForm.diseaseDescription" 
                  type="textarea" 
                  :rows="4"
                  placeholder="请详细描述您的病情"
                />
              </el-form-item>
              <el-form-item label="严重程度" prop="severityLevel">
                <el-select v-model="newFromHistoryForm.severityLevel" placeholder="请选择病情严重程度">
                  <el-option label="紧急" value="EMERGENCY" />
                  <el-option label="急迫" value="URGENT" />
                  <el-option label="普通" value="NORMAL" />
                  <el-option label="轻微" value="LOW" />
                </el-select>
              </el-form-item>
              <el-form-item label="既往病史">
                <el-input 
                  v-model="newFromHistoryForm.medicalHistory" 
                  type="textarea" 
                  :rows="2"
                  placeholder="请填写既往病史（如无请填无）"
                />
              </el-form-item>
              <el-form-item label="过敏史">
                <el-input 
                  v-model="newFromHistoryForm.allergies" 
                  type="textarea" 
                  :rows="2"
                  placeholder="请填写过敏史（如无请填无）"
                />
              </el-form-item>
              <el-form-item label="当前用药">
                <el-input 
                  v-model="newFromHistoryForm.currentMedications" 
                  type="textarea" 
                  :rows="2"
                  placeholder="请填写当前用药情况（如无请填无）"
                />
              </el-form-item>
            </el-form>
          </div>
        </div>
        
        <template #footer>
          <el-button @click="showNewFromHistoryDialog = false">取消</el-button>
          <el-button 
            type="primary" 
            @click="submitNewFromHistory" 
            :loading="submittingNewFromHistory"
            :disabled="!selectedHistoryId"
          >
            提交新建
          </el-button>
        </template>
      </el-dialog>

      <!-- 病情详情对话框 -->
      <el-dialog 
        v-model="showDetailDialog" 
        title="病情详情" 
        width="800px"
      >
        <div v-if="selectedRecord" class="detail-content">
          <!-- 基本信息 -->
          <el-card class="detail-card">
            <template #header>
              <div class="detail-header">
                <h3>基本信息</h3>
                <div class="detail-tags">
                  <el-tag v-if="isCurrentRecord" type="success" size="small">当前</el-tag>
                  <el-tag :type="getSeverityTagType(selectedRecord.severityLevel)" size="small">
                    {{ getSeverityText(selectedRecord.severityLevel) }}
                  </el-tag>
                </div>
              </div>
            </template>
            
            <el-descriptions :column="2" border>
              <el-descriptions-item label="病历号">
                {{ selectedRecord.medicalRecordNumber }}
              </el-descriptions-item>
              <el-descriptions-item label="身份证号">
                {{ selectedRecord.idCard }}
              </el-descriptions-item>
              <el-descriptions-item label="紧急联系人">
                {{ selectedRecord.emergencyContact }}
              </el-descriptions-item>
              <el-descriptions-item label="紧急联系电话">
                {{ selectedRecord.emergencyPhone }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDate(selectedRecord.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="getStatusTagType(selectedRecord.status)">
                  {{ getStatusText(selectedRecord.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 医疗信息 -->
          <el-card class="detail-card">
            <template #header>
              <h3>医疗信息</h3>
            </template>
            
            <div class="medical-info">
              <div class="info-section">
                <h4>病情描述</h4>
                <p>{{ selectedRecord.diseaseDescription }}</p>
              </div>
              
              <div class="info-section">
                <h4>既往病史</h4>
                <p>{{ selectedRecord.medicalHistory || '无' }}</p>
              </div>
              
              <div class="info-section">
                <h4>过敏史</h4>
                <p>{{ selectedRecord.allergies || '无' }}</p>
              </div>
              
              <div class="info-section">
                <h4>当前用药</h4>
                <p>{{ selectedRecord.currentMedications || '无' }}</p>
              </div>
            </div>
          </el-card>

          <!-- 医疗团队 -->
          <el-card class="detail-card">
            <template #header>
              <h3>医疗团队</h3>
            </template>
            
            <div class="team-info">
              <div class="team-member">
                <el-avatar :size="60" :src="doctorAvatar">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <div class="member-info">
                  <h4>主治医生</h4>
                  <p>{{ selectedRecord.assignedDoctor?.realName || '待分配' }}</p>
                  <p class="sub-info">{{ selectedRecord.assignedDoctor?.department || '' }}</p>
                </div>
              </div>
            </div>
          </el-card>
        </div>
        
        <template #footer>
          <el-button @click="showDetailDialog = false">关闭</el-button>
          <el-button 
            v-if="isCurrentRecord && !isBlacklisted"
            type="primary"
            @click="editCurrentRecord"
          >
            修改信息
          </el-button>
        </template>
      </el-dialog>

      <!-- 修改病情信息对话框 -->
      <el-dialog 
        v-model="showEditDialog" 
        title="修改病情信息" 
        width="600px"
      >
        <el-alert 
          title="温馨提示" 
          type="warning" 
          description="修改病情信息需要医生审核，请谨慎操作" 
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
          <el-form-item label="病情描述" prop="diseaseDescription">
            <el-input 
              v-model="editForm.diseaseDescription" 
              type="textarea" 
              :rows="4"
              placeholder="请详细描述您的病情"
            />
          </el-form-item>
          <el-form-item label="严重程度" prop="severityLevel">
            <el-select v-model="editForm.severityLevel" placeholder="请选择病情严重程度">
              <el-option label="紧急" value="EMERGENCY" />
              <el-option label="急迫" value="URGENT" />
              <el-option label="普通" value="NORMAL" />
              <el-option label="轻微" value="LOW" />
            </el-select>
          </el-form-item>
          <el-form-item label="当前用药">
            <el-input 
              v-model="editForm.currentMedications" 
              type="textarea" 
              :rows="2"
              placeholder="请填写当前用药情况（如无请填无）"
            />
          </el-form-item>
        </el-form>
        
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="updatePatientInfo" :loading="updating">
            保存修改
          </el-button>
        </template>
      </el-dialog>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Plus, Edit, UserFilled, DocumentCopy } from '@element-plus/icons-vue'
import PatientLayout from '../../layouts/PatientLayout.vue'
import axios from 'axios'
import dayjs from 'dayjs'

const store = useStore()

const unreadCount = ref(0)
const isBlacklisted = ref(false)
const showSubmitDialog = ref(false)
const showEditDialog = ref(false)
const showNewFromHistoryDialog = ref(false)
const showDetailDialog = ref(false)
const submitting = ref(false)
const updating = ref(false)
const submittingNewFromHistory = ref(false)
const submitFormRef = ref()
const editFormRef = ref()
const newFromHistoryFormRef = ref()
const patientHistory = ref([])
const selectedHistoryId = ref(null)
const selectedRecord = ref(null)

const doctorAvatar = computed(() => null)
const isCurrentRecord = computed(() => 
  selectedRecord.value && patientHistory.value.length > 0 && 
  selectedRecord.value.id === patientHistory.value[0].id
)

const submitForm = ref({
  medicalRecordNumber: '',
  idCard: '',
  emergencyContact: '',
  emergencyPhone: '',
  diseaseDescription: '',
  severityLevel: '',
  medicalHistory: '',
  allergies: '',
  currentMedications: ''
})

const editForm = ref({
  diseaseDescription: '',
  severityLevel: '',
  currentMedications: ''
})

const newFromHistoryForm = ref({
  medicalRecordNumber: '',
  idCard: '',
  emergencyContact: '',
  emergencyPhone: '',
  diseaseDescription: '',
  severityLevel: '',
  medicalHistory: '',
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

const editRules = {
  diseaseDescription: [
    { required: true, message: '请描述病情', trigger: 'blur' }
  ],
  severityLevel: [
    { required: true, message: '请选择严重程度', trigger: 'change' }
  ]
}

onMounted(() => {
  loadPatientHistory()
  checkBlacklistStatus()
})

const loadPatientHistory = async () => {
  try {
    const response = await axios.get('/api/patient-portal/my-history')
    if (response.data && Array.isArray(response.data)) {
      patientHistory.value = response.data
    }
  } catch (error) {
    console.error('加载病情历史失败:', error)
  }
}

const checkBlacklistStatus = async () => {
  try {
    const response = await axios.get('/api/patient-portal/cancellation-status')
    isBlacklisted.value = response.data.isBlacklisted || false
  } catch (error) {
    console.error('检查黑名单状态失败:', error)
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
    loadPatientHistory()
    resetForm()
    submitFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '提交失败')
  } finally {
    submitting.value = false
  }
}

const updatePatientInfo = async () => {
  if (!editFormRef.value) return
  
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  updating.value = true
  try {
    ElMessage.info('修改功能开发中...')
    showEditDialog.value = false
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    updating.value = false
  }
}

const handleSubmitDialogClose = (done) => {
  resetForm()
  submitFormRef.value?.clearValidate()
  if (done) done()
}

const handleNewFromHistoryDialogClose = (done) => {
  newFromHistoryFormRef.value?.resetFields()
  newFromHistoryFormRef.value?.clearValidate()
  selectedHistoryId.value = null
  resetNewFromHistoryForm()
  if (done) done()
}

const selectHistoryRecord = (record) => {
  newFromHistoryForm.value = {
    medicalRecordNumber: record.medicalRecordNumber,
    idCard: record.idCard,
    emergencyContact: record.emergencyContact,
    emergencyPhone: record.emergencyPhone,
    diseaseDescription: record.diseaseDescription,
    severityLevel: record.severityLevel,
    medicalHistory: record.medicalHistory,
    allergies: record.allergies,
    currentMedications: record.currentMedications
  }
  selectedHistoryId.value = record.id
}

const submitNewFromHistory = async () => {
  if (!newFromHistoryFormRef.value) return
  
  const valid = await newFromHistoryFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  submittingNewFromHistory.value = true
  try {
    await axios.post('/api/patient-portal/submit-info', newFromHistoryForm.value)
    ElMessage.success('基于历史病情新建成功！')
    showNewFromHistoryDialog.value = false
    selectedHistoryId.value = null
    loadPatientHistory()
    resetNewFromHistoryForm()
    newFromHistoryFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '新建失败')
  } finally {
    submittingNewFromHistory.value = false
  }
}

const openDetailDialog = (record) => {
  selectedRecord.value = record
  showDetailDialog.value = true
}

const editCurrentRecord = () => {
  if (selectedRecord.value) {
    editForm.value = {
      diseaseDescription: selectedRecord.value.diseaseDescription,
      severityLevel: selectedRecord.value.severityLevel,
      currentMedications: selectedRecord.value.currentMedications
    }
    showDetailDialog.value = false
    showEditDialog.value = true
  }
}

const resetForm = () => {
  submitForm.value = {
    medicalRecordNumber: '',
    idCard: '',
    emergencyContact: '',
    emergencyPhone: '',
    diseaseDescription: '',
    severityLevel: '',
    medicalHistory: '',
    allergies: '',
    currentMedications: ''
  }
}

const resetNewFromHistoryForm = () => {
  newFromHistoryForm.value = {
    medicalRecordNumber: '',
    idCard: '',
    emergencyContact: '',
    emergencyPhone: '',
    diseaseDescription: '',
    severityLevel: '',
    medicalHistory: '',
    allergies: '',
    currentMedications: ''
  }
}

// 工具函数
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
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
.patient-info-page {
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
    
    .header-buttons {
      display: flex;
      gap: 12px;
    }
  }
  
  .info-list-container {
    .list-card {
      .list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        h3 {
          margin: 0;
          font-size: 18px;
          color: #303133;
        }
      }
      
      .info-list {
        .info-item {
          padding: 20px;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          margin-bottom: 16px;
          cursor: pointer;
          transition: all 0.3s ease;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          &:hover {
            border-color: #409eff;
            box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
          }
          
          &.current {
            border-color: #67c23a;
            background-color: #f0f9ff;
          }
          
          .item-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;
            
            .item-title {
              display: flex;
              flex-direction: column;
              gap: 8px;
              
              .record-date {
                font-size: 16px;
                font-weight: 500;
                color: #303133;
              }
              
              .record-tags {
                display: flex;
                gap: 8px;
              }
            }
            
            .item-actions {
              flex-shrink: 0;
            }
          }
          
          .item-content {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
            
            .content-row {
              display: flex;
              align-items: flex-start;
              gap: 8px;
              
              .label {
                font-size: 14px;
                color: #606266;
                white-space: nowrap;
                min-width: 80px;
              }
              
              .value {
                font-size: 14px;
                color: #303133;
                flex: 1;
                
                &.description {
                  display: -webkit-box;
                  -webkit-line-clamp: 2;
                  -webkit-box-orient: vertical;
                  overflow: hidden;
                  line-height: 1.4;
                }
              }
            }
          }
        }
      }
    }
  }
  
  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  // 复诊新建对话框样式
  .new-from-history-content {
    max-height: 600px;
    overflow-y: auto;
    
    .history-selection {
      margin-bottom: 24px;
      
      h4 {
        margin-bottom: 16px;
        color: #303133;
        font-size: 16px;
      }
      
      .history-options {
        display: grid;
        gap: 12px;
        max-height: 200px;
        overflow-y: auto;
        
        .history-option {
          padding: 12px;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.3s;
          
          &:hover {
            border-color: #409eff;
            background-color: #f0f9ff;
          }
          
          &.active {
            border-color: #409eff;
            background-color: #e6f7ff;
          }
          
          .option-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
            
            .option-date {
              font-size: 13px;
              color: #606266;
              font-weight: 500;
            }
          }
          
          .option-content {
            p {
              font-size: 13px;
              color: #303133;
              margin: 0 0 4px 0;
              line-height: 1.4;
              
              &.option-doctor {
                font-size: 12px;
                color: #909399;
                margin: 0;
              }
            }
          }
        }
      }
    }
    
    .new-form-section {
      h4 {
        margin-bottom: 16px;
        color: #303133;
        font-size: 16px;
      }
    }
  }
  
  // 病情详情对话框样式
  .detail-content {
    .detail-card {
      margin-bottom: 20px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .detail-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        h3 {
          margin: 0;
          font-size: 18px;
          color: #303133;
        }
        
        .detail-tags {
          display: flex;
          gap: 8px;
        }
      }
      
      .medical-info {
        .info-section {
          margin-bottom: 24px;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          h4 {
            font-size: 14px;
            color: #606266;
            margin-bottom: 8px;
          }
          
          p {
            font-size: 14px;
            color: #303133;
            line-height: 1.6;
            margin: 0;
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
              font-size: 14px;
              color: #606266;
              margin: 0 0 4px 0;
            }
            
            p {
              font-size: 16px;
              color: #303133;
              margin: 0;
              
              &.sub-info {
                font-size: 13px;
                color: #909399;
                margin-top: 4px;
              }
            }
          }
        }
      }
    }
  }
  
  // 响应式设计
  @media (max-width: 768px) {
    .info-list-container {
      .list-card {
        .info-list {
          .info-item {
            .item-content {
              grid-template-columns: 1fr;
            }
          }
        }
      }
    }
  }
}
</style> 