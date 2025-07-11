<template>
  <div class="patient-form-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑病人信息' : '添加病人' }}</h2>
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
            <el-form-item label="病历号" prop="medicalRecordNumber">
              <el-input 
                v-model="formData.medicalRecordNumber" 
                placeholder="请输入病历号"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input 
                v-model="formData.idCard" 
                placeholder="请输入身份证号"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="紧急联系人" prop="emergencyContact">
              <el-input 
                v-model="formData.emergencyContact" 
                placeholder="请输入紧急联系人"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="emergencyPhone">
              <el-input 
                v-model="formData.emergencyPhone" 
                placeholder="请输入联系电话"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="病情严重程度" prop="severityLevel">
              <el-select 
                v-model="formData.severityLevel" 
                placeholder="请选择病情严重程度"
                style="width: 100%"
              >
                <el-option label="紧急" value="EMERGENCY" />
                <el-option label="急诊" value="URGENT" />
                <el-option label="普通" value="NORMAL" />
                <el-option label="低级" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="住院日期" prop="admissionDate">
              <el-date-picker
                v-model="formData.admissionDate"
                type="date"
                placeholder="选择住院日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="病房号" prop="wardNumber">
              <el-input 
                v-model="formData.wardNumber" 
                placeholder="请输入病房号"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="床位号" prop="bedNumber">
              <el-input 
                v-model="formData.bedNumber" 
                placeholder="请输入床位号"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="病史" prop="medicalHistory">
          <el-input 
            v-model="formData.medicalHistory" 
            type="textarea"
            :rows="3"
            placeholder="请输入病史信息"
          />
        </el-form-item>

        <el-form-item label="过敏史" prop="allergies">
          <el-input 
            v-model="formData.allergies" 
            type="textarea"
            :rows="3"
            placeholder="请输入过敏史信息"
          />
        </el-form-item>

        <el-form-item label="当前用药" prop="currentMedications">
          <el-input 
            v-model="formData.currentMedications" 
            type="textarea"
            :rows="3"
            placeholder="请输入当前用药信息"
          />
        </el-form-item>

        <el-form-item label="病情描述" prop="diseaseDescription">
          <el-input 
            v-model="formData.diseaseDescription" 
            type="textarea"
            :rows="4"
            placeholder="请详细描述病情"
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
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const saving = ref(false)

const isEdit = computed(() => !!route.params.id)

const formData = reactive({
  medicalRecordNumber: '',
  idCard: '',
  emergencyContact: '',
  emergencyPhone: '',
  medicalHistory: '',
  allergies: '',
  currentMedications: '',
  admissionDate: '',
  wardNumber: '',
  bedNumber: '',
  diseaseDescription: '',
  severityLevel: ''
})

const rules = {
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
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  severityLevel: [
    { required: true, message: '请选择病情严重程度', trigger: 'change' }
  ]
}

onMounted(() => {
  if (isEdit.value) {
    loadPatientData()
  }
})

const loadPatientData = async () => {
  try {
    const response = await axios.get(`/api/patients/${route.params.id}`)
    const patient = response.data
    
    // 填充表单数据
    Object.keys(formData).forEach(key => {
      if (patient[key] !== undefined) {
        formData[key] = patient[key]
      }
    })
  } catch (error) {
    console.error('加载病人信息失败:', error)
    ElMessage.error('加载病人信息失败: ' + (error.response?.data?.error || error.message))
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
    
    saving.value = true
    
    if (isEdit.value) {
      // 编辑模式
      await axios.put(`/api/patients/${route.params.id}`, formData)
      ElMessage.success('修改成功')
    } else {
      // 新增模式 - 注意：医生手动添加病人可能需要不同的API
      await axios.post('/api/patients', formData)
      ElMessage.success('添加成功')
    }
    
    router.push('/patients')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.response?.data?.error || error.message))
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.patient-form-container {
  .page-header {
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
    }
  }
}
</style> 