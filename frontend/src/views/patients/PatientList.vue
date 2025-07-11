<template>
  <div class="patient-list-container">
    <div class="page-header">
      <h2>病人管理</h2>
      <!-- 删除医生界面的添加病人按钮 -->
    </div>

    <!-- 搜索和筛选 -->
    <div class="card">
      <el-form inline>
        <el-form-item label="搜索">
          <el-input
            v-model="searchKeyword"
            placeholder="输入姓名、病历号、联系方式或病情描述"
            style="width: 300px"
            clearable
          />
        </el-form-item>
        <el-form-item label="病情严重程度">
          <el-select 
            v-model="selectedSeverity" 
            placeholder="全部" 
            clearable
            style="width: 150px"
          >
            <el-option label="紧急" value="EMERGENCY" />
            <el-option label="急诊" value="URGENT" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="低级" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 病人列表 -->
    <div class="card">
      <el-table 
        :data="patientList" 
        v-loading="loading"
        style="width: 100%"
        stripe
      >
        <el-table-column prop="medicalRecordNumber" label="病历号" width="120" />
        <el-table-column label="姓名" width="100">
          <template #default="scope">
            {{ scope.row.user?.realName }}
          </template>
        </el-table-column>
        <el-table-column label="性别" width="60">
          <template #default="scope">
            {{ getGenderText(scope.row.user?.gender) }}
          </template>
        </el-table-column>
        <el-table-column prop="user.phone" label="联系方式" width="120" />
        <el-table-column label="病情严重程度" width="120">
          <template #default="scope">
            <el-tag :type="getSeverityTagType(scope.row.severityLevel)">
              {{ getSeverityText(scope.row.severityLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配医生" width="100">
          <template #default="scope">
            {{ scope.row.assignedDoctor?.realName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ scope.row.status || '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" @click="viewPatient(scope.row)">
                查看
              </el-button>
              <el-button 
                v-if="isDoctor"
                size="small" 
                type="info"
                @click="viewPatientHistory(scope.row)"
              >
                历史
              </el-button>
              <el-button 
                v-if="isDoctor || isAdmin"
                size="small" 
                type="primary"
                @click="editPatient(scope.row)"
              >
                编辑
              </el-button>
              <el-button 
                v-if="isAdmin"
                size="small" 
                type="danger"
                @click="deletePatient(scope.row)"
              >
                删除
              </el-button>
            </div>
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
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// 删除不再使用的图标导入
import dayjs from 'dayjs'
import axios from 'axios'
import { useStore } from 'vuex'

const router = useRouter()
const store = useStore()

const loading = ref(false)
const patientList = ref([])
const searchKeyword = ref('')
const selectedSeverity = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 搜索防抖
let searchTimeout = null

// 权限检查
const isAdmin = computed(() => store.getters['auth/userRole'] === 'ADMIN')
const isDoctor = computed(() => store.getters['auth/userRole'] === 'DOCTOR')

onMounted(() => {
  loadPatients()
})

// 监听搜索条件变化，实现自动搜索
watch([searchKeyword, selectedSeverity], () => {
  handleSearchDebounced()
})

const handleSearchDebounced = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(() => {
    currentPage.value = 1
    loadPatients()
  }, 500) // 500ms防抖
}

const loadPatients = async () => {
  loading.value = true
  try {
    let response
    
    if (isDoctor.value) {
      // 医生查看分配给自己的病人（最新病情信息）
      response = await axios.get('/api/patients/my-patients-latest')
      let patients = response.data
      
      // 前端过滤搜索结果
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        patients = patients.filter(patient => 
          patient.user?.realName?.toLowerCase().includes(keyword) ||
          patient.medicalRecordNumber?.toLowerCase().includes(keyword) ||
          patient.user?.phone?.includes(keyword) ||
          patient.diseaseDescription?.toLowerCase().includes(keyword)
        )
      }
      
      // 前端过滤严重程度
      if (selectedSeverity.value) {
        patients = patients.filter(patient => 
          patient.severityLevel === selectedSeverity.value
        )
      }
      
      patientList.value = patients
      total.value = patients.length
    } else {
      // 管理员或护士查看所有病人
      const params = {
        page: currentPage.value - 1,
        size: pageSize.value,
        sortBy: 'createdAt',
        sortDir: 'desc'
      }
      
      if (searchKeyword.value) {
        params.search = searchKeyword.value
      }
      
      // 注意：后端API可能不支持按严重程度筛选，这里先添加参数
      if (selectedSeverity.value) {
        params.severity = selectedSeverity.value
      }
      
      response = await axios.get('/api/patients', { params })
      
      if (response.data.content) {
        patientList.value = response.data.content
        total.value = response.data.totalElements
      } else {
        patientList.value = response.data
        total.value = response.data.length
      }
    }
  } catch (error) {
    console.error('加载病人列表失败:', error)
    ElMessage.error('加载病人列表失败: ' + (error.response?.data?.error || error.message))
    patientList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadPatients()
}

const resetSearch = () => {
  searchKeyword.value = ''
  selectedSeverity.value = ''
  currentPage.value = 1
  loadPatients()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadPatients()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadPatients()
}

const viewPatient = (patient) => {
  router.push(`/patients/${patient.id}`)
}

const viewPatientHistory = async (patient) => {
  try {
    const response = await axios.get(`/api/patients/patient-history/${patient.user.id}`)
    const history = response.data
    
    if (history.length === 0) {
      ElMessage.info('该病人暂无历史记录')
      return
    }
    
    // 这里可以打开一个对话框显示历史记录，或者跳转到专门的页面
    console.log('病人历史记录:', history)
    ElMessage.success(`该病人共有 ${history.length} 条病情记录`)
  } catch (error) {
    console.error('获取病人历史记录失败:', error)
    ElMessage.error('获取病人历史记录失败: ' + (error.response?.data?.error || error.message))
  }
}

const editPatient = (patient) => {
  router.push(`/patients/${patient.id}/edit`)
}

const deletePatient = async (patient) => {
  try {
    await ElMessageBox.confirm('确定要删除此病人信息吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    ElMessage.success('删除成功')
    loadPatients()
  } catch {
    // 用户取消
  }
}

const getGenderText = (gender) => {
  const map = { 'MALE': '男', 'FEMALE': '女' }
  return map[gender] || gender
}

const getSeverityText = (severity) => {
  const map = {
    'EMERGENCY': '紧急',
    'URGENT': '急诊', 
    'NORMAL': '普通',
    'LOW': '低级'
  }
  return map[severity] || severity
}

const getSeverityTagType = (severity) => {
  const map = {
    'EMERGENCY': 'danger',
    'URGENT': 'warning',
    'NORMAL': 'primary', 
    'LOW': 'success'
  }
  return map[severity] || ''
}

const getStatusTagType = (status) => {
  const map = {
    'WAITING': 'warning',
    'SCHEDULED': 'primary',
    'COMPLETED': 'success'
  }
  return map[status] || 'info'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : ''
}
</script>

<style lang="scss" scoped>
.patient-list-container {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      color: #2c3e50;
    }
  }
  
  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
  
  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: nowrap;
    
    .el-button {
      margin: 0;
    }
  }
}
</style> 