<template>
  <div class="schedule-container">
    <div class="page-header">
      <h2>排班管理</h2>
      <p>查看医护人员排班信息和空闲时段</p>
    </div>

    <!-- 医护人员列表 -->
    <div class="staff-grid">
      <!-- 麻醉师卡片 -->
      <div class="staff-section">
        <div class="section-header">
          <h3>麻醉师</h3>
          <el-tag type="warning" size="small">{{ anesthesiologists.length }}人</el-tag>
        </div>
        <div class="staff-cards">
          <div 
            v-for="staff in anesthesiologists" 
            :key="staff.id"
            class="staff-card"
            @click="selectStaff(staff)"
            :class="{ active: selectedStaff?.id === staff.id }"
          >
            <div class="staff-avatar">
              <el-avatar :size="40">
                {{ staff.realName?.charAt(0) }}
              </el-avatar>
            </div>
            <div class="staff-info">
              <h4>{{ staff.realName }}</h4>
              <p>{{ staff.department || '麻醉科' }}</p>
              <p>{{ staff.professionalTitle || '麻醉师' }}</p>
            </div>
            <div class="staff-status">
              <el-tag :type="staff.isActive ? 'success' : 'danger'" size="small">
                {{ staff.isActive ? '在职' : '离职' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 护士卡片 -->
      <div class="staff-section">
        <div class="section-header">
          <h3>护士</h3>
          <el-tag type="success" size="small">{{ nurses.length }}人</el-tag>
        </div>
        <div class="staff-cards">
          <div 
            v-for="staff in nurses" 
            :key="staff.id"
            class="staff-card"
            @click="selectStaff(staff)"
            :class="{ active: selectedStaff?.id === staff.id }"
          >
            <div class="staff-avatar">
              <el-avatar :size="40">
                {{ staff.realName?.charAt(0) }}
              </el-avatar>
            </div>
            <div class="staff-info">
              <h4>{{ staff.realName }}</h4>
              <p>{{ staff.department || '手术室' }}</p>
              <p>{{ staff.professionalTitle || '护士' }}</p>
            </div>
            <div class="staff-status">
              <el-tag :type="staff.isActive ? 'success' : 'danger'" size="small">
                {{ staff.isActive ? '在职' : '离职' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 排班详情面板 -->
    <div v-if="selectedStaff" class="schedule-detail-panel">
      <div class="panel-header">
        <div class="staff-detail">
          <el-avatar :size="60">
            {{ selectedStaff.realName?.charAt(0) }}
          </el-avatar>
          <div class="staff-detail-info">
            <h3>{{ selectedStaff.realName }}</h3>
            <p>{{ getRoleText(selectedStaff.role) }} | {{ selectedStaff.department || '未设置' }}</p>
            <p>{{ selectedStaff.professionalTitle || '未设置' }}</p>
          </div>
        </div>
        <div class="panel-actions">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="loadDaySchedule"
          />
          <el-button type="primary" @click="loadWeeklySchedule">
            查看一周排班
          </el-button>
        </div>
      </div>

      <!-- 当天排班详情 -->
      <div v-if="daySchedule" class="day-schedule">
        <div class="schedule-header">
          <h4>{{ selectedDate }} 排班详情</h4>
          <div class="schedule-summary">
            <el-tag type="info">{{ daySchedule.totalAppointments }}台手术</el-tag>
            <el-tag type="success">{{ daySchedule.freeSlots?.length || 0 }}个空闲时段</el-tag>
          </div>
        </div>

        <div class="schedule-content">
          <!-- 手术安排 -->
          <div class="appointments-section">
            <h5>手术安排</h5>
            <div v-if="daySchedule.appointments?.length === 0" class="empty-state">
              <el-empty description="当天没有手术安排" :image-size="60" />
            </div>
            <div v-else class="appointment-list">
              <div 
                v-for="appointment in daySchedule.appointments" 
                :key="appointment.id"
                class="appointment-item"
              >
                <div class="appointment-time">
                  <el-tag type="primary" size="small">
                    {{ appointment.plannedStartTime }} - {{ appointment.plannedEndTime }}
                  </el-tag>
                </div>
                <div class="appointment-info">
                  <h6>{{ appointment.surgeryName }}</h6>
                  <p>病人：{{ appointment.patient?.user?.realName }}</p>
                  <p>主刀医生：{{ appointment.doctor?.realName }}</p>
                  <p>手术室：{{ appointment.room?.roomName }}</p>
                </div>
                <div class="appointment-status">
                  <el-tag :type="getStatusTagType(appointment.status)" size="small">
                    {{ getStatusText(appointment.status) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <!-- 空闲时段 -->
          <div class="free-slots-section">
            <h5>空闲时段</h5>
            <div v-if="daySchedule.freeSlots?.length === 0" class="empty-state">
              <el-empty description="当天没有空闲时段" :image-size="60" />
            </div>
            <div v-else class="free-slots-list">
              <div 
                v-for="(slot, index) in daySchedule.freeSlots" 
                :key="index"
                class="free-slot-item"
              >
                <div class="slot-time">
                  <el-tag type="success" size="small">
                    {{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}
                  </el-tag>
                </div>
                <div class="slot-info">
                  <span class="slot-duration">{{ slot.duration }}分钟</span>
                  <span class="slot-type">{{ slot.type }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 一周排班详情 -->
      <div v-if="weeklySchedule" class="weekly-schedule">
        <div class="schedule-header">
          <h4>{{ weeklySchedule.startDate }} 至 {{ weeklySchedule.endDate }} 一周排班</h4>
          <div class="schedule-summary">
            <el-tag type="info">
              共{{ getTotalWeeklyAppointments() }}台手术
            </el-tag>
            <el-tag type="success">
              平均{{ getAverageAvailableHours() }}小时/天空闲
            </el-tag>
          </div>
        </div>

        <div class="weekly-schedule-grid">
          <div 
            v-for="dayData in weeklySchedule.weeklySchedule" 
            :key="dayData.date"
            class="day-card"
          >
            <div class="day-header">
              <h6>{{ formatDate(dayData.date) }}</h6>
              <span class="day-name">{{ getDayName(dayData.dayOfWeek) }}</span>
            </div>
            <div class="day-stats">
              <div class="stat-item">
                <span class="stat-label">手术</span>
                <span class="stat-value">{{ dayData.totalAppointments }}台</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">空闲</span>
                <span class="stat-value">{{ dayData.availableHours?.toFixed(1) }}小时</span>
              </div>
            </div>
            <div class="day-availability">
              <div class="availability-bar">
                <div 
                  class="availability-fill"
                  :style="{ width: getAvailabilityPercentage(dayData.availableHours) + '%' }"
                ></div>
              </div>
              <span class="availability-text">
                {{ getAvailabilityText(dayData.availableHours) }}
              </span>
            </div>
            <div class="day-actions">
              <el-button 
                size="small" 
                type="text" 
                @click="viewDayDetail(dayData.date)"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!selectedStaff" class="empty-selection">
      <el-empty 
        description="请选择医护人员查看排班信息" 
        :image-size="120"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const anesthesiologists = ref([])
const nurses = ref([])
const selectedStaff = ref(null)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const daySchedule = ref(null)
const weeklySchedule = ref(null)

onMounted(() => {
  loadStaffList()
})

const loadStaffList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/schedule/staff')
    anesthesiologists.value = response.data.anesthesiologists || []
    nurses.value = response.data.nurses || []
  } catch (error) {
    console.error('加载医护人员列表失败:', error)
    ElMessage.error('加载医护人员列表失败')
  } finally {
    loading.value = false
  }
}

const selectStaff = (staff) => {
  selectedStaff.value = staff
  daySchedule.value = null
  weeklySchedule.value = null
  loadDaySchedule()
}

const loadDaySchedule = async () => {
  if (!selectedStaff.value || !selectedDate.value) return
  
  loading.value = true
  try {
    const response = await axios.get(
      `/api/schedule/staff/${selectedStaff.value.id}/availability/${selectedDate.value}`
    )
    daySchedule.value = response.data
    weeklySchedule.value = null
  } catch (error) {
    console.error('加载当天排班失败:', error)
    ElMessage.error('加载当天排班失败')
  } finally {
    loading.value = false
  }
}

const loadWeeklySchedule = async () => {
  if (!selectedStaff.value) return
  
  loading.value = true
  try {
    const response = await axios.get(
      `/api/schedule/staff/${selectedStaff.value.id}/weekly-availability`,
      { params: { startDate: selectedDate.value } }
    )
    weeklySchedule.value = response.data
    daySchedule.value = null
  } catch (error) {
    console.error('加载一周排班失败:', error)
    ElMessage.error('加载一周排班失败')
  } finally {
    loading.value = false
  }
}

const viewDayDetail = (date) => {
  selectedDate.value = date
  loadDaySchedule()
}

const getRoleText = (role) => {
  const map = { 
    'DOCTOR': '医生', 
    'NURSE': '护士', 
    'ANESTHESIOLOGIST': '麻醉师',
    'PATIENT': '病人',
    'ADMIN': '管理员'
  }
  return map[role] || role
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待安排',
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '待确认',
    'TEAM_CONFIRMED': '团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生已确认',
    'NOTIFIED': '已通知',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'SCHEDULED': 'primary',
    'PENDING_CONFIRMATION': 'warning',
    'TEAM_CONFIRMED': 'success',
    'DOCTOR_FINAL_CONFIRMED': 'success',
    'NOTIFIED': 'info',
    'IN_PROGRESS': 'primary',
    'COMPLETED': 'success',
    'CANCELLED': 'danger'
  }
  return statusMap[status] || 'info'
}

const formatTime = (time) => {
  if (typeof time === 'string') return time
  if (time && time.length >= 2) {
    return `${time[0].toString().padStart(2, '0')}:${time[1].toString().padStart(2, '0')}`
  }
  return time
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

const getDayName = (dayOfWeek) => {
  const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return days[dayOfWeek] || ''
}

const getTotalWeeklyAppointments = () => {
  if (!weeklySchedule.value) return 0
  return weeklySchedule.value.weeklySchedule.reduce((total, day) => {
    return total + day.totalAppointments
  }, 0)
}

const getAverageAvailableHours = () => {
  if (!weeklySchedule.value) return 0
  const totalHours = weeklySchedule.value.weeklySchedule.reduce((total, day) => {
    return total + (day.availableHours || 0)
  }, 0)
  return (totalHours / 7).toFixed(1)
}

const getAvailabilityPercentage = (hours) => {
  const maxHours = 10 // 工作日最大10小时
  return Math.min((hours / maxHours) * 100, 100)
}

const getAvailabilityText = (hours) => {
  if (hours >= 8) return '空闲充足'
  if (hours >= 4) return '空闲适中'
  if (hours >= 2) return '空闲较少'
  return '几乎无空闲'
}
</script>

<style lang="scss" scoped>
.schedule-container {
  padding: 20px;
  
  .page-header {
    margin-bottom: 24px;
    
    h2 {
      margin: 0 0 8px 0;
      color: #2c3e50;
      font-size: 24px;
      font-weight: 600;
    }
    
    p {
      margin: 0;
      color: #7f8c8d;
    }
  }
  
  .staff-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    margin-bottom: 24px;
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }
  
  .staff-section {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      h3 {
        margin: 0;
        color: #2c3e50;
        font-size: 18px;
        font-weight: 600;
      }
    }
    
    .staff-cards {
      display: grid;
      gap: 12px;
    }
    
    .staff-card {
      display: flex;
      align-items: center;
      padding: 16px;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
      }
      
      &.active {
        border-color: #409eff;
        background-color: #f0f9ff;
      }
      
      .staff-avatar {
        margin-right: 12px;
      }
      
      .staff-info {
        flex: 1;
        
        h4 {
          margin: 0 0 4px 0;
          color: #2c3e50;
          font-size: 16px;
          font-weight: 500;
        }
        
        p {
          margin: 0;
          color: #7f8c8d;
          font-size: 12px;
          line-height: 1.4;
        }
      }
      
      .staff-status {
        margin-left: 12px;
      }
    }
  }
  
  .schedule-detail-panel {
    background: white;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid #e4e7ed;
      
      @media (max-width: 768px) {
        flex-direction: column;
        gap: 16px;
      }
      
      .staff-detail {
        display: flex;
        align-items: center;
        gap: 16px;
        
        .staff-detail-info {
          h3 {
            margin: 0 0 4px 0;
            color: #2c3e50;
            font-size: 20px;
            font-weight: 600;
          }
          
          p {
            margin: 0;
            color: #7f8c8d;
            font-size: 14px;
            line-height: 1.4;
          }
        }
      }
      
      .panel-actions {
        display: flex;
        gap: 12px;
        align-items: center;
        
        @media (max-width: 768px) {
          flex-wrap: wrap;
        }
      }
    }
  }
  
  .day-schedule {
    .schedule-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
      h4 {
      margin: 0;
      color: #2c3e50;
        font-size: 18px;
        font-weight: 600;
      }
      
      .schedule-summary {
        display: flex;
        gap: 8px;
      }
    }
    
    .schedule-content {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      
      @media (max-width: 768px) {
        grid-template-columns: 1fr;
      }
      
      h5 {
        margin: 0 0 16px 0;
        color: #2c3e50;
        font-size: 16px;
        font-weight: 500;
      }
      
      .appointment-list, .free-slots-list {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      
      .appointment-item {
        display: flex;
        align-items: center;
        padding: 12px;
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        gap: 12px;
        
        .appointment-info {
          flex: 1;
          
          h6 {
            margin: 0 0 4px 0;
            color: #2c3e50;
            font-size: 14px;
            font-weight: 500;
          }
          
          p {
            margin: 0;
            color: #7f8c8d;
            font-size: 12px;
            line-height: 1.4;
          }
        }
      }
      
      .free-slot-item {
        display: flex;
        align-items: center;
        padding: 12px;
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        gap: 12px;
        
        .slot-info {
          flex: 1;
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          .slot-duration {
            color: #2c3e50;
            font-weight: 500;
          }
          
          .slot-type {
            color: #7f8c8d;
            font-size: 12px;
          }
        }
      }
    }
  }
  
  .weekly-schedule {
    .schedule-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      
      h4 {
        margin: 0;
        color: #2c3e50;
        font-size: 18px;
        font-weight: 600;
      }
      
      .schedule-summary {
        display: flex;
        gap: 8px;
      }
    }
    
    .weekly-schedule-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      
      .day-card {
        background: #f8f9fa;
        border-radius: 8px;
        padding: 16px;
        
        .day-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
          
          h6 {
            margin: 0;
            color: #2c3e50;
            font-size: 14px;
            font-weight: 600;
          }
          
          .day-name {
            color: #7f8c8d;
            font-size: 12px;
          }
        }
        
        .day-stats {
          display: flex;
          justify-content: space-between;
          margin-bottom: 12px;
          
          .stat-item {
            text-align: center;
            
            .stat-label {
              display: block;
              color: #7f8c8d;
              font-size: 12px;
              margin-bottom: 4px;
            }
            
            .stat-value {
              color: #2c3e50;
              font-weight: 500;
              font-size: 14px;
            }
          }
        }
        
        .day-availability {
          margin-bottom: 12px;
          
          .availability-bar {
            height: 6px;
            background: #e4e7ed;
            border-radius: 3px;
            overflow: hidden;
            margin-bottom: 4px;
            
            .availability-fill {
              height: 100%;
              background: linear-gradient(90deg, #67c23a, #409eff);
              transition: width 0.3s;
            }
          }
          
          .availability-text {
            color: #7f8c8d;
    font-size: 12px;
          }
        }
        
        .day-actions {
          text-align: center;
        }
      }
    }
  }
  
  .empty-selection {
    background: white;
    border-radius: 12px;
    padding: 60px 20px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  
  .empty-state {
    text-align: center;
    padding: 20px;
  }
}
</style> 