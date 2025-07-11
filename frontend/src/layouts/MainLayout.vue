<template>
  <div class="main-layout">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '250px'" class="sidebar">
        <div class="sidebar-header">
          <div v-if="!isCollapse" class="logo">
            <el-icon class="logo-icon"><Operation /></el-icon>
            <span class="logo-text">手术管理系统</span>
          </div>
          <el-icon v-else class="logo-icon-collapsed"><Operation /></el-icon>
        </div>
        
        <el-menu
          :default-active="$route.path"
          :collapse="isCollapse"
          :unique-opened="true"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          
          <el-sub-menu v-if="canAccessPatients" index="patients">
            <template #title>
              <el-icon><User /></el-icon>
              <span>病人管理</span>
            </template>
            <el-menu-item index="/patients">病人列表</el-menu-item>
            <el-menu-item v-if="isPatient" index="/patients/my-info">我的病历</el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu v-if="canAccessAppointments" index="appointments">
            <template #title>
              <el-icon><Calendar /></el-icon>
              <span>手术预约</span>
            </template>
            <el-menu-item index="/appointments">预约列表</el-menu-item>
            <el-menu-item index="/appointments/calendar">手术日历</el-menu-item>
            <el-menu-item v-if="isDoctor" index="/appointments/create">创建预约</el-menu-item>
          </el-sub-menu>
          
                    <el-menu-item v-if="isDoctor" index="/schedule">
            <el-icon><Clock /></el-icon>
            <span>排班管理</span>
          </el-menu-item>
          
          <el-menu-item v-if="isDoctor || isNurse || isAnesthesiologist || isPatient" index="/notifications">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </el-menu-item>
            
          <el-menu-item v-if="isDoctor || isAdmin" index="/rooms">
            <el-icon><OfficeBuilding /></el-icon>
            <span>手术室管理</span>
          </el-menu-item>
          
          <!-- 删除手术汇总菜单项 -->
          
          <el-menu-item v-if="isAdmin" index="/user-management">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          
          <el-menu-item index="/profile">
            <el-icon><Setting /></el-icon>
            <span>个人设置</span>
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
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentPageTitle">{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="header-right">
            <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
              <el-button text circle @click="goToNotifications">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
            
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
                    系统设置
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
          <transition name="fade-transform" mode="out-in">
            <router-view />
          </transition>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Operation, Monitor, User, Calendar, Clock, OfficeBuilding, Setting,
  Expand, Fold, Bell, ArrowDown, SwitchButton
} from '@element-plus/icons-vue'
import api from '@/api'

const store = useStore()
const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)
const notificationCount = ref(0)
let notificationTimer = null

const user = computed(() => store.getters['auth/user'])
const isDoctor = computed(() => store.getters['auth/isDoctor'])
const isPatient = computed(() => store.getters['auth/isPatient'])
const isNurse = computed(() => store.getters['auth/isNurse'])
const isAnesthesiologist = computed(() => store.getters['auth/isAnesthesiologist'])
const isAdmin = computed(() => store.getters['auth/isAdmin'])

const canAccessPatients = computed(() => {
  return isPatient.value || isDoctor.value
})

const canAccessAppointments = computed(() => {
  return isDoctor.value || isPatient.value
})

const currentPageTitle = computed(() => {
  return route.meta?.title || ''
})

const userAvatar = computed(() => {
  // 这里可以返回用户头像URL
  return null
})

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleUserAction = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中...')
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

const goToNotifications = () => {
  router.push('/notifications')
}

const loadNotificationCount = async () => {
  try {
    const response = await api.get('/api/notifications/unread-count')
    notificationCount.value = response.data.count || 0
  } catch (error) {
    console.error('加载通知数量失败:', error)
  }
}

onMounted(() => {
  // 立即加载通知数量
  loadNotificationCount()
  
  // 每30秒刷新一次通知数量
  notificationTimer = setInterval(loadNotificationCount, 30000)
})

onUnmounted(() => {
  if (notificationTimer) {
    clearInterval(notificationTimer)
  }
})
</script>

<style lang="scss" scoped>
.main-layout {
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
        color: #1890ff;
      }
      
      .logo-text {
        font-size: 16px;
        font-weight: 600;
      }
    }
    
    .logo-icon-collapsed {
      font-size: 24px;
      color: #1890ff;
    }
  }
  
  .sidebar-menu {
    border-right: none;
    background: #001529;
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: rgba(255, 255, 255, 0.65);
      
      &:hover {
        background-color: #1890ff !important;
        color: white;
      }
      
      &.is-active {
        background-color: #1890ff !important;
        color: white;
      }
    }
    
    :deep(.el-sub-menu .el-menu-item) {
      background-color: #000c17;
      
      &:hover {
        background-color: #1890ff !important;
      }
      
      &.is-active {
        background-color: #1890ff !important;
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
  padding: 24px;
  overflow-y: auto;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style> 