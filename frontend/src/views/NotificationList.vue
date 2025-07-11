<template>
  <div class="notification-list">
    <div class="page-header">
      <h1>消息通知</h1>
      <div class="header-actions">
        <el-button @click="markAllAsRead" :disabled="unreadCount === 0">
          <el-icon><Check /></el-icon>
          全部标记为已读
        </el-button>
        <el-button @click="loadNotifications" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="notification-stats">
      <el-card shadow="never">
        <div class="stats-content">
          <div class="stat-item">
            <span class="stat-label">总通知数</span>
            <span class="stat-value">{{ totalCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">未读通知</span>
            <span class="stat-value unread">{{ unreadCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">今日通知</span>
            <span class="stat-value">{{ todayCount }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 筛选器 -->
    <div class="notification-filters">
      <el-card shadow="never">
        <div class="filter-content">
          <div class="filter-buttons">
            <el-radio-group v-model="filterType" @change="loadNotifications">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="unread">未读</el-radio-button>
              <el-radio-button label="read">已读</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="filter-select">
            <el-select v-model="filterNotificationType" placeholder="通知类型" clearable @change="loadNotifications">
              <el-option label="手术安排" value="SURGERY_SCHEDULED" />
              <el-option label="手术确认" value="SURGERY_CONFIRMED" />
              <el-option label="团队确认请求" value="TEAM_CONFIRMATION_REQUEST" />
              <el-option label="医生最终确认请求" value="DOCTOR_FINAL_CONFIRMATION_REQUEST" />
              <el-option label="手术通知单" value="PATIENT_SURGERY_NOTICE" />
              <el-option label="手术更新" value="SURGERY_UPDATE" />
              <el-option label="手术完成" value="SURGERY_COMPLETE" />
              <el-option label="手术取消" value="SURGERY_CANCELLED" />
            </el-select>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 通知列表 -->
    <div class="notification-content">
      <el-card shadow="never" v-loading="loading">
        <div v-if="notifications.length === 0" class="empty-state">
          <el-empty description="暂无通知" />
        </div>
        
        <div v-else class="notification-items">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            :class="[
              'notification-item',
              { 'unread': !notification.isRead }
            ]"
            @click="viewNotification(notification)"
          >
            <div class="notification-icon">
              <el-icon :class="getNotificationIconClass(notification.type)">
                <Bell />
              </el-icon>
            </div>
            
            <div class="notification-content-main">
              <div class="notification-header">
                <h3 class="notification-title">{{ notification.title }}</h3>
                <div class="notification-meta">
                  <span class="notification-type">{{ getNotificationTypeText(notification.type) }}</span>
                  <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                </div>
              </div>
              
              <div class="notification-preview">
                {{ getContentPreview(notification.content) }}
              </div>
              
              <div v-if="notification.relatedAppointment" class="notification-related">
                <el-tag size="small" type="info">
                  相关手术：{{ notification.relatedAppointment.surgeryName }}
                </el-tag>
              </div>
            </div>
            
            <div class="notification-actions">
              <el-button
                v-if="!notification.isRead"
                size="small"
                type="primary"
                text
                @click.stop="markAsRead(notification.id)"
              >
                标记已读
              </el-button>
              
              <el-button
                v-if="canConfirm(notification)"
                size="small"
                type="success"
                @click.stop="handleConfirm(notification)"
              >
                确认
              </el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalCount > 0" class="pagination-wrapper">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :total="totalCount"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh, Bell } from '@element-plus/icons-vue'
import api from '@/api'

export default {
  name: 'NotificationList',
  components: {
    Check, Refresh, Bell
  },
  setup() {
    const router = useRouter()
    const store = useStore()
    
    const loading = ref(false)
    const notifications = ref([])
    const currentPage = ref(1)
    const pageSize = ref(20)
    const totalCount = ref(0)
    const unreadCount = ref(0)
    const todayCount = ref(0)
    const filterType = ref('all')
    const filterNotificationType = ref('')

    const user = computed(() => store.getters['auth/user'])

    const loadNotifications = async () => {
      try {
        loading.value = true
        const params = {
          page: currentPage.value - 1,
          size: pageSize.value
        }

        let url = '/api/notifications'
        if (filterType.value === 'unread') {
          url = '/api/notifications/unread'
          const response = await api.get(url)
          notifications.value = response.data
          totalCount.value = response.data.length
        } else {
          const response = await api.get(url, { params })
          notifications.value = response.data.content || response.data
          totalCount.value = response.data.totalElements || response.data.length
        }

        await loadStats()
      } catch (error) {
        console.error('加载通知失败:', error)
        ElMessage.error('加载通知失败')
      } finally {
        loading.value = false
      }
    }

    const loadStats = async () => {
      try {
        const [unreadResponse] = await Promise.all([
          api.get('/api/notifications/unread-count')
        ])
        
        unreadCount.value = unreadResponse.data.count || 0
        
        const today = new Date().toDateString()
        todayCount.value = notifications.value.filter(n => 
          new Date(n.createdAt).toDateString() === today
        ).length
      } catch (error) {
        console.error('加载统计数据失败:', error)
      }
    }

    const viewNotification = (notification) => {
      router.push(`/notifications/${notification.id}`)
    }

    const markAsRead = async (notificationId) => {
      try {
        await api.put(`/api/notifications/${notificationId}/read`)
        ElMessage.success('已标记为已读')
        loadNotifications()
      } catch (error) {
        console.error('标记已读失败:', error)
        ElMessage.error('标记已读失败')
      }
    }

    const markAllAsRead = async () => {
      try {
        await ElMessageBox.confirm('确定要将所有未读通知标记为已读吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        const unreadNotifications = notifications.value.filter(n => !n.isRead)
        await Promise.all(
          unreadNotifications.map(n => api.put(`/api/notifications/${n.id}/read`))
        )
        
        ElMessage.success('所有通知已标记为已读')
        loadNotifications()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('批量标记已读失败:', error)
          ElMessage.error('批量标记已读失败')
        }
      }
    }

    const canConfirm = (notification) => {
      if (!notification.relatedAppointment) return false
      
      const appointment = notification.relatedAppointment
      const isTeamMember = (
        (appointment.anesthesiologist && appointment.anesthesiologist.id === user.value.id) ||
        (appointment.nurse && appointment.nurse.id === user.value.id)
      )
      
      return (
        notification.type === 'TEAM_CONFIRMATION_REQUEST' &&
        appointment.status === 'PENDING_CONFIRMATION' &&
        isTeamMember
      ) || (
        notification.type === 'DOCTOR_FINAL_CONFIRMATION_REQUEST' &&
        appointment.status === 'TEAM_CONFIRMED' &&
        appointment.doctor.id === user.value.id
      )
    }

    const handleConfirm = async (notification) => {
      try {
        const appointment = notification.relatedAppointment
        
        if (notification.type === 'TEAM_CONFIRMATION_REQUEST') {
          await api.post(`/api/appointments/${appointment.id}/confirm-by-team`)
          ElMessage.success('确认成功')
        } else if (notification.type === 'DOCTOR_FINAL_CONFIRMATION_REQUEST') {
          await api.post(`/api/appointments/${appointment.id}/doctor-final-confirm`)
          ElMessage.success('最终确认成功')
        }
        
        loadNotifications()
      } catch (error) {
        console.error('确认失败:', error)
        ElMessage.error(error.response?.data?.error || '确认失败')
      }
    }

    const getNotificationIconClass = (type) => {
      const classMap = {
        'SURGERY_SCHEDULED': 'icon-blue',
        'SURGERY_CONFIRMED': 'icon-green',
        'TEAM_CONFIRMATION_REQUEST': 'icon-orange',
        'DOCTOR_FINAL_CONFIRMATION_REQUEST': 'icon-purple',
        'PATIENT_SURGERY_NOTICE': 'icon-blue',
        'SURGERY_UPDATE': 'icon-gray',
        'SURGERY_COMPLETE': 'icon-green',
        'SURGERY_CANCELLED': 'icon-red'
      }
      return classMap[type] || 'icon-gray'
    }

    const getNotificationTypeText = (type) => {
      const typeMap = {
        'SURGERY_SCHEDULED': '手术安排',
        'SURGERY_CONFIRMED': '手术确认',
        'TEAM_CONFIRMATION_REQUEST': '团队确认请求',
        'DOCTOR_FINAL_CONFIRMATION_REQUEST': '医生最终确认请求',
        'PATIENT_SURGERY_NOTICE': '手术通知单',
        'SURGERY_UPDATE': '手术更新',
        'SURGERY_COMPLETE': '手术完成',
        'SURGERY_CANCELLED': '手术取消'
      }
      return typeMap[type] || type
    }

    const getContentPreview = (content) => {
      if (!content) return ''
      return content.length > 100 ? content.substring(0, 100) + '...' : content
    }

    const formatTime = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
      if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
      
      return date.toLocaleDateString('zh-CN')
    }

    const handleSizeChange = (newSize) => {
      pageSize.value = newSize
      currentPage.value = 1
      loadNotifications()
    }

    const handleCurrentChange = (newPage) => {
      currentPage.value = newPage
      loadNotifications()
    }

    onMounted(() => {
      loadNotifications()
    })

    return {
      loading,
      notifications,
      currentPage,
      pageSize,
      totalCount,
      unreadCount,
      todayCount,
      filterType,
      filterNotificationType,
      loadNotifications,
      viewNotification,
      markAsRead,
      markAllAsRead,
      canConfirm,
      handleConfirm,
      getNotificationIconClass,
      getNotificationTypeText,
      getContentPreview,
      formatTime,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style lang="scss" scoped>
.notification-list {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #1f2937;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
  
  .notification-stats {
    margin-bottom: 24px;
    
    .stats-content {
      display: flex;
      gap: 48px;
      
      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .stat-label {
          font-size: 14px;
          color: #6b7280;
          margin-bottom: 8px;
        }
        
        .stat-value {
          font-size: 24px;
          font-weight: 600;
          color: #1f2937;
          
          &.unread {
            color: #ef4444;
          }
        }
      }
    }
  }
  
  .notification-filters {
    margin-bottom: 24px;
    
    .filter-content {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
      
      .filter-buttons {
        flex-shrink: 0;
        
        .el-radio-group {
          display: flex;
          
          .el-radio-button {
            flex: 1;
            min-width: 0;
            
            :deep(.el-radio-button__inner) {
              padding: 8px 16px;
              border-radius: 4px;
              font-size: 14px;
              font-weight: 500;
              white-space: nowrap;
              text-align: center;
              min-width: 60px;
            }
            
            &:first-child :deep(.el-radio-button__inner) {
              border-top-left-radius: 6px;
              border-bottom-left-radius: 6px;
            }
            
            &:last-child :deep(.el-radio-button__inner) {
              border-top-right-radius: 6px;
              border-bottom-right-radius: 6px;
            }
          }
        }
      }
      
      .filter-select {
        flex-shrink: 0;
        min-width: 200px;
        
        .el-select {
          width: 100%;
        }
      }
      
      @media (max-width: 768px) {
        flex-direction: column;
        align-items: stretch;
        gap: 12px;
        
        .filter-buttons {
          width: 100%;
          
          .el-radio-group {
            width: 100%;
            
            .el-radio-button {
              flex: 1;
              
              :deep(.el-radio-button__inner) {
                width: 100%;
                padding: 10px 8px;
              }
            }
          }
        }
        
        .filter-select {
          width: 100%;
          min-width: auto;
        }
      }
    }
  }
  
  .notification-content {
    .empty-state {
      padding: 48px 0;
    }
    
    .notification-items {
      .notification-item {
        display: flex;
        align-items: flex-start;
        padding: 16px;
        border-bottom: 1px solid #f3f4f6;
        cursor: pointer;
        transition: background-color 0.2s ease;
        
        &:hover {
          background-color: #f9fafb;
        }
        
        &.unread {
          background-color: #eff6ff;
          border-left: 4px solid #3b82f6;
          
          .notification-title {
            font-weight: 600;
          }
        }
        
        &:last-child {
          border-bottom: none;
        }
        
        .notification-icon {
          margin-right: 12px;
          margin-top: 4px;
          
          .el-icon {
            font-size: 20px;
            
            &.icon-blue { color: #3b82f6; }
            &.icon-green { color: #10b981; }
            &.icon-orange { color: #f59e0b; }
            &.icon-purple { color: #8b5cf6; }
            &.icon-red { color: #ef4444; }
            &.icon-gray { color: #6b7280; }
          }
        }
        
        .notification-content-main {
          flex: 1;
          min-width: 0;
          
          .notification-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 8px;
            
            .notification-title {
              font-size: 16px;
              color: #1f2937;
              margin: 0;
              margin-right: 12px;
            }
            
            .notification-meta {
              display: flex;
              flex-direction: column;
              align-items: flex-end;
              font-size: 12px;
              color: #6b7280;
              white-space: nowrap;
              
              .notification-type {
                margin-bottom: 4px;
                padding: 2px 8px;
                background-color: #f3f4f6;
                border-radius: 4px;
              }
            }
          }
          
          .notification-preview {
            color: #6b7280;
            font-size: 14px;
            line-height: 1.5;
            margin-bottom: 8px;
          }
          
          .notification-related {
            margin-top: 8px;
          }
        }
        
        .notification-actions {
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-left: 12px;
        }
      }
    }
    
    .pagination-wrapper {
      margin-top: 24px;
      display: flex;
      justify-content: center;
    }
  }
}
</style> 