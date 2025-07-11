<!--<template>-->
<!--  <div class="report-container">-->
<!--    <div class="page-header">-->
<!--      <h2>手术汇总报告</h2>-->
<!--      <el-button type="primary" :icon="Download" @click="exportReport">-->
<!--        导出报告-->
<!--      </el-button>-->
<!--    </div>-->

<!--    &lt;!&ndash; 统计卡片 &ndash;&gt;-->
<!--    <el-row :gutter="20" class="stats-row">-->
<!--      <el-col :span="6">-->
<!--        <div class="stat-card">-->
<!--          <div class="stat-number">{{ stats.totalSurgeries }}</div>-->
<!--          <div class="stat-label">总手术数</div>-->
<!--        </div>-->
<!--      </el-col>-->
<!--      <el-col :span="6">-->
<!--        <div class="stat-card">-->
<!--          <div class="stat-number">{{ stats.completedSurgeries }}</div>-->
<!--          <div class="stat-label">已完成</div>-->
<!--        </div>-->
<!--      </el-col>-->
<!--      <el-col :span="6">-->
<!--        <div class="stat-card">-->
<!--          <div class="stat-number">{{ stats.pendingSurgeries }}</div>-->
<!--          <div class="stat-label">待处理</div>-->
<!--        </div>-->
<!--      </el-col>-->
<!--      <el-col :span="6">-->
<!--        <div class="stat-card">-->
<!--          <div class="stat-number">{{ stats.cancelledSurgeries }}</div>-->
<!--          <div class="stat-label">已取消</div>-->
<!--        </div>-->
<!--      </el-col>-->
<!--    </el-row>-->

<!--    &lt;!&ndash; 筛选器 &ndash;&gt;-->
<!--    <div class="card">-->
<!--      <el-form inline>-->
<!--        <el-form-item label="时间范围">-->
<!--          <el-date-picker-->
<!--            v-model="dateRange"-->
<!--            type="daterange"-->
<!--            range-separator="至"-->
<!--            start-placeholder="开始日期"-->
<!--            end-placeholder="结束日期"-->
<!--            format="YYYY-MM-DD"-->
<!--            value-format="YYYY-MM-DD"-->
<!--            @change="loadReports"-->
<!--          />-->
<!--        </el-form-item>-->
<!--        <el-form-item label="手术类型">-->
<!--          <el-select v-model="selectedType" placeholder="全部类型" clearable @change="loadReports">-->
<!--            <el-option label="心脏手术" value="CARDIAC" />-->
<!--            <el-option label="神经手术" value="NEUROLOGICAL" />-->
<!--            <el-option label="骨科手术" value="ORTHOPEDIC" />-->
<!--          </el-select>-->
<!--        </el-form-item>-->
<!--      </el-form>-->
<!--    </div>-->

<!--    &lt;!&ndash; 报告列表 &ndash;&gt;-->
<!--    <div class="card">-->
<!--      <el-table :data="reportList" v-loading="loading" stripe>-->
<!--        <el-table-column prop="date" label="日期" width="120" />-->
<!--        <el-table-column prop="surgeryType" label="手术类型" width="120" />-->
<!--        <el-table-column prop="patientName" label="病人姓名" width="100" />-->
<!--        <el-table-column prop="doctorName" label="主治医生" width="100" />-->
<!--        <el-table-column prop="duration" label="手术时长" width="100" />-->
<!--        <el-table-column label="状态" width="100">-->
<!--          <template #default="scope">-->
<!--            <el-tag :type="getStatusTagType(scope.row.status)">-->
<!--              {{ getStatusText(scope.row.status) }}-->
<!--            </el-tag>-->
<!--          </template>-->
<!--        </el-table-column>-->
<!--        <el-table-column prop="notes" label="备注" min-width="200" />-->
<!--      </el-table>-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, reactive, onMounted } from 'vue'-->
<!--import { ElMessage } from 'element-plus'-->
<!--import { Download } from '@element-plus/icons-vue'-->

<!--const loading = ref(false)-->
<!--const reportList = ref([])-->
<!--const dateRange = ref([])-->
<!--const selectedType = ref('')-->

<!--const stats = reactive({-->
<!--  totalSurgeries: 0,-->
<!--  completedSurgeries: 0,-->
<!--  pendingSurgeries: 0,-->
<!--  cancelledSurgeries: 0-->
<!--})-->

<!--onMounted(() => {-->
<!--  loadReports()-->
<!--  loadStats()-->
<!--})-->

<!--const loadReports = async () => {-->
<!--  loading.value = true-->
<!--  try {-->
<!--    reportList.value = []-->
<!--  } catch (error) {-->
<!--    ElMessage.error('加载报告数据失败')-->
<!--  } finally {-->
<!--    loading.value = false-->
<!--  }-->
<!--}-->

<!--const loadStats = async () => {-->
<!--  try {-->
<!--    Object.assign(stats, {-->
<!--      totalSurgeries: 0,-->
<!--      completedSurgeries: 0,-->
<!--      pendingSurgeries: 0,-->
<!--      cancelledSurgeries: 0-->
<!--    })-->
<!--  } catch (error) {-->
<!--    ElMessage.error('加载统计数据失败')-->
<!--  }-->
<!--}-->

<!--const exportReport = () => {-->
<!--  ElMessage.success('报告导出功能开发中...')-->
<!--}-->

<!--const getStatusText = (status) => {-->
<!--  const map = { 'COMPLETED': '已完成', 'PENDING': '待处理', 'CANCELLED': '已取消' }-->
<!--  return map[status] || status-->
<!--}-->

<!--const getStatusTagType = (status) => {-->
<!--  const map = { 'COMPLETED': 'success', 'PENDING': 'warning', 'CANCELLED': 'danger' }-->
<!--  return map[status] || 'info'-->
<!--}-->
<!--</script>-->

<!--<style lang="scss" scoped>-->
<!--.report-container {-->
<!--  .page-header {-->
<!--    display: flex;-->
<!--    justify-content: space-between;-->
<!--    align-items: center;-->
<!--    margin-bottom: 20px;-->
<!--    -->
<!--    h2 {-->
<!--      margin: 0;-->
<!--      color: #2c3e50;-->
<!--    }-->
<!--  }-->

<!--  .stats-row {-->
<!--    margin-bottom: 20px;-->
<!--  }-->

<!--  .stat-card {-->
<!--    background: white;-->
<!--    padding: 20px;-->
<!--    border-radius: 8px;-->
<!--    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);-->
<!--    text-align: center;-->
<!--    -->
<!--    .stat-number {-->
<!--      font-size: 32px;-->
<!--      font-weight: bold;-->
<!--      color: #409eff;-->
<!--      margin-bottom: 8px;-->
<!--    }-->
<!--    -->
<!--    .stat-label {-->
<!--      color: #606266;-->
<!--      font-size: 14px;-->
<!--    }-->
<!--  }-->
<!--}-->
<!--</style>-->