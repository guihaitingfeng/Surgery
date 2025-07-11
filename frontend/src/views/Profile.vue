<template>
  <div class="profile-container">
    <div class="profile-header">
      <div class="profile-avatar">
        <el-avatar :size="120">
          <el-icon><UserFilled /></el-icon>
        </el-avatar>
      </div>
      <div class="profile-info">
        <h2>{{ user?.realName || user?.username }}</h2>
        <p class="role-badge">{{ getRoleText(user?.role) }}</p>
        <p class="department">{{ user?.department || '暂无科室信息' }}</p>
      </div>
    </div>

    <el-row :gutter="24">
      <el-col :span="24">
        <div class="card">
          <div class="card-header">
            <h3>基本信息</h3>
          </div>
          <div class="card-content">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户名">
                {{ user?.username }}
              </el-descriptions-item>
              <el-descriptions-item label="真实姓名">
                {{ user?.realName }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                {{ user?.email }}
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                {{ user?.phone }}
              </el-descriptions-item>
              <el-descriptions-item label="性别">
                {{ getGenderText(user?.gender) }}
              </el-descriptions-item>
              <el-descriptions-item label="出生日期">
                {{ user?.birthDate }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { UserFilled } from '@element-plus/icons-vue'

const store = useStore()
const user = computed(() => store.getters['auth/user'])

const getRoleText = (role) => {
  const roleMap = {
    'PATIENT': '病人',
    'DOCTOR': '医生',
    'NURSE': '护士',
    'ANESTHESIOLOGIST': '麻醉师',
    'ADMIN': '管理员'
  }
  return roleMap[role] || role
}

const getGenderText = (gender) => {
  const genderMap = {
    'MALE': '男',
    'FEMALE': '女'
  }
  return genderMap[gender] || gender
}
</script>

<style lang="scss" scoped>
.profile-container {
  .profile-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 40px;
    margin-bottom: 24px;
    color: white;
    display: flex;
    align-items: center;
    gap: 24px;
    
    .profile-info {
      h2 {
        font-size: 28px;
        font-weight: 600;
        margin-bottom: 8px;
      }
      
      .role-badge {
        background: rgba(255, 255, 255, 0.2);
        padding: 4px 12px;
        border-radius: 16px;
        font-size: 14px;
        display: inline-block;
        margin-bottom: 8px;
      }
      
      .department {
        font-size: 16px;
        opacity: 0.9;
      }
    }
  }
}
</style> 