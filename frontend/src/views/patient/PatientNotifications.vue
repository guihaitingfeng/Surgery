<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="!!patientInfo"
    :is-blacklisted="isBlacklisted"
  >
    <div class="patient-notifications-page">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>通知消息</h2>
        <div class="header-actions">
          <el-button 
            v-if="unreadCount > 0"
            type="primary"
            plain
            size="small"
            @click="markAllAsRead"
          >
            全部标为已读
          </el-button>
        </div>
      </div>

      <!-- 筛选标签 -->
      <div class="filter-tabs">
        <el-radio-group v-model="filterType" @change="filterNotifications">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="unread">
            未读
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="tab-badge" />
          </el-radio-button>
          <el-radio-button label="SURGERY_SCHEDULED">手术安排</el-radio-button>
          <el-radio-button label="SURGERY_CONFIRMED">手术确认</el-radio-button>
          <el-radio-button label="SURGERY_CANCELLED">手术取消</el-radio-button>
          <el-radio-button label="SURGERY_UPDATED">手术更新</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 通知列表 -->
      <div class="notifications-container">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="5" animated />
        </div>
        
        <div v-else-if="filteredNotifications.length === 0" class="empty-container">
          <el-empty :description="emptyDescription" />
        </div>
        
        <div v-else class="notifications-list">
          <el-card 
            v-for="notification in filteredNotifications" 
            :key="notification.id"
            class="notification-card"
            :class="{ 'unread': !notification.isRead }"
            @click="viewNotification(notification)"
          >
            <div class="notification-header">
              <div class="header-left">
                <el-icon class="notification-icon" :class="getNotificationIconClass(notification.type)">
                  <component :is="getNotificationIcon(notification.type)" />
                </el-icon>
                <div class="notification-info">
                  <h4>{{ notification.title }}</h4>
                  <span class="notification-time">{{ formatTime(notification.createdAt) }}</span>
                </div>
              </div>
              <div class="header-right">
                <el-tag :type="getNotificationTagType(notification.type)" size="small">
                  {{ getNotificationTypeText(notification.type) }}
                </el-tag>
              </div>
            </div>
            
            <div class="notification-content">
              <p>{{ notification.content }}</p>
            </div>
            
            <div v-if="notification.relatedAppointment" class="related-info">
              <el-divider />
              <div class="appointment-brief">
                <span class="label">相关手术：</span>
                <span>{{ notification.relatedAppointment.surgeryName }}</span>
                <el-button 
                  type="text" 
                  size="small"
                  @click.stop="viewAppointment(notification.relatedAppointment)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
        
        <!-- 分页 -->
        <div v-if="total > pageSize" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadNotifications"
            @current-change="loadNotifications"
          />
        </div>
      </div>

      <!-- 通知详情对话框 -->
      <el-dialog
        v-model="detailDialogVisible"
        :title="selectedNotification?.title"
        width="600px"
      >
        <div v-if="selectedNotification" class="notification-detail">
          <div class="detail-header">
            <el-tag :type="getNotificationTagType(selectedNotification.type)">
              {{ getNotificationTypeText(selectedNotification.type) }}
            </el-tag>
            <span class="detail-time">{{ formatDateTime(selectedNotification.createdAt) }}</span>
          </div>
          
          <div class="detail-content">
            <p>{{ selectedNotification.content }}</p>
          </div>
          
          <div v-if="selectedNotification.relatedAppointment" class="detail-appointment">
            <h4>相关手术信息</h4>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="手术名称">
                {{ selectedNotification.relatedAppointment.surgeryName }}
              </el-descriptions-item>
              <el-descriptions-item label="手术日期">
                {{ selectedNotification.relatedAppointment.plannedDate }}
              </el-descriptions-item>
              <el-descriptions-item label="手术时间">
                {{ selectedNotification.relatedAppointment.plannedStartTime }} - 
                {{ selectedNotification.relatedAppointment.plannedEndTime }}
              </el-descriptions-item>
              <el-descriptions-item label="主刀医生">
                {{ selectedNotification.relatedAppointment.doctor?.realName }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-dialog>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Calendar, CircleCheck, Close, InfoFilled, Bell
} from '@element-plus/icons-vue'
import PatientLayout from '../../layouts/PatientLayout.vue'
import axios from 'axios'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

const loading = ref(false)
const notifications = ref([])
const patientInfo = ref(null)
const unreadCount = ref(0)
const isBlacklisted = ref(false)

const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const detailDialogVisible = ref(false)
const selectedNotification = ref(null)

const filteredNotifications = computed(() => {
  if (filterType.value === 'all') {
    return notifications.value
  } else if (filterType.value === 'unread') {
    return notifications.value.filter(n => !n.isRead)
  } else {
    return notifications.value.filter(n => n.type === filterType.value)
  }
})

const emptyDescription = computed(() => {
  if (filterType.value === 'unread') {
    return '暂无未读消息'
  } else if (filterType.value !== 'all') {
    return `暂无${getNotificationTypeText(filterType.value)}通知`
  }
  return '暂无通知消息'
})

onMounted(() => {
  loadPatientInfo()
  loadNotifications()
})

