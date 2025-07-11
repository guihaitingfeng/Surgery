<template>
  <div class="appointment-calendar-container">
    <div class="page-header">
      <h2>手术日历</h2>
      <div class="header-actions">
        <el-button 
          type="default"
          @click="$router.push('/appointments')"
        >
          列表视图
        </el-button>
        <el-button 
          v-if="isDoctor"
          type="primary" 
          :icon="Plus"
          @click="$router.push('/appointments/create')"
        >
          创建预约
        </el-button>
      </div>
    </div>

    <!-- 主体布局：左侧日历 + 右侧详情 -->
    <div class="calendar-layout">
      <!-- 左侧日历 -->
      <div class="calendar-section">
        <el-calendar 
          v-model="currentDate"
          @update:value="onDateChange"
        >
          <template #header="{ date }">
            <span>{{ formatHeaderDate(date) }}</span>
          </template>
          
          <template #date-cell="{ data }">
            <div 
              class="calendar-cell"
              :class="{ 
                'has-appointments': getAppointmentsForDate(data.day).length > 0,
                'selected-date': selectedDate === data.day
              }"
              @click="selectDate(data.day)"
            >
              <div class="date-number">{{ data.day.split('-').slice(-1)[0] }}</div>
              <div class="appointment-indicator" v-if="getAppointmentsForDate(data.day).length > 0">
                <div class="appointment-count">
                  {{ getAppointmentsForDate(data.day).length }}台手术
                </div>
                <div class="status-dots">
                  <span 
                    v-for="appointment in getAppointmentsForDate(data.day).slice(0, 3)"
                    :key="appointment.id"
                    :class="[
                      'status-dot',
                      `dot-${getStatusClass(appointment.status)}`
                    ]"
                  ></span>
                  <span v-if="getAppointmentsForDate(data.day).length > 3" class="more-indicator">...</span>
                </div>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>

      <!-- 右侧详情面板 -->
      <div class="detail-section">
        <el-card class="detail-card" shadow="always">
          <template #header>
            <div class="detail-header">
              <h3 v-if="selectedDate">
                {{ formatSelectedDate(selectedDate) }} 的手术安排
              </h3>
              <h3 v-else>
                请点击日期查看手术安排
              </h3>
              <el-tag v-if="selectedDateAppointments.length > 0" type="primary">
                共 {{ selectedDateAppointments.length }} 台手术
              </el-tag>
            </div>
          </template>

          <div v-if="!selectedDate" class="empty-state">
            <el-empty 
              description="请在左侧日历中点击日期查看该日的手术安排"
              :image-size="120"
            />
          </div>

          <div v-else-if="selectedDateAppointments.length === 0" class="empty-state">
            <el-empty 
              description="该日期暂无手术安排"
              :image-size="120"
            />
          </div>

          <div v-else class="appointments-list">
            <div 
              v-for="appointment in selectedDateAppointments"
              :key="appointment.id"
              class="appointment-card"
              @click="viewAppointment(appointment)"
            >
              <div class="appointment-time-header">
                <div class="time-info">
                  <el-icon><Clock /></el-icon>
                  <span class="time-text">
                    {{ appointment.plannedStartTime }} - {{ appointment.plannedEndTime }}
                  </span>
                  <span class="duration">
                    ({{ appointment.estimatedDuration }}分钟)
                  </span>
                </div>
                <el-tag 
                  :type="getStatusTagType(appointment.status)" 
                  size="small"
                >
                  {{ getStatusText(appointment.status) }}
                </el-tag>
              </div>

              <div class="appointment-content">
                <div class="surgery-info">
                  <h4 class="surgery-name">{{ appointment.surgeryName }}</h4>
                  <p class="surgery-type" v-if="appointment.surgeryType">
                    {{ appointment.surgeryType }}
                  </p>
                </div>

                <div class="appointment-details">
                  <div class="detail-row">
                    <el-icon><User /></el-icon>
                    <span class="label">病人：</span>
                    <span class="value">{{ appointment.patient?.user?.realName || '未知' }}</span>
                  </div>
                  <div class="detail-row">
                    <el-icon><UserFilled /></el-icon>
                    <span class="label">主刀医生：</span>
                    <span class="value">{{ appointment.doctor?.realName || '未分配' }}</span>
                  </div>
                  <div class="detail-row">
                    <el-icon><Location /></el-icon>
                    <span class="label">手术室：</span>
                    <span class="value">{{ appointment.room?.roomName || '未分配' }}</span>
                  </div>
                  <div class="detail-row" v-if="appointment.anesthesiologist">
                    <el-icon><Avatar /></el-icon>
                    <span class="label">麻醉师：</span>
                    <span class="value">{{ appointment.anesthesiologist.realName }}</span>
                  </div>
                  <div class="detail-row" v-if="appointment.nurse">
                    <el-icon><Avatar /></el-icon>
                    <span class="label">护士：</span>
                    <span class="value">{{ appointment.nurse.realName }}</span>
                  </div>
                </div>

                <div class="appointment-actions">
                  <el-button 
                    type="primary" 
                    size="small"
                    @click.stop="viewAppointment(appointment)"
                  >
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 图例 -->
        <el-card class="legend-card" shadow="never">
          <div class="legend">
            <h4>状态图例</h4>
            <div class="legend-items">
              <div class="legend-item">
                <div class="legend-color dot-pending"></div>
                <span>等待确认</span>
              </div>
              <div class="legend-item">
                <div class="legend-color dot-scheduled"></div>
                <span>已安排</span>
              </div>
              <div class="legend-item">
                <div class="legend-color dot-in-progress"></div>
                <span>进行中</span>
              </div>
              <div class="legend-item">
                <div class="legend-color dot-completed"></div>
                <span>已完成</span>
              </div>
              <div class="legend-item">
                <div class="legend-color dot-cancelled"></div>
                <span>已取消</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Plus, Clock, User, UserFilled, Location, Avatar } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import axios from 'axios'

