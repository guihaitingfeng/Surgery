<template>
  <div class="appointment-form-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑手术预约' : '创建手术预约' }}</h2>
    </div>

    <div class="card">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="病人" prop="patientId">
              <el-select 
                v-model="formData.patientId" 
                placeholder="选择病人"
                filterable
                style="width: 100%"
                :disabled="!!route.query.patientId"
              >
                <el-option
                  v-for="patient in patientOptions"
                  :key="patient.id"
                  :label="`${patient.user?.realName} (${patient.medicalRecordNumber}) - ${getSeverityText(patient.severityLevel)}`"
                  :value="patient.id"
                />
              </el-select>
              <div v-if="isFromPatientDetail && selectedPatientName" class="patient-info-tip">
                <el-alert
                  :title="`已为病人 ${selectedPatientName} 安排手术`"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手术名称" prop="surgeryName">
              <el-input 
                v-model="formData.surgeryName" 
                placeholder="请输入手术名称"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="手术类型" prop="surgeryType">
              <el-input 
                v-model="formData.surgeryType" 
                placeholder="请输入手术类型"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手术室" prop="roomId">
              <el-select 
                v-model="formData.roomId" 
                placeholder="选择手术室"
                style="width: 100%"
                @change="handleRoomChange"
              >
                <el-option
                  v-for="room in roomOptions"
                  :key="room.id"
                  :label="`${room.roomName} (${room.roomNumber})`"
                  :value="room.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="床位" prop="bedId">
              <el-select 
                v-model="formData.bedId" 
                placeholder="选择床位"
                style="width: 100%"
                :disabled="!formData.roomId"
              >
                <el-option
                  v-for="bed in bedOptions"
                  :key="bed.id"
                  :label="`${bed.bedNumber} (${bed.bedType || '标准床位'})`"
                  :value="bed.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="麻醉师" prop="anesthesiologistId">
              <el-select 
                v-model="formData.anesthesiologistId" 
                placeholder="选择麻醉师"
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="anesthesiologist in anesthesiologistOptions"
                  :key="anesthesiologist.id"
                  :label="anesthesiologist.realName"
                  :value="anesthesiologist.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="护士" prop="nurseId">
              <el-select 
                v-model="formData.nurseId" 
                placeholder="选择护士"
                style="width: 100%"
                clearable
              >
                <el-option
                  v-for="nurse in nurseOptions"
                  :key="nurse.id"
                  :label="nurse.realName"
                  :value="nurse.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priorityLevel">
              <el-select 
                v-model="formData.priorityLevel" 
                placeholder="选择优先级"
                style="width: 100%"
              >
                <el-option label="紧急" value="EMERGENCY" />
                <el-option label="急诊" value="URGENT" />
                <el-option label="普通" value="NORMAL" />
                <el-option label="低级" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="手术日期" prop="plannedDate">
              <el-date-picker
                v-model="formData.plannedDate"
                type="date"
                placeholder="选择手术日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :disabled-date="disabledDate"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始时间" prop="plannedStartTime">
              <el-time-picker
                v-model="formData.plannedStartTime"
                placeholder="选择开始时间"
                format="HH:mm"
                value-format="HH:mm"
                style="width: 100%"
                @change="calculateDuration"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" prop="plannedEndTime">
              <el-time-picker
                v-model="formData.plannedEndTime"
                placeholder="选择结束时间"
                format="HH:mm"
                value-format="HH:mm"
                style="width: 100%"
                @change="calculateDuration"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="预估时长(分钟)" prop="estimatedDuration">
              <el-input-number
                v-model="formData.estimatedDuration"
                :min="1"
                :max="480"
                :step="15"
                style="width: 100%"
                :disabled="true"
                placeholder="将根据开始和结束时间自动计算"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="手术描述" prop="surgeryDescription">
          <el-input 
            v-model="formData.surgeryDescription" 
            type="textarea"
            :rows="4"
            placeholder="请详细描述手术内容和要求"
          />
        </el-form-item>

        <el-form-item label="术前备注" prop="preSurgeryNotes">
          <el-input 
            v-model="formData.preSurgeryNotes" 
            type="textarea"
            :rows="3"
            placeholder="术前准备事项、特殊要求等"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSubmit">
            {{ saving ? '保存中...' : '保存' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const store = useStore()

const formRef = ref()
const saving = ref(false)

const isEdit = computed(() => !!route.params.id)
const isFromPatientDetail = computed(() => !!route.query.patientId)
const selectedPatientName = computed(() => {
  const selectedPatient = patientOptions.value.find(p => p.id == formData.patientId)
  return selectedPatient ? selectedPatient.user?.realName : ''
})

const formData = reactive({
  patientId: '',
  surgeryName: '',
  surgeryType: '',
  roomId: '',
  bedId: '',
  anesthesiologistId: '',
  nurseId: '',
  plannedDate: '',
  plannedStartTime: '',
  plannedEndTime: '',
  estimatedDuration: 0,
  priorityLevel: 'NORMAL',
  surgeryDescription: '',
  preSurgeryNotes: ''
})

const patientOptions = ref([])
const roomOptions = ref([])
const anesthesiologistOptions = ref([])
const nurseOptions = ref([])
const bedOptions = ref([])

const rules = {
  patientId: [
    { required: true, message: '请选择病人', trigger: 'change' }
  ],
  surgeryName: [
    { required: true, message: '请输入手术名称', trigger: 'blur' }
  ],
  surgeryType: [
    { required: true, message: '请输入手术类型', trigger: 'blur' }
  ],
  roomId: [
    { required: true, message: '请选择手术室', trigger: 'change' }
  ],
  bedId: [
    { required: true, message: '请选择床位', trigger: 'change' }
  ],
  plannedDate: [
    { required: true, message: '请选择手术日期', trigger: 'change' }
  ],
  plannedStartTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  plannedEndTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ],
  estimatedDuration: [
    { required: true, message: '预估时长不能为空', trigger: 'change' },
    { type: 'number', min: 1, max: 480, message: '时长必须在1-480分钟之间', trigger: 'change' }
  ],
  priorityLevel: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ],
  surgeryDescription: [
    { required: true, message: '请输入手术描述', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadOptions()
  if (isEdit.value) {
    loadAppointmentData()
  } else {
    // 检查URL参数中是否有病人ID，如果有则自动填充
    const patientIdFromUrl = route.query.patientId
    if (patientIdFromUrl) {
      formData.patientId = patientIdFromUrl
    }
  }
})

const loadOptions = async () => {
  try {
    const response = await axios.get('/api/appointments/create-options')
    const data = response.data
    
    patientOptions.value = data.patients || []
    roomOptions.value = data.rooms || []
    anesthesiologistOptions.value = data.anesthesiologists || []
    nurseOptions.value = data.nurses || []
    bedOptions.value = data.beds || []
  } catch (error) {
    console.error('加载选项数据失败:', error)
    ElMessage.error('加载选项数据失败: ' + (error.response?.data?.error || error.message))
  }
}

const loadAppointmentData = async () => {
  try {
    const response = await axios.get(`/api/appointments/${route.params.id}`)
    const appointment = response.data
    
    // 填充表单数据
    Object.assign(formData, {
      patientId: appointment.patient?.id,
      surgeryName: appointment.surgeryName,
      surgeryType: appointment.surgeryType,
      roomId: appointment.room?.id,
      bedId: appointment.bed?.id,
      anesthesiologistId: appointment.anesthesiologist?.id,
      nurseId: appointment.nurse?.id,
      plannedDate: appointment.plannedDate,
      plannedStartTime: appointment.plannedStartTime,
      plannedEndTime: appointment.plannedEndTime,
      estimatedDuration: appointment.estimatedDuration,
      priorityLevel: appointment.priorityLevel,
      surgeryDescription: appointment.surgeryDescription,
      preSurgeryNotes: appointment.preSurgeryNotes
    })
  } catch (error) {
    console.error('加载预约数据失败:', error)
    ElMessage.error('加载预约信息失败')
  }
}

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

// 计算预估时长
const calculateDuration = () => {
  if (formData.plannedStartTime && formData.plannedEndTime) {
    // 解析时间字符串，格式为 "HH:mm"
    const [startHour, startMinute] = formData.plannedStartTime.split(':').map(Number)
    const [endHour, endMinute] = formData.plannedEndTime.split(':').map(Number)
    
    // 计算总分钟数
    const startTotalMinutes = startHour * 60 + startMinute
    const endTotalMinutes = endHour * 60 + endMinute
    
    if (endTotalMinutes > startTotalMinutes) {
      formData.estimatedDuration = endTotalMinutes - startTotalMinutes
    } else {
      formData.estimatedDuration = 0
      ElMessage.warning('结束时间必须晚于开始时间')
    }
  } else {
    formData.estimatedDuration = 0
  }
}

// 验证时间
const validateDateTime = () => {
  if (!formData.plannedDate || !formData.plannedStartTime) {
    return true // 让表单验证处理必填项
  }
  
  const now = dayjs()
  const appointmentDateTime = dayjs(`${formData.plannedDate} ${formData.plannedStartTime}`)
  
  if (appointmentDateTime.isBefore(now)) {
    ElMessage.error('手术时间不能早于当前时间')
    return false
  }
  
  if (formData.plannedEndTime) {
    const startTime = dayjs(`2000-01-01 ${formData.plannedStartTime}`)
    const endTime = dayjs(`2000-01-01 ${formData.plannedEndTime}`)
    
    if (!endTime.isAfter(startTime)) {
      ElMessage.error('结束时间必须晚于开始时间')
      return false
    }
  }
  
  return true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
    
    // 验证时间
    if (!validateDateTime()) return
    
    // 验证预估时长
    if (formData.estimatedDuration <= 0) {
      ElMessage.error('请设置正确的开始和结束时间')
      return
    }
    
    saving.value = true
    
    // 构造提交数据
    const submitData = {
      patient: { id: formData.patientId },
      doctor: { id: store.getters['auth/user'].id }, // 当前医生作为主刀医生
      anesthesiologist: formData.anesthesiologistId ? { id: formData.anesthesiologistId } : null,
      nurse: formData.nurseId ? { id: formData.nurseId } : null,
      room: { id: formData.roomId },
      bed: { id: formData.bedId },
      surgeryName: formData.surgeryName,
      surgeryType: formData.surgeryType,
      plannedDate: formData.plannedDate,
      plannedStartTime: formData.plannedStartTime,
      plannedEndTime: formData.plannedEndTime,
      estimatedDuration: formData.estimatedDuration,
      priorityLevel: formData.priorityLevel,
      surgeryDescription: formData.surgeryDescription,
      preSurgeryNotes: formData.preSurgeryNotes
    }
    
    if (isEdit.value) {
      await axios.put(`/api/appointments/${route.params.id}`, submitData)
      ElMessage.success('修改成功')
    } else {
      await axios.post('/api/appointments', submitData)
      ElMessage.success('创建成功')
    }
    
    router.push('/appointments')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.response?.data?.error || error.message))
  } finally {
    saving.value = false
  }
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

const handleRoomChange = async () => {
  // 清空床位选择
  formData.bedId = ''
  bedOptions.value = []
  
  if (formData.roomId) {
    try {
      // 获取选中手术室的可用床位
      const response = await axios.get(`/api/appointments/rooms/${formData.roomId}/beds`)
      bedOptions.value = response.data || []
    } catch (error) {
      console.error('获取床位信息失败:', error)
      ElMessage.error('获取床位信息失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.appointment-form-container {
  .page-header {
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
    }
  }
  
  .el-input-number {
    width: 100%;
  }
  
  .patient-info-tip {
    margin-top: 8px;
    
    :deep(.el-alert) {
      padding: 8px 12px;
      
      .el-alert__title {
        font-size: 13px;
      }
    }
  }
}
</style> 