const loadPatientInfo = async () => {
  try {
    const response = await axios.get('/api/patient-portal/my-info')
    if (response.data && !response.data.message) {
      patientInfo.value = response.data
    }
  } catch (error) {
    console.error('加载病情信息失败:', error)
  }
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/patient-portal/notifications', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    notifications.value = response.data.content || []
    total.value = response.data.totalElements || 0
    
    // 更新未读数量
    unreadCount.value = notifications.value.filter(n => !n.isRead).length
  } catch (error) {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const viewNotification = async (notification) => {
  selectedNotification.value = notification
  detailDialogVisible.value = true
  
  // 标记为已读
  if (!notification.isRead) {
    await markAsRead(notification)
  }
}

const markAsRead = async (notification) => {
  try {
    await axios.put(`/api/patient-portal/notifications/${notification.id}/read`)
    notification.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

const markAllAsRead = async () => {
  try {
    // 批量标记已读
    const unreadNotifications = notifications.value.filter(n => !n.isRead)
    for (const notification of unreadNotifications) {
      await markAsRead(notification)
    }
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const viewAppointment = (appointment) => {
  // 跳转到预约详情
  router.push('/patient-dashboard/appointments')
}

const filterNotifications = () => {
  currentPage.value = 1
}

// 工具函数
const getNotificationIcon = (type) => {
  const iconMap = {
    'SURGERY_SCHEDULED': 'Calendar',
    'SURGERY_CONFIRMED': 'CircleCheck',
    'SURGERY_CANCELLED': 'Close',
    'SURGERY_UPDATED': 'InfoFilled'
  }
  return iconMap[type] || 'Bell'
}

const getNotificationIconClass = (type) => {
  const classMap = {
    'SURGERY_SCHEDULED': 'primary',
    'SURGERY_CONFIRMED': 'success',
    'SURGERY_CANCELLED': 'danger',
    'SURGERY_UPDATED': 'warning'
  }
  return classMap[type] || 'info'
}

const getNotificationTagType = (type) => {
  const typeMap = {
    'SURGERY_SCHEDULED': '',
    'SURGERY_CONFIRMED': 'success',
    'SURGERY_CANCELLED': 'danger',
    'SURGERY_UPDATED': 'warning'
  }
  return typeMap[type] || 'info'
}

const getNotificationTypeText = (type) => {
  const textMap = {
    'SURGERY_SCHEDULED': '手术安排',
    'SURGERY_CONFIRMED': '手术确认',
    'SURGERY_CANCELLED': '手术取消',
    'SURGERY_UPDATED': '手术更新'
  }
  return textMap[type] || '系统通知'
}

const formatTime = (time) => {
  return dayjs(time).fromNow()
}

const formatDateTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style lang="scss" scoped>
.patient-notifications-page {
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
  }
  
  .filter-tabs {
    margin-bottom: 24px;
    
    .tab-badge {
      margin-left: 4px;
      
      :deep(.el-badge__content) {
        height: 16px;
        line-height: 16px;
        padding: 0 6px;
      }
    }
  }
  
  .notifications-container {
    .loading-container {
      background: white;
      padding: 24px;
      border-radius: 8px;
    }
    
    .empty-container {
      background: white;
      padding: 60px;
      border-radius: 8px;
      text-align: center;
    }
    
    .notifications-list {
      display: grid;
      gap: 16px;
    }
    
    .notification-card {
      cursor: pointer;
      transition: all 0.3s ease;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
      }
      
      &.unread {
        border-left: 3px solid #409eff;
        background-color: #f0f9ff;
      }
      
      .notification-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;
        
        .header-left {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          
          .notification-icon {
            font-size: 20px;
            margin-top: 2px;
            
            &.primary {
              color: #409eff;
            }
            
            &.success {
              color: #67c23a;
            }
            
            &.warning {
              color: #e6a23c;
            }
            
            &.danger {
              color: #f56c6c;
            }
            
            &.info {
              color: #909399;
            }
          }
          
          .notification-info {
            h4 {
              margin: 0 0 4px 0;
              font-size: 16px;
              color: #303133;
            }
            
            .notification-time {
              font-size: 12px;
              color: #909399;
            }
          }
        }
      }
      
      .notification-content {
        p {
          margin: 0;
          font-size: 14px;
          color: #606266;
          line-height: 1.6;
        }
      }
      
      .related-info {
        .appointment-brief {
          display: flex;
          align-items: center;
          gap: 8px;
          
          .label {
            font-size: 13px;
            color: #909399;
          }
          
          span {
            font-size: 13px;
            color: #303133;
          }
        }
      }
    }
    
    .pagination-container {
      margin-top: 24px;
      display: flex;
      justify-content: center;
    }
  }
  
  .notification-detail {
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      
      .detail-time {
        font-size: 13px;
        color: #909399;
      }
    }
    
    .detail-content {
      margin-bottom: 24px;
      
      p {
        font-size: 14px;
        color: #303133;
        line-height: 1.8;
      }
    }
    
    .detail-appointment {
      h4 {
        font-size: 16px;
        color: #303133;
        margin: 0 0 16px 0;
      }
    }
  }
}
</style> 