const router = useRouter()
const store = useStore()

const isDoctor = computed(() => store.getters['auth/isDoctor'])

const currentDate = ref(new Date())
const appointments = ref([])
const selectedDate = ref('')
const selectedDateAppointments = computed(() => {
  if (!selectedDate.value) return []
  return getAppointmentsForDate(selectedDate.value)
})

onMounted(() => {
  loadCalendarData()
})

const loadCalendarData = async () => {
  try {
    let url = '/api/appointments/my-appointments'
    
    // 如果是管理员或其他角色，可以获取所有预约
    if (store.getters['auth/userRole'] === 'ADMIN') {
      url = '/api/appointments'
    }
    
    const response = await axios.get(url)
    appointments.value = response.data.content || response.data || []
    
    console.log('日历数据加载成功，预约数量:', appointments.value.length)
  } catch (error) {
    console.error('加载日历数据失败:', error)
    ElMessage.error('加载日历数据失败')
  }
}

const onDateChange = (date) => {
  currentDate.value = date
  loadCalendarData()
}

const selectDate = (date) => {
  selectedDate.value = date
}

const getAppointmentsForDate = (date) => {
  return appointments.value.filter(appointment => 
    appointment.plannedDate === date
  )
}

const getStatusClass = (status) => {
  const map = {
    'SCHEDULED': 'scheduled',
    'PENDING_CONFIRMATION': 'pending',
    'TEAM_CONFIRMED': 'scheduled',
    'DOCTOR_FINAL_CONFIRMED': 'scheduled',
    'NOTIFIED': 'scheduled',
    'IN_PROGRESS': 'in-progress',
    'COMPLETED': 'completed',
    'CANCELLED': 'cancelled'
  }
  return map[status] || 'pending'
}

const viewAppointment = (appointment) => {
  router.push(`/appointments/${appointment.id}`)
}

const formatHeaderDate = (date) => {
  return dayjs(date).format('YYYY年MM月')
}

const formatSelectedDate = (date) => {
  return dayjs(date).format('YYYY年MM月DD日')
}

