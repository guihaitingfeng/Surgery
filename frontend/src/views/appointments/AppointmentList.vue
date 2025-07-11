<template>
  <div class="appointment-list-container">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <div class="header-actions">
        <el-button 
          type="success" 
          :icon="Calendar"
          @click="$router.push('/appointments/calendar')"
        >
          日历视图
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

    <!-- 搜索筛选 -->
    <div class="card">
      <el-form inline class="search-form">
        <el-form-item label="病人姓名">
          <el-input
            v-model="searchForm.patientName"
            placeholder="输入病人姓名"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="预约状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="全部状态" 
            clearable
            style="width: 200px"
          >
            <el-option 
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label" 
              :value="option.value" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手术日期">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 预约列表 -->
    <div class="card">
      <el-table 
        :data="appointmentList" 
        v-loading="loading"
        style="width: 100%"
        stripe
      >
        <el-table-column label="病人信息" width="120">
          <template #default="scope">
            <div>{{ scope.row.patient?.user?.realName }}</div>
            <div class="text-gray">{{ scope.row.patient?.medicalRecordNumber }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="surgeryName" label="手术名称" width="150" />
        <el-table-column prop="surgeryType" label="手术类型" width="120" />
        <el-table-column label="主治医生" width="100">
          <template #default="scope">
            {{ scope.row.doctor?.realName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column v-if="isViewingMyAssignments" label="医疗团队" width="120">
          <template #default="scope">
            <div v-if="scope.row.nurse">
              <div class="text-small">护士: {{ scope.row.nurse.realName }}</div>
            </div>
            <div v-if="scope.row.anesthesiologist">
              <div class="text-small">麻醉师: {{ scope.row.anesthesiologist.realName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手术室" width="80">
          <template #default="scope">
            {{ scope.row.room?.roomName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="预约时间" width="150">
          <template #default="scope">
            <div>{{ formatDate(scope.row.plannedDate) }}</div>
            <div class="text-gray">{{ scope.row.plannedStartTime }} - {{ scope.row.plannedEndTime }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预估时长" width="80">
          <template #default="scope">
            {{ scope.row.estimatedDuration ? scope.row.estimatedDuration + '分钟' : '未设置' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewAppointment(scope.row)">
              查看详情
            </el-button>
            <el-button 
              v-if="isDoctor && scope.row.status === 'CANCELLED'"
              size="small" 
              type="danger"
              @click="deleteAppointment(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import axios from 'axios'

const router = useRouter()
const store = useStore()

const isDoctor = computed(() => store.getters['auth/isDoctor'])
const isNurse = computed(() => store.getters['auth/isNurse'])
const isAnesthesiologist = computed(() => store.getters['auth/isAnesthesiologist'])

const loading = ref(false)
const appointmentList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive({
  patientName: '',
  status: '',
  dateRange: []
})

// 检查是否是查看分配给自己的手术
const isViewingMyAssignments = computed(() => {
  return router.currentRoute.value.query.view === 'my-assignments'
})

// 页面标题
const pageTitle = computed(() => {
  if (isViewingMyAssignments.value) {
    if (isNurse.value) return '我参与的手术'
    if (isAnesthesiologist.value) return '我参与的手术'
  }
  return '手术预约管理'
})

// 状态选项列表
const statusOptions = [
  { label: '已安排', value: 'SCHEDULED' },
  { label: '待团队确认', value: 'PENDING_CONFIRMATION' },
  { label: '团队已确认', value: 'TEAM_CONFIRMED' },
  { label: '医生已确认', value: 'DOCTOR_FINAL_CONFIRMED' },
  { label: '已通知病人', value: 'NOTIFIED' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

// 搜索防抖
let searchTimeout = null
const debouncedSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    handleSearch()
  }, 500)
}

// 监听搜索表单变化
watch(() => searchForm.patientName, debouncedSearch)
watch(() => searchForm.status, () => handleSearch())
watch(() => searchForm.dateRange, () => handleSearch())

onMounted(() => {
  loadAppointments()
})

const loadAppointments = async () => {
  loading.value = true
  try {
    let response
    
    if (isViewingMyAssignments.value && (isNurse.value || isAnesthesiologist.value)) {
      // 护士和麻醉师查看分配给自己的手术
      response = await axios.get('/api/appointments/my-staff-appointments')
      
      // 前端过滤数据
      let filteredData = response.data || []
      
      // 按病人姓名过滤
      if (searchForm.patientName) {
        filteredData = filteredData.filter(appointment => 
          appointment.patient?.user?.realName?.includes(searchForm.patientName)
        )
      }
      
      // 按状态过滤
      if (searchForm.status) {
        filteredData = filteredData.filter(appointment => 
          appointment.status === searchForm.status
        )
      }
      
      // 按日期范围过滤
      if (searchForm.dateRange && searchForm.dateRange.length === 2) {
        const [startDate, endDate] = searchForm.dateRange
        filteredData = filteredData.filter(appointment => {
          const appointmentDate = appointment.plannedDate
          return appointmentDate >= startDate && appointmentDate <= endDate
        })
      }
      
      // 分页处理
      const startIndex = (currentPage.value - 1) * pageSize.value
      const endIndex = startIndex + pageSize.value
      appointmentList.value = filteredData.slice(startIndex, endIndex)
      total.value = filteredData.length
      
    } else if (isDoctor.value) {
      // 医生查看自己的预约
      response = await axios.get('/api/appointments/my-appointments')
      
      // 前端过滤数据
      let filteredData = response.data || []
      
      // 按病人姓名过滤
      if (searchForm.patientName) {
        filteredData = filteredData.filter(appointment => 
          appointment.patient?.user?.realName?.includes(searchForm.patientName)
        )
      }
      
      // 按状态过滤
      if (searchForm.status) {
        filteredData = filteredData.filter(appointment => 
          appointment.status === searchForm.status
        )
      }
      
      // 按日期范围过滤
      if (searchForm.dateRange && searchForm.dateRange.length === 2) {
        const [startDate, endDate] = searchForm.dateRange
        filteredData = filteredData.filter(appointment => {
          const appointmentDate = appointment.plannedDate
          return appointmentDate >= startDate && appointmentDate <= endDate
        })
      }
      
      // 分页处理
      const startIndex = (currentPage.value - 1) * pageSize.value
      const endIndex = startIndex + pageSize.value
      appointmentList.value = filteredData.slice(startIndex, endIndex)
      total.value = filteredData.length
      
    } else {
      // 其他角色查看所有预约 - 使用后端分页和筛选
      const params = {
        page: currentPage.value - 1,
        size: pageSize.value
      }
      
      // 添加搜索参数
      if (searchForm.patientName) {
        params.patientName = searchForm.patientName
      }
      if (searchForm.status) {
        params.status = searchForm.status
      }
      if (searchForm.dateRange && searchForm.dateRange.length === 2) {
        params.startDate = searchForm.dateRange[0]
        params.endDate = searchForm.dateRange[1]
      }
      
      response = await axios.get('/api/appointments', { params })
      
      if (response.data.content) {
        // 分页数据
        appointmentList.value = response.data.content || []
        total.value = response.data.totalElements || 0
      } else {
        // 直接数组数据
        appointmentList.value = response.data || []
        total.value = appointmentList.value.length
      }
    }
  } catch (error) {
    console.error('加载预约列表失败:', error)
    ElMessage.error('加载预约列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadAppointments()
}

const resetSearch = () => {
  Object.assign(searchForm, {
    patientName: '',
    status: '',
    dateRange: []
  })
  currentPage.value = 1
  loadAppointments()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadAppointments()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadAppointments()
}

const viewAppointment = (appointment) => {
  router.push(`/appointments/${appointment.id}`)
}

const editAppointment = (appointment) => {
  router.push(`/appointments/${appointment.id}/edit`)
}

const cancelAppointment = async (appointment) => {
  try {
    await ElMessageBox.confirm('确定要取消此预约吗？', '取消确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    ElMessage.success('预约已取消')
    loadAppointments()
  } catch {
    // 用户取消
  }
}

const deleteAppointment = async (appointment) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除此已取消的预约吗？\n病人：${appointment.patient?.user?.realName}\n手术：${appointment.surgeryName}\n删除后无法恢复。`, 
      '删除确认', 
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除API
    await axios.delete(`/api/appointments/${appointment.id}`)
    
    ElMessage.success('预约删除成功')
    loadAppointments()
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else if (error.message !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const getStatusText = (status) => {
  const map = {
    'SCHEDULED': '已安排',
    'PENDING_CONFIRMATION': '待团队确认',
    'TEAM_CONFIRMED': '团队已确认',
    'DOCTOR_FINAL_CONFIRMED': '医生已确认',
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

const getUrgencyText = (urgency) => {
  const map = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高',
    'EMERGENCY': '紧急'
  }
  return map[urgency] || urgency
}

const getUrgencyTagType = (urgency) => {
  const map = {
    'LOW': 'success',
    'MEDIUM': 'primary',
    'HIGH': 'warning',
    'EMERGENCY': 'danger'
  }
  return map[urgency] || 'info'
}

const getPriorityText = (priority) => {
  const map = {
    'EMERGENCY': '紧急',
    'URGENT': '急迫',
    'NORMAL': '普通',
    'LOW': '轻微'
  }
  return map[priority] || priority
}

const getPriorityTagType = (priority) => {
  const map = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary',
    'LOW': 'success'
  }
  return map[priority] || 'info'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD') : ''
}
</script>

<style lang="scss" scoped>
.appointment-list-container {
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
  
  .card {
    background: white;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .search-form {
    .el-form-item {
      margin-bottom: 16px;
      margin-right: 20px;
      
      .el-form-item__label {
        font-weight: 500;
        color: #606266;
      }
    }
    
    .el-select {
      .el-input__inner {
        text-align: left;
      }
    }
  }
  
  .text-gray {
    color: #909399;
    font-size: 12px;
  }
  
  .text-small {
    font-size: 12px;
    color: #606266;
    margin: 2px 0;
  }
  
  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
}
</style> 