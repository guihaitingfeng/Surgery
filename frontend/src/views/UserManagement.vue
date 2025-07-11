<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
      <p>管理系统中的所有用户账户</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="search-section">
        <div class="search-controls">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名、真实姓名或邮箱"
            class="search-input"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select
            v-model="selectedRole"
            placeholder="选择角色"
            clearable
            @change="handleRoleFilter"
            class="role-filter"
          >
            <el-option label="全部角色" value="" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="病人" value="PATIENT" />
            <el-option label="护士" value="NURSE" />
            <el-option label="麻醉师" value="ANESTHESIOLOGIST" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </div>
      </div>

      <div class="action-buttons">
        <!-- 删除添加用户按钮 -->
        <el-button @click="refreshUserList" :icon="Refresh">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 用户列表 -->
    <div class="user-table">
      <el-table
        :data="filteredUsers"
        v-loading="loading"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column prop="id" label="ID" width="80" />
        
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="scope">
            <el-tag type="info" size="small">{{ scope.row.username }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="realName" label="真实姓名" width="120" />
        
        <el-table-column prop="email" label="邮箱" width="200" />
        
        <el-table-column prop="phone" label="手机号" width="130" />
        
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)" size="small">
              {{ getRoleText(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="lastLoginAt" label="最后登录" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.lastLoginAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'danger'" size="small">
              {{ scope.row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button
                type="primary"
                size="small"
                @click="editUser(scope.row)"
                :icon="Edit"
                class="action-btn"
                plain
              >
                编辑
              </el-button>
              <el-button
                :type="scope.row.enabled ? 'warning' : 'success'"
                size="small"
                @click="toggleUserStatus(scope.row)"
                class="action-btn"
                plain
              >
                {{ scope.row.enabled ? '禁用' : '启用' }}
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="deleteUser(scope.row)"
                :icon="Delete"
                class="action-btn"
                plain
                v-if="scope.row.role !== 'ADMIN' || scope.row.id !== currentUser?.id"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalUsers"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEditMode ? '编辑用户' : '添加用户'"
      width="600px"
      @close="resetUserForm"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userFormRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            placeholder="请输入用户名"
            :disabled="isEditMode"
          />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="!isEditMode">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="医生" value="DOCTOR" />
            <el-option label="病人" value="PATIENT" />
            <el-option label="护士" value="NURSE" />
            <el-option label="麻醉师" value="ANESTHESIOLOGIST" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态" prop="enabled">
          <el-switch
            v-model="userForm.enabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="userDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser" :loading="saving">
            {{ isEditMode ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Edit, Delete
} from '@element-plus/icons-vue'
import axios from 'axios'
import dayjs from 'dayjs'

const store = useStore()

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const users = ref([])
const searchKeyword = ref('')
const selectedRole = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const totalUsers = ref(0)
const selectedUsers = ref([])

// 对话框相关
const userDialogVisible = ref(false)
const isEditMode = ref(false)
const userFormRef = ref()

// 用户表单
const userForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  role: '',
  enabled: true
})

// 表单验证规则
const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3456789]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 计算属性
const currentUser = computed(() => store.getters['auth/user'])

const filteredUsers = computed(() => {
  let filtered = users.value

  // 角色筛选
  if (selectedRole.value) {
    filtered = filtered.filter(user => user.role === selectedRole.value)
  }

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(user => 
      user.username.toLowerCase().includes(keyword) ||
      user.realName?.toLowerCase().includes(keyword) ||
      user.email?.toLowerCase().includes(keyword)
    )
  }

  return filtered
})

// 生命周期
onMounted(() => {
  loadUsers()
})

// 方法
const loadUsers = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/users', {
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    
    if (response.data.content) {
      users.value = response.data.content
      totalUsers.value = response.data.totalElements
    } else {
      users.value = response.data
      totalUsers.value = response.data.length
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const refreshUserList = () => {
  loadUsers()
}

const handleSearch = () => {
  // 搜索是通过计算属性实现的，这里可以添加防抖逻辑
}

const handleRoleFilter = () => {
  // 筛选是通过计算属性实现的
}

const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadUsers()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadUsers()
}

const showAddUserDialog = () => {
  isEditMode.value = false
  userDialogVisible.value = true
  resetUserForm()
}

const editUser = (user) => {
  isEditMode.value = true
  userDialogVisible.value = true
  
  // 填充表单数据
  Object.keys(userForm).forEach(key => {
    if (key !== 'password') {
      userForm[key] = user[key]
    }
  })
}

const resetUserForm = () => {
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
  
  Object.keys(userForm).forEach(key => {
    if (key === 'enabled') {
      userForm[key] = true
    } else {
      userForm[key] = key === 'id' ? null : ''
    }
  })
}

const saveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    
    saving.value = true
    
    const userData = { ...userForm }
    if (isEditMode.value) {
      delete userData.password // 编辑时不发送密码
    }
    
    if (isEditMode.value) {
      await axios.put(`/api/users/${userData.id}`, userData)
      ElMessage.success('用户更新成功')
    } else {
      await axios.post('/api/users', userData)
      ElMessage.success('用户创建成功')
    }
    
    userDialogVisible.value = false
    loadUsers()
  } catch (error) {
    if (error.response?.data?.error) {
      ElMessage.error(error.response.data.error)
    } else {
      ElMessage.error(isEditMode.value ? '更新用户失败' : '创建用户失败')
    }
  } finally {
    saving.value = false
  }
}

