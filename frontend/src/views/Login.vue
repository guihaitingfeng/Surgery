<template>
  <div class="login-container">
    <div class="login-background">
      <div class="medical-icons">
        <el-icon class="icon-1"><Operation /></el-icon>
        <el-icon class="icon-2"><UserFilled /></el-icon>
        <el-icon class="icon-3"><Calendar /></el-icon>
        <el-icon class="icon-4"><OfficeBuilding /></el-icon>
      </div>
    </div>
    
    <div class="login-card">
      <div class="login-header">
        <h1>手术预约与排班管理系统</h1>
        <p class="subtitle">Surgery Reservation & Scheduling System</p>
      </div>
      
      <el-form
        ref="loginForm"
        :model="loginData"
        :rules="rules"
        class="login-form"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="loginData.username"
            prefix-icon="User"
            placeholder="请输入用户名"
            size="large"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginData.password"
            type="password"
            prefix-icon="Lock"
            placeholder="请输入密码"
            size="large"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            <el-icon v-if="!loading"><Right /></el-icon>
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>

        <div class="admin-login-entry">
          <el-divider>
            <span class="divider-text">系统管理</span>
          </el-divider>
          <el-button 
            text 
            class="admin-login-btn"
            @click="$router.push('/admin-login')"
          >
            <el-icon><Lock /></el-icon>
            管理员登录
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Operation, UserFilled, Calendar, OfficeBuilding, User, Lock, Right } from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()

const loginForm = ref()
const loading = ref(false)

const loginData = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginForm.value) return
  
  try {
    const valid = await loginForm.value.validate()
    if (!valid) return
    
    loading.value = true
    const result = await store.dispatch('auth/login', loginData)
    
    if (result.success) {
      ElMessage.success('登录成功')
      
      // 等待一小段时间确保token已经设置到localStorage和axios中
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // 根据用户角色跳转到不同的首页
      const userRole = result.data.user.role
      switch (userRole) {
        case 'PATIENT':
          router.push('/patient-dashboard') // 病人Dashboard - 显示我的预约
          break
        case 'DOCTOR':
          router.push('/dashboard') // 医生Dashboard - 显示今日手术
          break
        case 'NURSE':
        case 'ANESTHESIOLOGIST':
          router.push('/dashboard') // 护士/麻醉师Dashboard - 显示待确认预约
          break
        case 'ADMIN':
          router.push('/dashboard') // 管理员Dashboard - 显示系统统计
          break
        default:
          router.push('/dashboard')
      }
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    ElMessage.error('登录失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-background {
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

.login-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 40px;
  width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  h1 {
    color: #2c3e50;
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .subtitle {
    color: #7f8c8d;
    font-size: 14px;
    font-style: italic;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
    
    :deep(.el-form-item__label) {
      color: #2c3e50;
      font-weight: 500;
      margin-bottom: 8px;
    }
    
    :deep(.el-input__wrapper) {
      border-radius: 10px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border: 1px solid #e1e8ed;
      transition: all 0.3s ease;
      
      &:hover {
        border-color: #667eea;
      }
      
      &.is-focus {
        border-color: #667eea;
        box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
      }
    }
  }
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  color: #7f8c8d;
  font-size: 14px;
  
  .register-link {
    color: #667eea;
    text-decoration: none;
    font-weight: 500;
    margin-left: 8px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.admin-login-entry {
  text-align: center;
  margin-top: 20px;
  color: #7f8c8d;
  font-size: 14px;
  
  .admin-login-btn {
    color: #667eea;
    text-decoration: none;
    font-weight: 500;
    margin-left: 8px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}
</style> 