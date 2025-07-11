<template>
  <div class="patient-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '250px'" class="sidebar">
        <div class="sidebar-header">
          <div v-if="!isCollapse" class="logo">
            <el-icon class="logo-icon"><FirstAidKit /></el-icon>
            <span class="logo-text">病人服务中心</span>
          </div>
          <el-icon v-else class="logo-icon-collapsed"><FirstAidKit /></el-icon>
        </div>
        
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          class="sidebar-menu"
        >
          <el-menu-item index="/patient-dashboard" @click="handleMenuClick('/patient-dashboard')">
            <el-icon><HomeFilled /></el-icon>
            <span>我的主页</span>
          </el-menu-item>
          
          <el-menu-item index="/patient-dashboard/info" @click="handleMenuClick('/patient-dashboard/info')">
            <el-icon><Document /></el-icon>
            <span>病情信息</span>
          </el-menu-item>
          
          <el-menu-item index="/patient-dashboard/appointments" @click="handleMenuClick('/patient-dashboard/appointments')">
            <el-icon><Calendar /></el-icon>
            <span>手术预约</span>
          </el-menu-item>
          
          <el-menu-item index="/patient-dashboard/notifications" @click="handleMenuClick('/patient-dashboard/notifications')">
            <el-icon><Bell /></el-icon>
            <span>通知消息</span>
            <el-badge v-if="unreadCount > 0 && !isCollapse" :value="unreadCount" class="menu-badge" />
          </el-menu-item>
          
          <el-menu-item index="/patient-profile" @click="handleMenuClick('/patient-profile')">
            <el-icon><User /></el-icon>
            <span>个人资料</span>
          </el-menu-item>
          
          <el-menu-item index="/patient-dashboard/help" @click="handleMenuClick('/patient-dashboard/help')">
            <el-icon><QuestionFilled /></el-icon>
            <span>帮助中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部导航 -->
        <el-header class="header">
          <div class="header-left">
            <el-button
              text
              @click="toggleSidebar"
              class="collapse-btn"
            >
              <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
            </el-button>
            
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/patient-dashboard' }">病人中心</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentPageTitle">{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="header-right">
            <!-- 账户状态指示器 -->
            <div v-if="isBlacklisted" class="status-indicator blacklisted">
              <el-icon><WarningFilled /></el-icon>
              <span>账户受限</span>
            </div>
            
            <!-- 快捷操作 -->
            <el-tooltip content="提交病情信息" placement="bottom">
              <el-button 
                v-if="!hasPatientInfo"
                type="primary" 
                size="small"
                circle
                @click="showSubmitDialog"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
            </el-tooltip>
            
            <!-- 通知铃铛 -->
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button 
                :icon="Bell" 
                circle 
                @click="router.push('/patient-dashboard/notifications')"
              />
            </el-badge>
            
            <!-- 用户下拉菜单 -->
            <el-dropdown @command="handleUserAction">
              <div class="user-info">
                <el-avatar :size="32" :src="userAvatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ user?.realName || user?.username }}</span>
                <el-icon class="arrow-down"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人信息
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    账户设置
                  </el-dropdown-item>
                  <el-dropdown-item command="help">
                    <el-icon><QuestionFilled /></el-icon>
                    帮助中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        
        <!-- 主内容 -->
        <el-main class="main-content">
          <slot />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FirstAidKit, HomeFilled, Document, Calendar, Bell, User, QuestionFilled,
  Expand, Fold, ArrowDown, SwitchButton, Setting, Plus, WarningFilled
} from '@element-plus/icons-vue'

const props = defineProps({
  unreadCount: {
    type: Number,
    default: 0
  },
  hasPatientInfo: {
    type: Boolean,
    default: false
  },
  isBlacklisted: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['show-submit-dialog'])

const store = useStore()
const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)

const user = computed(() => store.getters['auth/user'])
const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => route.meta?.title || '')
const userAvatar = computed(() => null)

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const goToNotifications = () => {
  ElMessage.info('通知中心功能开发中...')
}

const showSubmitDialog = () => {
  emit('show-submit-dialog')
}

const handleMenuClick = (path) => {
  if (path.startsWith('http')) {
    window.open(path, '_blank')
  } else {
    router.push(path)
  }
}

const handleUserAction = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/patient-profile')
      break
    case 'settings':
      ElMessage.info('账户设置功能开发中...')
      break
    case 'help':
      router.push('/patient-dashboard/help')
      break
    case 'logout':
      await handleLogout()
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    store.dispatch('auth/logout')
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 取消操作
  }
}
</script>

<style lang="scss" scoped>
.patient-layout {
  height: 100vh;
  
  .el-container {
    height: 100%;
  }
}

.sidebar {
  background: #001529;
  transition: width 0.3s ease;
  overflow: hidden;
  
  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid #1f1f1f;
    
    .logo {
      display: flex;
      align-items: center;
      color: white;
      
      .logo-icon {
        font-size: 24px;
        margin-right: 12px;
        color: #67c23a;
      }
      
      .logo-text {
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .logo-icon-collapsed {
      font-size: 24px;
      color: #67c23a;
    }
  }
  
  .sidebar-menu {
    border-right: none;
    background: #001529;
    
    .menu-badge {
      position: absolute;
      right: 16px;
      top: 50%;
      transform: translateY(-50%);
    }
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: rgba(255, 255, 255, 0.65);
      position: relative;
      
      &:hover {
        background-color: #1890ff !important;
        color: white;
      }
      
      &.is-active {
        background-color: #1890ff !important;
        color: white;
      }
    }
  }
}

.header {
  background: white;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  
  .header-left {
    display: flex;
    align-items: center;
    
    .collapse-btn {
      margin-right: 24px;
      font-size: 18px;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .status-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      
      &.blacklisted {
        background-color: #fef0f0;
        color: #f56c6c;
        
        .el-icon {
          font-size: 16px;
        }
      }
    }
    
    .notification-badge {
      .el-button {
        font-size: 18px;
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 8px 12px;
      border-radius: 6px;
      transition: background-color 0.3s ease;
      
      &:hover {
        background-color: #f5f5f5;
      }
      
      .username {
        font-size: 14px;
        color: #333;
      }
      
      .arrow-down {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

.main-content {
  background: #f5f5f5;
  padding: 0;
  overflow-y: auto;
}
</style> 