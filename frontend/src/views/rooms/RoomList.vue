<template>
  <div class="room-container">
    <div class="page-header">
      <h2>{{ isAdmin ? '手术室管理' : '手术室可用时段' }}</h2>
      <el-button 
        v-if="isAdmin"
        type="primary" 
        :icon="Plus" 
        @click="createRoom"
      >
        添加手术室
      </el-button>
      <div v-else class="date-selector">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择查询日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadRoomAvailability"
        />
      </div>
    </div>

    <!-- 管理员视图：传统的增删改查 -->
    <div v-if="isAdmin" class="admin-view">
    <div class="card">
      <el-table :data="roomList" v-loading="loading" stripe>
        <el-table-column prop="roomName" label="手术室名称" width="150" />
        <el-table-column prop="roomNumber" label="房间号" width="100" />
        <el-table-column prop="floorNumber" label="楼层" width="80" />
        <el-table-column label="设备信息" width="200">
          <template #default="scope">
            {{ scope.row.equipmentList || '无特殊设备' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="床位数" width="80">
          <template #default="scope">
            {{ scope.row.operatingBeds?.length || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" @click="viewRoom(scope.row)">查看</el-button>
              <el-button size="small" type="success" @click="manageBeds(scope.row)">床位</el-button>
              <el-button size="small" type="primary" @click="editRoom(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteRoom(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </div>

    <!-- 医生视图：手术室可用时段 -->
    <div v-else class="doctor-view">
      <div class="availability-grid">
        <div 
          v-for="room in roomList" 
          :key="room.id"
          class="room-card"
          @click="viewRoomAvailability(room)"
        >
          <div class="room-header">
            <h3>{{ room.roomName }}</h3>
            <el-tag :type="getStatusTagType(room.status)" size="small">
              {{ getStatusText(room.status) }}
            </el-tag>
          </div>
          
          <div class="room-info">
            <div class="info-item">
              <span class="label">房间号：</span>
              <span>{{ room.roomNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">楼层：</span>
              <span>{{ room.floorNumber }}楼</span>
            </div>
            <div class="info-item">
              <span class="label">床位数：</span>
              <span>{{ room.operatingBeds?.length || 0 }}张</span>
            </div>
          </div>

          <div v-if="roomAvailability[room.id]" class="availability-summary">
            <div class="summary-item">
              <span class="label">今日手术：</span>
              <span class="value">{{ roomAvailability[room.id].totalAppointments }}台</span>
            </div>
            <div class="summary-item">
              <span class="label">可用床位：</span>
              <span class="value">{{ roomAvailability[room.id].totalAvailableBeds }}/{{ roomAvailability[room.id].totalBeds }}张</span>
            </div>
            <div class="availability-bar">
              <div 
                class="availability-fill"
                :style="{ width: getBedAvailabilityPercentage(roomAvailability[room.id]) + '%' }"
              ></div>
            </div>
          </div>
          
          <div class="room-actions">
            <el-button size="small" type="primary">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 手术室床位可用性详情对话框 -->
    <el-dialog
      v-model="availabilityDialogVisible"
      :title="`${selectedRoom?.roomName} - ${selectedDate} 床位可用性`"
      width="1200px"
    >
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>
      <div v-else-if="selectedRoomAvailability" class="availability-detail">
        <div class="detail-header">
          <div class="room-overview">
            <h4>手术室信息</h4>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="房间号">{{ selectedRoom.roomNumber }}</el-descriptions-item>
              <el-descriptions-item label="楼层">{{ selectedRoom.floorNumber }}楼</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusTagType(selectedRoom.status)" size="small">
                  {{ getStatusText(selectedRoom.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
          
          <div class="availability-stats">
            <el-row :gutter="16">
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-value">{{ selectedRoomAvailability.totalAppointments }}</div>
                  <div class="stat-label">今日手术</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-value">{{ selectedRoomAvailability.totalAvailableBeds }}</div>
                  <div class="stat-label">可用床位</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="stat-card">
                  <div class="stat-value">{{ selectedRoomAvailability.totalBeds }}</div>
                  <div class="stat-label">总床位数</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>

        <div class="detail-content">
          <!-- 床位详细信息 -->
          <div class="section">
            <h5>床位详细可用性</h5>
            <div v-if="selectedRoomAvailability.beds?.length === 0" class="empty-state">
              <el-empty description="该手术室没有床位" :image-size="60" />
            </div>
            <div v-else class="bed-details-grid">
              <div 
                v-for="bedData in selectedRoomAvailability.beds" 
                :key="bedData.bed.id"
                class="bed-detail-card"
              >
                <div class="bed-card-header">
                  <div class="bed-info">
                    <h6>{{ bedData.bed.bedNumber }}</h6>
                    <span class="bed-type">{{ bedData.bed.bedType || '标准床位' }}</span>
                  </div>
                  <el-tag :type="getBedStatusTagType(bedData.bed.status)" size="small">
                    {{ getBedStatusText(bedData.bed.status) }}
                  </el-tag>
                </div>
                
                <div class="bed-card-content">
                  <!-- 今日手术安排 -->
                  <div class="bed-appointments">
                    <h6>今日手术安排</h6>
                    <div v-if="bedData.appointments.length === 0" class="no-appointments">
                      <el-text type="info" size="small">无手术安排</el-text>
                    </div>
                    <div v-else class="appointments-list">
                      <div 
                        v-for="appointment in bedData.appointments" 
                        :key="appointment.id"
                        class="appointment-item"
                      >
                        <div class="appointment-time">
                          <el-tag type="primary" size="small">
                            {{ appointment.plannedStartTime }} - {{ appointment.plannedEndTime }}
                          </el-tag>
                        </div>
                        <div class="appointment-info">
                          <div class="surgery-name">{{ appointment.surgeryName }}</div>
                          <div class="doctor-name">{{ appointment.doctor?.realName }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 空闲时段 -->
                  <div class="bed-free-slots">
                    <h6>空闲时段</h6>
                    <div v-if="bedData.freeSlots.length === 0" class="no-free-slots">
                      <el-text type="warning" size="small">无空闲时段</el-text>
                    </div>
                    <div v-else class="free-slots-list">
                      <div 
                        v-for="(slot, index) in bedData.freeSlots" 
                        :key="index"
                        class="free-slot-item"
                      >
                        <el-tag type="success" size="small">
                          {{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}
                        </el-tag>
                        <span class="slot-duration">{{ slot.duration }}分钟</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="debug-info">
        <p>调试信息：</p>
        <p>selectedRoom: {{ selectedRoom?.roomName }}</p>
        <p>selectedRoomAvailability: {{ selectedRoomAvailability }}</p>
        <p>roomAvailability: {{ roomAvailability }}</p>
      </div>
    </el-dialog>

    <!-- 管理员的对话框保持原样 -->
    <div v-if="isAdmin">
    <!-- 添加/编辑手术室对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="roomFormRef"
        :model="roomForm"
        :rules="roomRules"
        label-width="100px"
      >
        <el-form-item label="房间号" prop="roomNumber">
          <el-input v-model="roomForm.roomNumber" placeholder="请输入房间号" />
        </el-form-item>
        
        <el-form-item label="手术室名称" prop="roomName">
          <el-input v-model="roomForm.roomName" placeholder="请输入手术室名称" />
        </el-form-item>
        
        <el-form-item label="楼层" prop="floorNumber">
          <el-input-number v-model="roomForm.floorNumber" :min="1" :max="20" />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-select v-model="roomForm.status" placeholder="请选择状态">
            <el-option label="可用" value="AVAILABLE" />
            <el-option label="占用中" value="OCCUPIED" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="预留" value="RESERVED" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="设备信息" prop="equipmentList">
          <el-input
            v-model="roomForm.equipmentList"
            type="textarea"
            :rows="3"
            placeholder="请输入设备信息"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看手术室详情对话框 -->
    <el-dialog
      title="手术室详情"
      v-model="viewDialogVisible"
      width="500px"
    >
      <div v-if="selectedRoom" class="room-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="房间号">{{ selectedRoom.roomNumber }}</el-descriptions-item>
          <el-descriptions-item label="手术室名称">{{ selectedRoom.roomName }}</el-descriptions-item>
          <el-descriptions-item label="楼层">{{ selectedRoom.floorNumber }}</el-descriptions-item>
          <el-descriptions-item label="床位数">{{ selectedRoom.operatingBeds?.length || 0 }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedRoom.status)">
              {{ getStatusText(selectedRoom.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备信息" :span="2">
            {{ selectedRoom.equipmentList || '无特殊设备' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 床位管理对话框 -->
    <el-dialog
      title="床位管理"
      v-model="bedDialogVisible"
      width="800px"
      @close="resetBedForm"
    >
      <div class="bed-management">
        <div class="bed-header">
          <h4>{{ selectedRoom?.roomName }} - 床位列表</h4>
          <el-button type="primary" size="small" @click="createBed">
            <el-icon><Plus /></el-icon>
            添加床位
          </el-button>
        </div>
        
        <el-table :data="bedList" v-loading="bedLoading" stripe>
          <el-table-column prop="bedNumber" label="床位号" width="100" />
          <el-table-column prop="bedType" label="床位类型" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getBedStatusTagType(scope.row.status)">
                {{ getBedStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <div class="action-buttons">
                <el-button size="small" type="primary" @click="editBed(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteBed(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 添加/编辑床位表单 -->
      <el-dialog
        :title="bedFormTitle"
        v-model="bedFormVisible"
        width="400px"
        append-to-body
        @close="resetBedForm"
      >
        <el-form
          ref="bedFormRef"
          :model="bedForm"
          :rules="bedRules"
          label-width="80px"
        >
          <el-form-item label="床位号" prop="bedNumber">
            <el-input v-model="bedForm.bedNumber" placeholder="请输入床位号" />
          </el-form-item>
          
          <el-form-item label="床位类型" prop="bedType">
            <el-select v-model="bedForm.bedType" placeholder="请选择床位类型">
              <el-option label="普通床位" value="NORMAL" />
              <el-option label="重症床位" value="ICU" />
              <el-option label="手术床位" value="SURGERY" />
              <el-option label="观察床位" value="OBSERVATION" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="bedForm.status" placeholder="请选择状态">
              <el-option label="可用" value="AVAILABLE" />
              <el-option label="占用中" value="OCCUPIED" />
              <el-option label="维护中" value="MAINTENANCE" />
            </el-select>
          </el-form-item>
        </el-form>
        
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="bedFormVisible = false">取消</el-button>
            <el-button type="primary" @click="submitBedForm" :loading="bedSubmitting">
              {{ isBedEdit ? '更新' : '创建' }}
            </el-button>
          </div>
        </template>
      </el-dialog>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const store = useStore()
const loading = ref(false)
const submitting = ref(false)
const roomList = ref([])
const selectedDate = ref(new Date().toISOString().split('T')[0])
const roomAvailability = ref({})
const availabilityDialogVisible = ref(false)
const selectedRoom = ref(null)
const selectedRoomAvailability = ref(null)

// 管理员功能相关状态
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const bedDialogVisible = ref(false)
const bedFormVisible = ref(false)
const isEdit = ref(false)
const roomFormRef = ref()

// 床位相关状态
const bedLoading = ref(false)
const bedSubmitting = ref(false)
const bedList = ref([])
const isBedEdit = ref(false)
const bedFormRef = ref()

const roomForm = ref({
  roomNumber: '',
  roomName: '',
  floorNumber: 1,
  status: 'AVAILABLE',
  equipmentList: ''
})

const bedForm = ref({
  bedNumber: '',
  bedType: 'NORMAL',
  status: 'AVAILABLE'
})

const roomRules = {
  roomNumber: [
    { required: true, message: '请输入房间号', trigger: 'blur' },
    { min: 1, max: 20, message: '房间号长度应为1-20个字符', trigger: 'blur' }
  ],
  roomName: [
    { required: true, message: '请输入手术室名称', trigger: 'blur' },
    { min: 2, max: 100, message: '手术室名称长度应为2-100个字符', trigger: 'blur' }
  ],
  floorNumber: [
    { required: true, message: '请选择楼层', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const bedRules = {
  bedNumber: [
    { required: true, message: '请输入床位号', trigger: 'blur' },
    { min: 1, max: 20, message: '床位号长度应为1-20个字符', trigger: 'blur' }
  ],
  bedType: [
    { required: true, message: '请选择床位类型', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const isAdmin = computed(() => store.getters['auth/userRole'] === 'ADMIN')
const dialogTitle = computed(() => isEdit.value ? '编辑手术室' : '添加手术室')
const bedFormTitle = computed(() => isBedEdit.value ? '编辑床位' : '添加床位')

onMounted(async () => {
  await loadRooms()
  if (!isAdmin.value) {
    loadRoomAvailability()
  }
})

// 监听日期变化
watch(selectedDate, () => {
  if (!isAdmin.value) {
    roomAvailability.value = {} // 清空之前的数据
    loadRoomAvailability()
  }
})

const loadRooms = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/rooms')
    roomList.value = response.data.content || []
  } catch (error) {
    ElMessage.error('加载手术室数据失败')
    console.error('Load rooms error:', error)
  } finally {
    loading.value = false
  }
}

const loadRoomAvailability = async () => {
  if (isAdmin.value || !selectedDate.value) return
  
  console.log('Loading room bed availability for date:', selectedDate.value)
  console.log('Room list:', roomList.value)
  
  try {
    // 为每个手术室加载床位可用性数据
    const availabilityPromises = roomList.value.map(async (room) => {
      try {
        const response = await axios.get(`/api/appointments/room/${room.id}/beds/availability/${selectedDate.value}`)
        const bedAvailabilities = response.data || []
        
        console.log(`Room ${room.id} bed availabilities:`, bedAvailabilities)
        
        // 计算手术室统计信息
        let totalAppointments = 0
        let totalAvailableBeds = 0
        
        const bedsWithAvailability = bedAvailabilities.map(bedData => {
          const bed = bedData.bed
          const appointments = bedData.appointments || []
          const freeSlots = calculateBedFreeSlots(appointments)
          
          totalAppointments += appointments.length
          if (freeSlots.length > 0) {
            totalAvailableBeds++
          }
          
          return {
            bed,
            appointments,
            freeSlots,
            totalAppointments: appointments.length
          }
        })
        
        return {
          roomId: room.id,
          beds: bedsWithAvailability,
          totalAppointments,
          totalAvailableBeds,
          totalBeds: bedsWithAvailability.length
        }
      } catch (error) {
        console.error(`获取手术室 ${room.id} 床位可用性失败:`, error)
        return {
          roomId: room.id,
          beds: [],
          totalAppointments: 0,
          totalAvailableBeds: 0,
          totalBeds: 0
        }
      }
    })
    
    const results = await Promise.all(availabilityPromises)
    const availabilityMap = {}
    results.forEach(result => {
      availabilityMap[result.roomId] = result
    })
    roomAvailability.value = availabilityMap
    console.log('Final room bed availability:', roomAvailability.value)
  } catch (error) {
    console.error('加载手术室床位可用性失败:', error)
    ElMessage.error('加载手术室床位可用性失败')
  }
}

const calculateBedFreeSlots = (appointments) => {
  const freeSlots = []
  const workStartTime = { hour: 8, minute: 0 }
  const workEndTime = { hour: 18, minute: 0 }
  
  if (appointments.length === 0) {
    freeSlots.push({
      startTime: workStartTime,
      endTime: workEndTime,
      duration: 600,
      type: '全天空闲'
    })
    return freeSlots
  }
  
  // 按开始时间排序
  const sortedAppointments = appointments.sort((a, b) => {
    return a.plannedStartTime.localeCompare(b.plannedStartTime)
  })
  
  let currentTime = workStartTime
  
  sortedAppointments.forEach(appointment => {
    const appointmentStart = parseTime(appointment.plannedStartTime)
    const appointmentEnd = parseTime(appointment.plannedEndTime)
    
    // 检查当前时间到预约开始时间之间是否有空闲
    if (timeToMinutes(currentTime) < timeToMinutes(appointmentStart)) {
      freeSlots.push({
        startTime: currentTime,
        endTime: appointmentStart,
        duration: timeToMinutes(appointmentStart) - timeToMinutes(currentTime),
        type: '空闲'
      })
    }
    
    // 更新当前时间
    currentTime = appointmentEnd
  })
  
  // 检查最后一个预约后是否还有空闲时间
  if (timeToMinutes(currentTime) < timeToMinutes(workEndTime)) {
    freeSlots.push({
      startTime: currentTime,
      endTime: workEndTime,
      duration: timeToMinutes(workEndTime) - timeToMinutes(currentTime),
      type: '空闲'
    })
  }
  
  return freeSlots
}

// 保留原有的calculateFreeSlots函数用于向后兼容
const calculateFreeSlots = calculateBedFreeSlots

const parseTime = (timeString) => {
  const [hour, minute] = timeString.split(':').map(Number)
  return { hour, minute }
}

const timeToMinutes = (time) => {
  return time.hour * 60 + time.minute
}

const formatTime = (time) => {
  if (typeof time === 'string') return time
  if (time && typeof time === 'object') {
    return `${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}`
  }
  return time
}

const getBedAvailabilityPercentage = (availability) => {
  if (!availability || availability.totalBeds === 0) return 0
  return (availability.totalAvailableBeds / availability.totalBeds) * 100
}

// 保留原有函数用于向后兼容
const getAvailabilityPercentage = (availability) => {
  if (!availability || !availability.freeSlots) return 0
  const totalFreeMinutes = availability.freeSlots.reduce((sum, slot) => sum + slot.duration, 0)
  const workDayMinutes = 600 // 10小时工作日
  return Math.min((totalFreeMinutes / workDayMinutes) * 100, 100)
}

const viewRoomAvailability = async (room) => {
  selectedRoom.value = room
  availabilityDialogVisible.value = true
  
  // 如果没有当前房间的床位可用性数据，则重新加载
  if (!roomAvailability.value[room.id]) {
    loading.value = true
    try {
      const response = await axios.get(`/api/appointments/room/${room.id}/beds/availability/${selectedDate.value}`)
      const bedAvailabilities = response.data || []
      
      console.log(`获取到手术室 ${room.id} 的床位可用性数据:`, bedAvailabilities)
      
      // 计算手术室统计信息
      let totalAppointments = 0
      let totalAvailableBeds = 0
      
      const bedsWithAvailability = bedAvailabilities.map(bedData => {
        const bed = bedData.bed
        const appointments = bedData.appointments || []
        const freeSlots = calculateBedFreeSlots(appointments)
        
        totalAppointments += appointments.length
        if (freeSlots.length > 0) {
          totalAvailableBeds++
        }
        
        return {
          bed,
          appointments,
          freeSlots,
          totalAppointments: appointments.length
        }
      })
      
      const availabilityData = {
        roomId: room.id,
        beds: bedsWithAvailability,
        totalAppointments,
        totalAvailableBeds,
        totalBeds: bedsWithAvailability.length
      }
      
      // 更新可用性数据
      roomAvailability.value[room.id] = availabilityData
      selectedRoomAvailability.value = availabilityData
    } catch (error) {
      console.error(`获取手术室 ${room.id} 床位可用性失败:`, error)
      ElMessage.error('获取手术室床位可用性失败')
      selectedRoomAvailability.value = null
    } finally {
      loading.value = false
    }
  } else {
    selectedRoomAvailability.value = roomAvailability.value[room.id]
  }
  
  console.log('Selected room bed availability:', selectedRoomAvailability.value)
}

// 床位详细可用性已在详情对话框中展示

// 管理员功能保持不变
const loadBeds = async (roomId) => {
  bedLoading.value = true
  try {
    const response = await axios.get(`/api/rooms/${roomId}/beds`)
    bedList.value = response.data || []
  } catch (error) {
    ElMessage.error('加载床位数据失败')
    console.error('Load beds error:', error)
  } finally {
    bedLoading.value = false
  }
}

const createRoom = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const editRoom = (room) => {
  isEdit.value = true
  dialogVisible.value = true
  roomForm.value = { ...room }
}

const viewRoom = (room) => {
  selectedRoom.value = room
  viewDialogVisible.value = true
}

const manageBeds = (room) => {
  selectedRoom.value = room
  bedDialogVisible.value = true
  loadBeds(room.id)
}

const createBed = () => {
  isBedEdit.value = false
  bedFormVisible.value = true
  resetBedForm()
}

const editBed = (bed) => {
  isBedEdit.value = true
  bedFormVisible.value = true
  bedForm.value = { ...bed }
}

const deleteRoom = async (room) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除手术室 "${room.roomName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/rooms/${room.id}`)
    ElMessage.success('删除成功')
    loadRooms()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('Delete room error:', error)
    }
  }
}

const deleteBed = async (bed) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除床位 "${bed.bedNumber}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/rooms/${selectedRoom.value.id}/beds/${bed.id}`)
    ElMessage.success('删除成功')
    loadBeds(selectedRoom.value.id)
    loadRooms()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('Delete bed error:', error)
    }
  }
}

const submitForm = async () => {
  if (!roomFormRef.value) return
  
  try {
    const valid = await roomFormRef.value.validate()
    if (!valid) return
    
    submitting.value = true
    
    if (isEdit.value) {
      await axios.put(`/api/rooms/${roomForm.value.id}`, roomForm.value)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/rooms', roomForm.value)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadRooms()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    console.error('Submit form error:', error)
  } finally {
    submitting.value = false
  }
}

const submitBedForm = async () => {
  if (!bedFormRef.value) return
  
  try {
    const valid = await bedFormRef.value.validate()
    if (!valid) return
    
    bedSubmitting.value = true
    
    if (isBedEdit.value) {
      await axios.put(`/api/rooms/${selectedRoom.value.id}/beds/${bedForm.value.id}`, bedForm.value)
      ElMessage.success('更新成功')
    } else {
      await axios.post(`/api/rooms/${selectedRoom.value.id}/beds`, bedForm.value)
      ElMessage.success('创建成功')
    }
    
    bedFormVisible.value = false
    loadBeds(selectedRoom.value.id)
    loadRooms()
  } catch (error) {
    ElMessage.error(isBedEdit.value ? '更新失败' : '创建失败')
    console.error('Submit bed form error:', error)
  } finally {
    bedSubmitting.value = false
  }
}

const resetForm = () => {
  roomForm.value = {
    roomNumber: '',
    roomName: '',
    floorNumber: 1,
    status: 'AVAILABLE',
    equipmentList: ''
  }
  if (roomFormRef.value) {
    roomFormRef.value.clearValidate()
  }
}

const resetBedForm = () => {
  bedForm.value = {
    bedNumber: '',
    bedType: 'NORMAL',
    status: 'AVAILABLE'
  }
  if (bedFormRef.value) {
    bedFormRef.value.clearValidate()
  }
}

const getStatusText = (status) => {
  const map = { 
    'AVAILABLE': '可用', 
    'OCCUPIED': '占用中', 
    'MAINTENANCE': '维护中',
    'RESERVED': '预留'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { 
    'AVAILABLE': 'success', 
    'OCCUPIED': 'warning', 
    'MAINTENANCE': 'danger',
    'RESERVED': 'info'
  }
  return map[status] || 'info'
}

const getBedStatusText = (status) => {
  const map = { 
    'AVAILABLE': '可用', 
    'OCCUPIED': '占用中', 
    'MAINTENANCE': '维护中'
  }
  return map[status] || status
}

const getBedStatusTagType = (status) => {
  const map = { 
    'AVAILABLE': 'success', 
    'OCCUPIED': 'warning', 
    'MAINTENANCE': 'danger'
  }
  return map[status] || 'info'
}
</script>

<style lang="scss" scoped>
.room-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
      font-size: 24px;
      font-weight: 600;
    }
    
    .date-selector {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }
  
  // 医生视图样式
  .doctor-view {
    .availability-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 20px;
      
      .room-card {
    background: white;
        border-radius: 12px;
    padding: 20px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        cursor: pointer;
        transition: all 0.3s;
        
        &:hover {
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
          transform: translateY(-2px);
        }
        
        .room-header {
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
        
        .room-info {
          margin-bottom: 16px;
          
          .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            
            .label {
              color: #7f8c8d;
              font-size: 14px;
            }
          }
        }
        
        .availability-summary {
          margin-bottom: 16px;
          
          .summary-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            
            .label {
              color: #7f8c8d;
              font-size: 14px;
            }
            
            .value {
              font-weight: 500;
              color: #2c3e50;
            }
          }
          
          .availability-bar {
            height: 6px;
            background: #e4e7ed;
            border-radius: 3px;
            overflow: hidden;
            margin-top: 8px;
            
            .availability-fill {
              height: 100%;
              background: linear-gradient(90deg, #67c23a, #409eff);
              transition: width 0.3s;
            }
          }
        }
        
        .room-actions {
          text-align: center;
        }
      }
    }
  }
  
  // 可用时段详情对话框样式
  .availability-detail {
    .detail-header {
      margin-bottom: 24px;
      
      .room-overview {
        margin-bottom: 16px;
        
        h4 {
          margin: 0 0 12px 0;
          color: #2c3e50;
          font-size: 16px;
          font-weight: 600;
        }
      }
      
      .availability-stats {
        .stat-card {
          text-align: center;
          padding: 16px;
          background: #f8f9fa;
          border-radius: 8px;
          
          .stat-value {
            font-size: 24px;
            font-weight: 600;
            color: #409eff;
            margin-bottom: 4px;
          }
          
          .stat-label {
            color: #7f8c8d;
            font-size: 14px;
          }
        }
      }
    }
    
    .detail-content {
      .section {
        margin-bottom: 24px;
        
        h5 {
          margin: 0 0 16px 0;
          color: #2c3e50;
          font-size: 16px;
          font-weight: 500;
          border-bottom: 1px solid #e4e7ed;
          padding-bottom: 8px;
        }
        
        .appointment-timeline {
          .timeline-item {
            display: flex;
            gap: 12px;
            margin-bottom: 16px;
            padding: 12px;
            border: 1px solid #e4e7ed;
            border-radius: 8px;
            
            .timeline-time {
              flex-shrink: 0;
            }
            
            .timeline-content {
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
        }
        
        .free-slots-list {
          .free-slot-item {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 12px;
            padding: 12px;
            border: 1px solid #e4e7ed;
            border-radius: 8px;
            
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
        
        .bed-availability-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          gap: 12px;
          
          .bed-card {
            padding: 16px;
            border: 1px solid #e4e7ed;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
            
            &:hover {
              border-color: #409eff;
              box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
            }
            
            .bed-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;
              
              .bed-number {
                font-weight: 600;
                color: #2c3e50;
              }
            }
            
            .bed-type {
              color: #7f8c8d;
              font-size: 12px;
              margin-bottom: 8px;
            }
            
            .bed-availability {
              .availability-text {
                color: #409eff;
                font-size: 12px;
              }
            }
          }
        }
      }
    }
  }
  
  // 管理员视图样式保持不变
  .admin-view {
    .card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }
    
    // 操作按钮样式
    .action-buttons {
      display: flex;
      gap: 8px;
      flex-wrap: nowrap;
      align-items: center;
      
      .el-button {
        margin: 0;
        flex-shrink: 0;
      }
    }
  }
  
  .empty-state {
    text-align: center;
    padding: 20px;
}

.bed-management {
  .bed-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
      margin-bottom: 16px;
    
    h4 {
      margin: 0;
      color: #2c3e50;
      }
    }
  }
  
  // 床位详细信息样式
  .bed-details-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
    gap: 20px;
    
    .bed-detail-card {
      background: white;
      border: 1px solid #e4e7ed;
      border-radius: 8px;
      padding: 16px;
      
      .bed-card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #f0f2f5;
        
        .bed-info {
          h6 {
            margin: 0 0 4px 0;
            font-size: 16px;
            font-weight: 600;
            color: #2c3e50;
          }
          
          .bed-type {
            color: #7f8c8d;
            font-size: 14px;
          }
        }
      }
      
      .bed-card-content {
        .bed-appointments, .bed-free-slots {
          margin-bottom: 16px;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          h6 {
            margin: 0 0 8px 0;
            font-size: 14px;
            font-weight: 500;
            color: #2c3e50;
          }
          
          .no-appointments, .no-free-slots {
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 4px;
            text-align: center;
          }
        }
        
        .appointments-list {
          .appointment-item {
            display: flex;
            gap: 12px;
            padding: 8px 12px;
            background: #f8f9fa;
            border-radius: 4px;
            margin-bottom: 8px;
            
            &:last-child {
              margin-bottom: 0;
            }
            
            .appointment-time {
              flex-shrink: 0;
            }
            
            .appointment-info {
              flex: 1;
              
              .surgery-name {
                font-weight: 500;
                color: #2c3e50;
                font-size: 14px;
                margin-bottom: 2px;
              }
              
              .doctor-name {
                color: #7f8c8d;
                font-size: 12px;
              }
            }
          }
        }
        
        .free-slots-list {
          .free-slot-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 6px 12px;
            background: #f0f9ff;
            border-radius: 4px;
            margin-bottom: 6px;
            
            &:last-child {
              margin-bottom: 0;
            }
            
            .slot-duration {
              color: #7f8c8d;
              font-size: 12px;
            }
          }
        }
      }
    }
  }
}
</style> 