const toggleUserStatus = async (user) => {
  try {
    await axios.put(`/api/users/${user.id}/toggle-status`)
    ElMessage.success(`用户已${user.enabled ? '禁用' : '启用'}`)
    loadUsers()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await axios.delete(`/api/users/${user.id}`)
    ElMessage.success('用户删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除用户失败')
    }
  }
}

const getRoleTagType = (role) => {
  const roleMap = {
    'ADMIN': 'danger',
    'DOCTOR': 'primary',
    'NURSE': 'success',
    'ANESTHESIOLOGIST': 'warning',
    'PATIENT': 'info'
  }
  return roleMap[role] || 'info'
}

const getRoleText = (role) => {
  const roleMap = {
    'ADMIN': '管理员',
    'DOCTOR': '医生',
    'NURSE': '护士',
    'ANESTHESIOLOGIST': '麻醉师',
    'PATIENT': '病人'
  }
  return roleMap[role] || role
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return dayjs(dateString).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style lang="scss" scoped>
.user-management {
  padding: 20px;
  
  .page-header {
    margin-bottom: 24px;
    
    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #2c3e50;
      margin-bottom: 8px;
    }
    
    p {
      color: #7f8c8d;
      margin: 0;
    }
  }
  
  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    flex-wrap: wrap;
    gap: 16px;
    
    .search-section {
      flex: 1;
      min-width: 300px;
      
      .search-controls {
        display: flex;
        gap: 12px;
        align-items: center;
        flex-wrap: wrap;
        
        .search-input {
          flex: 1;
          min-width: 280px;
          max-width: 400px;
          
          :deep(.el-input__wrapper) {
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            border-radius: 8px;
          }
        }
        
        .role-filter {
          width: 140px;
          flex-shrink: 0;
          
          :deep(.el-select__wrapper) {
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
            border-radius: 8px;
          }
        }
      }
    }
    
    .action-buttons {
      display: flex;
      gap: 12px;
      flex-shrink: 0;
      
      .el-button {
        border-radius: 8px;
        padding: 10px 20px;
        font-weight: 500;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        
        &.el-button--primary {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          
          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
          }
        }
        
        &:not(.el-button--primary) {
          background: white;
          border: 1px solid #e5e7eb;
          color: #6b7280;
          
          &:hover {
            background: #f9fafb;
            border-color: #d1d5db;
            transform: translateY(-1px);
            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
          }
        }
      }
    }
  }
  
  .user-table {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    
    .table-actions {
      display: flex;
      gap: 4px;
      align-items: center;
      justify-content: flex-start;
      white-space: nowrap;
      
      .action-btn {
        padding: 2px 6px !important;
        font-size: 11px !important;
        height: 24px !important;
        border-radius: 3px !important;
        border: none !important;
        font-weight: 400 !important;
        min-width: auto !important;
        
        &.el-button--primary {
          &.is-plain {
            background: rgba(64, 158, 255, 0.1) !important;
            color: #409eff !important;
            
            &:hover {
              background: rgba(64, 158, 255, 0.2) !important;
              color: #409eff !important;
            }
          }
        }
        
        &.el-button--warning {
          &.is-plain {
            background: rgba(230, 162, 60, 0.1) !important;
            color: #e6a23c !important;
            
            &:hover {
              background: rgba(230, 162, 60, 0.2) !important;
              color: #e6a23c !important;
            }
          }
        }
        
        &.el-button--success {
          &.is-plain {
            background: rgba(103, 194, 58, 0.1) !important;
            color: #67c23a !important;
            
            &:hover {
              background: rgba(103, 194, 58, 0.2) !important;
              color: #67c23a !important;
            }
          }
        }
        
        &.el-button--danger {
          &.is-plain {
            background: rgba(245, 108, 108, 0.1) !important;
            color: #f56c6c !important;
            
            &:hover {
              background: rgba(245, 108, 108, 0.2) !important;
              color: #f56c6c !important;
            }
          }
        }
        
        .el-icon {
          font-size: 11px !important;
          margin-right: 2px !important;
        }
      }
    }
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
  
  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .user-management {
    padding: 16px;
    
    .action-bar {
      flex-direction: column;
      align-items: stretch;
      
      .search-section {
        .search-controls {
          .search-input {
            width: 100%;
          }
        }
      }
      
      .action-buttons {
        justify-content: center;
      }
    }
  }
}
</style> 