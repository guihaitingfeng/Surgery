<template>
  <div class="register-container">
    <div class="register-background">
      <div class="medical-icons">
        <el-icon class="icon-1"><Operation /></el-icon>
        <el-icon class="icon-2"><UserFilled /></el-icon>
        <el-icon class="icon-3"><Calendar /></el-icon>
        <el-icon class="icon-4"><OfficeBuilding /></el-icon>
      </div>
    </div>
    
    <div class="register-card">
      <div class="register-header">
        <h1>用户注册</h1>
        <p class="subtitle">加入手术预约与排班管理系统</p>
      </div>
      
      <el-form
        ref="registerForm"
        :model="registerData"
        :rules="rules"
        class="register-form"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerData.username"
                prefix-icon="User"
                placeholder="请输入用户名"
                size="large"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input
                v-model="registerData.realName"
                prefix-icon="Avatar"
                placeholder="请输入真实姓名"
                size="large"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="registerData.email"
            type="email"
            prefix-icon="Message"
            placeholder="请输入邮箱地址"
            size="large"
            clearable
          />
        </el-form-item>
        
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerData.password"
                type="password"
                prefix-icon="Lock"
                placeholder="请输入密码"
                size="large"
                show-password
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerData.confirmPassword"
                type="password"
                prefix-icon="Lock"
                placeholder="请再次输入密码"
                size="large"
                show-password
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="registerData.phone"
                prefix-icon="Phone"
                placeholder="请输入手机号"
                size="large"
                clearable
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select
                v-model="registerData.gender"
                placeholder="请选择性别"
                size="large"
                style="width: 100%"
              >
                <el-option label="男" value="MALE" />
                <el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="registerData.birthDate"
                type="date"
                placeholder="选择出生日期"
                size="large"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户角色" prop="role">
              <el-select
                v-model="registerData.role"
                placeholder="请选择角色"
                size="large"
                style="width: 100%"
              >
                <el-option label="病人" value="PATIENT" />
                <el-option label="医生" value="DOCTOR" />
                <el-option label="护士" value="NURSE" />
                <el-option label="麻醉师" value="ANESTHESIOLOGIST" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 专业信息（医护人员） -->
        <div v-if="isProfessional" class="professional-info">
          <el-divider content-position="left">专业信息</el-divider>
          
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="科室" prop="department">
                <el-input
                  v-model="registerData.department"
                  placeholder="请输入所属科室"
                  size="large"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职称" prop="professionalTitle">
                <el-input
                  v-model="registerData.professionalTitle"
                  placeholder="请输入职称"
                  size="large"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="执业证号" prop="licenseNumber">
            <el-input
              v-model="registerData.licenseNumber"
              placeholder="请输入执业证号"
              size="large"
              clearable
            />
          </el-form-item>
        </div>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="register-button"
            :loading="loading"
            @click="handleRegister"
          >
            <el-icon v-if="!loading"><Right /></el-icon>
            {{ loading ? '注册中...' : '立即注册' }}
          </el-button>
        </el-form-item>
        
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Operation, UserFilled, Calendar, OfficeBuilding, Right } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

const registerForm = ref()
const loading = ref(false)

const registerData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  realName: '',
  phone: '',
  role: '',
  gender: '',
  birthDate: '',
  department: '',
  professionalTitle: '',
  licenseNumber: ''
})

// 计算属性：是否为专业人员
const isProfessional = computed(() => {
  return ['DOCTOR', 'NURSE', 'ANESTHESIOLOGIST'].includes(registerData.role)
})

// 自定义验证函数
const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度至少6个字符'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerData.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度应为2-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!registerForm.value) return
  
  try {
    const valid = await registerForm.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 构造请求数据
    const requestData = { ...registerData }
    delete requestData.confirmPassword
    
    // 如果不是专业人员，删除专业字段
    if (!isProfessional.value) {
      delete requestData.department
      delete requestData.professionalTitle
      delete requestData.licenseNumber
    }
    
    const response = await axios.post('/api/auth/register', requestData)
    
    if (response.data) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.error || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.register-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0.1;
  
  .medical-icons {
    position: relative;
    width: 100%;
    height: 100%;
    
    .el-icon {
      position: absolute;
      font-size: 120px;
      color: white;
      animation: float 6s ease-in-out infinite;
      
      &.icon-1 {
        top: 10%;
        left: 10%;
        animation-delay: 0s;
      }
      
      &.icon-2 {
        top: 20%;
        right: 15%;
        animation-delay: 1.5s;
      }
      
      &.icon-3 {
        bottom: 30%;
        left: 20%;
        animation-delay: 3s;
      }
      
      &.icon-4 {
        bottom: 15%;
        right: 10%;
        animation-delay: 4.5s;
      }
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

.register-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 40px;
  width: 100%;
  max-width: 800px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 10;
  max-height: 90vh;
  overflow-y: auto;
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
  
  h1 {
    font-size: 32px;
    font-weight: 700;
    color: #2c3e50;
    margin: 0 0 8px 0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .subtitle {
    color: #7f8c8d;
    font-size: 16px;
    margin: 0;
  }
}

.register-form {
  .professional-info {
    margin-top: 20px;
    padding: 20px;
    background: rgba(103, 126, 234, 0.05);
    border-radius: 12px;
    border-left: 4px solid #667eea;
  }
}

.register-button {
  width: 100%;
  height: 50px;
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  margin-top: 20px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
  }
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  color: #7f8c8d;
  
  .login-link {
    color: #667eea;
    text-decoration: none;
    font-weight: 600;
    margin-left: 8px;
    
    &:hover {
      color: #764ba2;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .register-card {
    padding: 20px;
    margin: 10px;
  }
  
  .register-header h1 {
    font-size: 24px;
  }
}
</style> 