const getStatusText = (status) => {
  const map = {
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '等待医疗团队确认',
    'TEAM_CONFIRMED': '医疗团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生最终确认',
    'NOTIFIED': '已通知病人',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'POSTPONED': '已延期'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = {
    'SCHEDULED': 'warning',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'primary',
    'DOCTOR_FINAL_CONFIRMED': 'primary',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'success',
    'COMPLETED': 'success',
    'CANCELLED': 'danger',
    'POSTPONED': 'info'
  }
  return map[status] || 'info'
}
</script>

<style lang="scss" scoped>
.appointment-calendar-container {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .calendar-layout {
    display: flex;
    gap: 20px;
    height: calc(100vh - 200px);
    min-height: 600px;
    
    .calendar-section {
      flex: 1;
      min-width: 0;
      
      .calendar-cell {
        height: 80px;
        padding: 8px;
        cursor: pointer;
        transition: all 0.2s;
        border-radius: 4px;
        position: relative;
        
        &:hover {
          background-color: #f5f5f5;
        }
        
        &.has-appointments {
          background-color: #f0f9ff;
          border: 1px solid #e0f2fe;
          
          &:hover {
            background-color: #e0f2fe;
          }
        }
        
        &.selected-date {
          background-color: #1890ff;
          color: white;
          
          .appointment-count {
            color: white;
          }
        }
        
        .date-number {
          font-weight: bold;
          font-size: 16px;
          margin-bottom: 4px;
        }
        
        .appointment-indicator {
          .appointment-count {
            font-size: 12px;
            color: #666;
            margin-bottom: 4px;
          }
          
          .status-dots {
            display: flex;
            gap: 2px;
            align-items: center;
            
            .status-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              display: inline-block;
              
              &.dot-pending {
                background: #facc15;
              }
              
              &.dot-scheduled {
                background: #3b82f6;
              }
              
              &.dot-in-progress {
                background: #10b981;
              }
              
              &.dot-completed {
                background: #6b7280;
              }
              
              &.dot-cancelled {
                background: #ef4444;
              }
            }
            
            .more-indicator {
              font-size: 10px;
              color: #999;
              margin-left: 2px;
            }
          }
        }
      }
    }
    
    .detail-section {
      width: 400px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      
      .detail-card {
        flex: 1;
        
        .detail-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          h3 {
            margin: 0;
            color: #2c3e50;
            font-size: 16px;
          }
        }
        
        .empty-state {
          padding: 40px 20px;
          text-align: center;
        }
        
        .appointments-list {
          max-height: calc(100vh - 350px);
          overflow-y: auto;
          
          .appointment-card {
            border: 1px solid #e4e7ed;
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 12px;
            cursor: pointer;
            transition: all 0.2s;
            
            &:hover {
              border-color: #1890ff;
              box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
            }
            
            .appointment-time-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 12px;
              
              .time-info {
                display: flex;
                align-items: center;
                gap: 8px;
                
                .el-icon {
                  color: #1890ff;
                }
                
                .time-text {
                  font-weight: 600;
                  color: #2c3e50;
                }
                
                .duration {
                  font-size: 12px;
                  color: #999;
                }
              }
            }
            
            .appointment-content {
              .surgery-info {
                margin-bottom: 12px;
                
                .surgery-name {
                  margin: 0 0 4px 0;
                  color: #2c3e50;
                  font-size: 16px;
                }
                
                .surgery-type {
                  margin: 0;
                  color: #666;
                  font-size: 14px;
                }
              }
              
              .appointment-details {
                margin-bottom: 12px;
                
                .detail-row {
                  display: flex;
                  align-items: center;
                  margin-bottom: 8px;
                  font-size: 14px;
                  
                  .el-icon {
                    margin-right: 8px;
                    color: #666;
                    width: 16px;
                  }
                  
                  .label {
                    color: #666;
                    margin-right: 8px;
                    min-width: 60px;
                  }
                  
                  .value {
                    color: #2c3e50;
                    font-weight: 500;
                  }
                }
              }
              
              .appointment-actions {
                text-align: right;
              }
            }
          }
        }
      }
      
      .legend-card {
        .legend {
          h4 {
            margin: 0 0 12px 0;
            color: #2c3e50;
            font-size: 14px;
          }
          
          .legend-items {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
          }
          
          .legend-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
          }
          
          .legend-color {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            
            &.dot-pending {
              background: #facc15;
            }
            
            &.dot-scheduled {
              background: #3b82f6;
            }
            
            &.dot-in-progress {
              background: #10b981;
            }
            
            &.dot-completed {
              background: #6b7280;
            }
            
            &.dot-cancelled {
              background: #ef4444;
            }
          }
        }
      }
    }
  }
}

:deep(.el-calendar-table .el-calendar-day) {
  height: 100px;
  padding: 0;
}

:deep(.el-calendar__header) {
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
}

// 响应式设计
@media (max-width: 1200px) {
  .appointment-calendar-container .calendar-layout {
    flex-direction: column;
    height: auto;
    
    .detail-section {
      width: 100%;
    }
  }
}
</style> 