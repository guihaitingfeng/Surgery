<template>
  <div class="admin-login-container">
    <div class="admin-login-card">
      <div class="admin-header">
        <div class="admin-logo">
          <el-icon class="logo-icon"><Lock /></el-icon>
        </div>
        <h1>管理员登录</h1>
        <p class="admin-subtitle">手术预约与排班管理系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="rules"
        class="admin-login-form"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="管理员账号" prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入管理员账号"
            size="large"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="admin-login-btn"
          >
            {{ loading ? '登录中...' : '管理员登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="admin-footer">
        <el-divider>
          <span class="divider-text">其他登录方式</span>
        </el-divider>
        <div class="other-login">
          <el-button text @click="$router.push('/login')">
            <el-icon><User /></el-icon>
            普通用户登录
          </el-button>
        </div>
      </div>
    </div>

    <!-- 管理员信息提示 -->
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const store = useStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return

    loading.value = true

    // 使用临时的管理员登录端点
    const response = await axios.post('/api/auth/admin-login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (response.data) {
      const { token, user } = response.data
      
      // 设置认证信息
      store.commit('auth/SET_TOKEN', token)
      store.commit('auth/SET_USER', user)
      
      ElMessage.success('管理员登录成功')
      
      // 跳转到管理员仪表盘
      router.push('/dashboard')
    }
  } catch (error) {
    console.error('登录错误:', error)
    const errorMessage = error.response?.data?.error || '登录失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.admin-login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;

  .admin-login-card {
    background: white;
    border-radius: 16px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
    padding: 40px;
    width: 100%;
    max-width: 400px;
    position: relative;
    z-index: 2;

    .admin-header {
      text-align: center;
      margin-bottom: 30px;

      .admin-logo {
        margin-bottom: 16px;
        
        .logo-icon {
          font-size: 48px;
          color: #667eea;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }
      }

      h1 {
        font-size: 28px;
        font-weight: 700;
        color: #2c3e50;
        margin-bottom: 8px;
      }

      .admin-subtitle {
        color: #7f8c8d;
        font-size: 14px;
        margin: 0;
      }
    }

    .admin-login-form {
      .admin-login-btn {
        width: 100%;
        height: 48px;
        font-size: 16px;
        font-weight: 600;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
        
        &:hover {
          background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
        }
      }
    }

    .admin-footer {
      margin-top: 24px;

      .divider-text {
        color: #95a5a6;
        font-size: 12px;
      }

      .other-login {
        text-align: center;
        margin-top: 16px;

        .el-button {
          color: #667eea;
          
          &:hover {
            color: #764ba2;
          }
        }
      }
    }
  }

  .admin-info {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 280px;
    z-index: 1;

    .info-card {
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(10px);

      .card-header {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #e74c3c;
        font-weight: 600;
      }

      .info-content {
        p {
          margin: 8px 0;
          font-size: 14px;
          
          &.warning {
            color: #e74c3c;
            font-weight: 600;
            margin-top: 12px;
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .admin-login-container {
    .admin-info {
      position: static;
      width: 100%;
      max-width: 400px;
      margin-top: 20px;
    }
  }
}
</style> 