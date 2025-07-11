<template>
  <div class="notification-detail">
    <div class="main-content">
      <!-- 返回按钮 -->
      <div class="mb-3">
        <el-button 
          @click="$router.go(-1)" 
          type="primary"
          text
          :icon="ArrowLeft"
        >
          返回
        </el-button>
      </div>

      <!-- 通知卡片 -->
      <el-card v-if="notification" shadow="always" class="notification-card">
        <!-- 通知头部 -->
        <template #header>
          <div class="flex flex-between">
            <h1 style="font-size: 20px; font-weight: 600; margin: 0;">{{ notification.title }}</h1>
            <el-tag 
              :type="notification.isRead ? 'info' : 'primary'"
              size="small"
            >
              {{ notification.isRead ? '已读' : '未读' }}
            </el-tag>
          </div>
          <div class="mt-2 text-muted" style="font-size: 12px;">
            <span>{{ formatDate(notification.createdAt) }}</span>
            <span v-if="notification.readAt" style="margin-left: 16px;">
              阅读时间：{{ formatDate(notification.readAt) }}
            </span>
          </div>
        </template>

        <!-- 通知内容 -->
        <div class="mb-3">
          <div style="line-height: 1.6; white-space: pre-wrap;">{{ notification.content }}</div>
        </div>

        <!-- 相关预约信息 -->
        <el-card v-if="notification.relatedAppointment" shadow="never" class="mb-3" style="background-color: #f8f9fa;">
          <template #header>
            <h3 style="font-size: 16px; font-weight: 600; margin: 0;">相关预约信息</h3>
          </template>
          
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="info-item">
                <span class="text-muted">手术名称：</span>
                <span style="font-weight: 500;">{{ notification.relatedAppointment.surgeryName }}</span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="text-muted">手术时间：</span>
                <span style="font-weight: 500;">
                  {{ notification.relatedAppointment.plannedDate }} 
                  {{ notification.relatedAppointment.plannedStartTime }}
                </span>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="info-item">
                <span class="text-muted">状态：</span>
                <el-tag :type="getStatusType(notification.relatedAppointment.status)" size="small">
                  {{ getStatusText(notification.relatedAppointment.status) }}
                </el-tag>
              </div>
            </el-col>
            <el-col :span="12" v-if="notification.relatedAppointment.room">
              <div class="info-item">
                <span class="text-muted">手术室：</span>
                <span style="font-weight: 500;">{{ notification.relatedAppointment.room.roomName }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 操作按钮 -->
        <div class="flex" style="gap: 12px; flex-wrap: wrap;">
          <!-- 医疗团队确认按钮 -->
          <el-button
            v-if="canConfirmByTeam"
            @click="confirmByTeam"
            :loading="loading"
            type="success"
            :icon="Check"
          >
            {{ loading ? '确认中...' : '确认参与' }}
          </el-button>

          <!-- 医生最终确认按钮 -->
          <el-button
            v-if="canDoctorFinalConfirm"
            @click="doctorFinalConfirm"
            :loading="loading"
            type="primary"
            :icon="Check"
          >
            {{ loading ? '确认中...' : '最终确认' }}
          </el-button>

          <!-- 查看预约详情按钮 -->
          <el-button
            v-if="notification.relatedAppointment"
            @click="viewAppointment"
            type="info"
            :icon="View"
          >
            查看预约详情
          </el-button>

          <!-- 标记为已读按钮 -->
          <el-button
            v-if="!notification.isRead"
            @click="markAsRead"
            :loading="loading"
            type="default"
            :icon="Check"
          >
            标记为已读
          </el-button>
        </div>
      </el-card>

      <!-- 加载状态 -->
      <div v-else class="flex flex-center" style="height: 300px;">
        <el-loading-directive />
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { ArrowLeft, Check, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

export default {
  name: 'NotificationDetail',
  components: {
    ArrowLeft,
    Check,
    View
  },
  data() {
    return {
      notification: null,
      loading: false
    }
  },
  computed: {
    ...mapGetters('auth', ['user']),
    canConfirmByTeam() {
      if (!this.notification || !this.notification.relatedAppointment || !this.user) return false
      
      const appointment = this.notification.relatedAppointment
      const isTeamMember = (
        (appointment.anesthesiologist && appointment.anesthesiologist.id === this.user.id) ||
        (appointment.nurse && appointment.nurse.id === this.user.id)
      )
      
      return (
        this.notification.type === 'TEAM_CONFIRMATION_REQUEST' &&
        appointment.status === 'PENDING_CONFIRMATION' &&
        isTeamMember &&
        (this.user.role === 'ANESTHESIOLOGIST' || this.user.role === 'NURSE')
      )
    },
    canDoctorFinalConfirm() {
      if (!this.notification || !this.notification.relatedAppointment || !this.user) return false
      
      const appointment = this.notification.relatedAppointment
      return (
        this.notification.type === 'DOCTOR_FINAL_CONFIRMATION_REQUEST' &&
        appointment.status === 'TEAM_CONFIRMED' &&
        appointment.doctor && appointment.doctor.id === this.user.id &&
        this.user.role === 'DOCTOR'
      )
    }
  },
  async created() {
    console.log('当前用户:', this.user)
    await this.loadNotification()
    if (this.notification && !this.notification.isRead) {
      await this.markAsRead()
    }
  },
  methods: {
    async loadNotification() {
      try {
        console.log('正在加载通知ID:', this.$route.params.id)
        const response = await api.get(`/api/notifications/${this.$route.params.id}`)
        this.notification = response.data
        console.log('通知数据加载成功:', this.notification)
      } catch (error) {
        console.error('加载通知失败:', error)
        ElMessage.error('加载通知失败')
      }
    },
    async confirmByTeam() {
      if (!this.notification.relatedAppointment) return
      
      try {
        this.loading = true
        await api.post(`/api/appointments/${this.notification.relatedAppointment.id}/confirm-by-team`)
        ElMessage.success('确认成功')
        await this.loadNotification() // 重新加载通知
      } catch (error) {
        console.error('确认失败:', error)
        ElMessage.error(error.response?.data?.error || '确认失败')
      } finally {
        this.loading = false
      }
    },
    async doctorFinalConfirm() {
      if (!this.notification.relatedAppointment) return
      
      try {
        this.loading = true
        await api.post(`/api/appointments/${this.notification.relatedAppointment.id}/doctor-final-confirm`)
        ElMessage.success('最终确认成功，已发送手术通知单给病人')
        await this.loadNotification() // 重新加载通知
      } catch (error) {
        console.error('最终确认失败:', error)
        ElMessage.error(error.response?.data?.error || '最终确认失败')
      } finally {
        this.loading = false
      }
    },
    async markAsRead() {
      try {
        this.loading = true
        await api.put(`/api/notifications/${this.notification.id}/read`)
        this.notification.isRead = true
        this.notification.readAt = new Date().toISOString()
      } catch (error) {
        console.error('标记已读失败:', error)
      } finally {
        this.loading = false
      }
    },
    viewAppointment() {
      if (this.notification.relatedAppointment) {
        this.$router.push(`/appointments/${this.notification.relatedAppointment.id}`)
      }
    },
    formatDate(dateString) {
      if (!dateString) return ''
      return new Date(dateString).toLocaleString('zh-CN')
    },
    getStatusType(status) {
      const statusTypes = {
        'SCHEDULED': 'warning',
        'PENDING_CONFIRMATION': 'warning',
        'TEAM_CONFIRMED': 'primary',
        'DOCTOR_FINAL_CONFIRMED': 'primary',
        'NOTIFIED': 'info',
        'IN_PROGRESS': 'success',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
      }
      return statusTypes[status] || 'info'
    },
    getStatusText(status) {
      const statusTexts = {
        'SCHEDULED': '已安排',
        'PENDING_CONFIRMATION': '等待医疗团队确认',
        'TEAM_CONFIRMED': '医疗团队已确认',
        'DOCTOR_FINAL_CONFIRMED': '医生最终确认',
        'NOTIFIED': '已通知病人',
        'IN_PROGRESS': '进行中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消'
      }
      return statusTexts[status] || status
    }
  }
}
</script>

<style scoped>
.notification-detail .main-content {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.notification-card .info-item {
  margin-bottom: 12px;
}

.notification-card .info-item .text-muted {
  color: #909399;
  font-size: 14px;
  margin-right: 8px;
}

@media (max-width: 768px) {
  .notification-detail .main-content {
    padding: 16px;
  }
}
</style> 