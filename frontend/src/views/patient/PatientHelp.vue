<template>
  <PatientLayout 
    :unread-count="unreadCount"
    :has-patient-info="!!patientInfo"
    :is-blacklisted="isBlacklisted"
  >
    <div class="patient-help-page">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>帮助中心</h2>
      </div>

      <!-- 搜索框 -->
      <el-card class="search-card">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索您的问题..."
          size="large"
          :prefix-icon="Search"
          @keyup.enter="searchHelp"
        >
          <template #append>
            <el-button @click="searchHelp">搜索</el-button>
          </template>
        </el-input>
      </el-card>

      <!-- 常见问题分类 -->
      <el-row :gutter="20" class="category-row">
        <el-col :xs="24" :sm="12" :md="6" v-for="category in categories" :key="category.id">
          <el-card class="category-card" @click="selectCategory(category)">
            <div class="category-content">
              <el-icon class="category-icon" :style="{ color: category.color }">
                <component :is="category.icon" />
              </el-icon>
              <h3>{{ category.name }}</h3>
              <p>{{ category.description }}</p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- FAQ列表 -->
      <el-card class="faq-card">
        <template #header>
          <div class="card-header">
            <h3>{{ selectedCategory ? selectedCategory.name : '常见问题' }}</h3>
            <el-button 
              v-if="selectedCategory" 
              type="text" 
              @click="selectedCategory = null"
            >
              查看全部
            </el-button>
          </div>
        </template>
        
        <el-collapse v-model="activeNames" accordion>
          <el-collapse-item
            v-for="faq in filteredFAQs"
            :key="faq.id"
            :title="faq.question"
            :name="faq.id"
          >
            <div class="faq-answer" v-html="faq.answer"></div>
            <div class="faq-footer">
              <span class="helpful-text">这个回答有帮助吗？</span>
              <el-button-group size="small">
                <el-button @click="markHelpful(faq, true)">
                  👍 有帮助 ({{ faq.helpful }})
                </el-button>
                <el-button @click="markHelpful(faq, false)">
                  👎 没帮助 ({{ faq.notHelpful }})
                </el-button>
              </el-button-group>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <!-- 联系方式 -->
      <el-card class="contact-card">
        <template #header>
          <h3>还有其他问题？</h3>
        </template>
        
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <div class="contact-item">
              <el-icon class="contact-icon"><Phone /></el-icon>
              <div class="contact-info">
                <h4>电话咨询</h4>
                <p>工作时间：周一至周五 8:00-17:00</p>
                <p class="contact-value">400-123-4567</p>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12">
            <div class="contact-item">
              <el-icon class="contact-icon"><Message /></el-icon>
              <div class="contact-info">
                <h4>在线客服</h4>
                <p>7×24小时在线服务</p>
                <el-button type="primary" size="small" @click="openChat">
                  立即咨询
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 使用指南 -->
      <el-card class="guide-card">
        <template #header>
          <h3>系统使用指南</h3>
        </template>
        
        <el-steps direction="vertical" :active="0">
          <el-step title="注册账号" description="使用手机号注册病人账号">
            <template #description>
              <p>1. 点击注册按钮</p>
              <p>2. 填写手机号和验证码</p>
              <p>3. 设置登录密码</p>
            </template>
          </el-step>
          <el-step title="提交病情信息" description="完善个人病情资料">
            <template #description>
              <p>1. 登录后进入个人中心</p>
              <p>2. 点击"提交病情信息"</p>
              <p>3. 填写病历号、病情描述等信息</p>
              <p>4. 系统会自动为您分配医生</p>
            </template>
          </el-step>
          <el-step title="等待手术安排" description="医生会根据您的病情安排手术">
            <template #description>
              <p>1. 医生评估病情后会安排手术</p>
              <p>2. 您会收到手术安排通知</p>
              <p>3. 可在"手术预约"页面查看详情</p>
            </template>
          </el-step>
          <el-step title="确认手术信息" description="查看并确认手术相关信息">
            <template #description>
              <p>1. 查看手术时间、地点、医生等信息</p>
              <p>2. 如需取消请提前48小时操作</p>
              <p>3. 按时到达医院进行手术</p>
            </template>
          </el-step>
        </el-steps>
      </el-card>
    </div>
  </PatientLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Search, Calendar, Document, Bell, QuestionFilled, 
  Phone, Message
} from '@element-plus/icons-vue'
import PatientLayout from '../../layouts/PatientLayout.vue'

const patientInfo = ref(null)
const unreadCount = ref(0)
const isBlacklisted = ref(false)

const searchKeyword = ref('')
const selectedCategory = ref(null)
const activeNames = ref([])

const categories = ref([
  {
    id: 1,
    name: '预约相关',
    description: '手术预约、取消、修改等',
    icon: 'Calendar',
    color: '#409eff'
  },
  {
    id: 2,
    name: '病情信息',
    description: '病情提交、修改、查看等',
    icon: 'Document',
    color: '#67c23a'
  },
  {
    id: 3,
    name: '通知消息',
    description: '通知查看、提醒设置等',
    icon: 'Bell',
    color: '#e6a23c'
  },
  {
    id: 4,
    name: '其他问题',
    description: '账号、系统使用等',
    icon: 'QuestionFilled',
    color: '#909399'
  }
])

const faqs = ref([
  {
    id: 1,
    categoryId: 1,
    question: '如何取消手术预约？',
    answer: `<p>取消手术预约需要注意以下几点：</p>
    <ol>
      <li>必须提前48小时取消，否则会被列入黑名单</li>
      <li>每月只能取消1次预约</li>
      <li>在"手术预约"页面找到要取消的预约，点击"取消"按钮</li>
      <li>填写取消原因并确认</li>
    </ol>
    <p><strong>注意：</strong>违规取消将导致您被列入黑名单1个月，期间无法使用预约系统。</p>`,
    helpful: 156,
    notHelpful: 12
  },
  {
    id: 2,
    categoryId: 1,
    question: '什么情况下会被列入黑名单？',
    answer: `<p>以下情况会被列入黑名单：</p>
    <ul>
      <li>未提前48小时取消预约</li>
      <li>恶意占用医疗资源</li>
      <li>提供虚假病情信息</li>
    </ul>
    <p>黑名单期限通常为1个月，期间您将无法：</p>
    <ul>
      <li>提交新的病情信息</li>
      <li>进行手术预约</li>
      <li>取消现有预约</li>
    </ul>`,
    helpful: 89,
    notHelpful: 5
  },
  {
    id: 3,
    categoryId: 2,
    question: '如何提交病情信息？',
    answer: `<p>提交病情信息的步骤：</p>
    <ol>
      <li>登录系统后，点击"提交病情信息"按钮</li>
      <li>填写以下必要信息：
        <ul>
          <li>病历号</li>
          <li>身份证号</li>
          <li>紧急联系人及电话</li>
          <li>详细病情描述</li>
          <li>病情严重程度</li>
        </ul>
      </li>
      <li>填写既往病史、过敏史等补充信息</li>
      <li>提交后系统会自动为您分配医生</li>
    </ol>`,
    helpful: 234,
    notHelpful: 8
  },
  {
    id: 4,
    categoryId: 2,
    question: '提交病情信息后多久会安排手术？',
    answer: `<p>手术安排时间取决于多个因素：</p>
    <ul>
      <li><strong>病情严重程度：</strong>紧急情况会优先安排</li>
      <li><strong>医生评估：</strong>医生需要时间评估您的病情</li>
      <li><strong>手术室资源：</strong>需要协调手术室和医护人员</li>
    </ul>
    <p>一般情况下：</p>
    <ul>
      <li>紧急手术：24小时内安排</li>
      <li>急迫手术：3-7天内安排</li>
      <li>普通手术：7-14天内安排</li>
    </ul>
    <p>您会在手术安排确定后收到系统通知。</p>`,
    helpful: 178,
    notHelpful: 15
  },
  {
    id: 5,
    categoryId: 3,
    question: '如何查看通知消息？',
    answer: `<p>查看通知消息的方法：</p>
    <ol>
      <li>点击顶部导航栏的铃铛图标</li>
      <li>或者进入"通知消息"页面</li>
      <li>未读消息会有蓝色背景标识</li>
      <li>点击消息可查看详情并自动标记为已读</li>
    </ol>
    <p>通知类型包括：</p>
    <ul>
      <li>手术安排通知</li>
      <li>手术确认通知</li>
      <li>手术取消通知</li>
      <li>手术更新通知</li>
    </ul>`,
    helpful: 92,
    notHelpful: 3
  }
])

const filteredFAQs = computed(() => {
  let result = faqs.value
  
  if (selectedCategory.value) {
    result = result.filter(faq => faq.categoryId === selectedCategory.value.id)
  }
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(faq => 
      faq.question.toLowerCase().includes(keyword) ||
      faq.answer.toLowerCase().includes(keyword)
    )
  }
  
  return result
})

onMounted(() => {
  // 加载必要数据
})

const searchHelp = () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  // 执行搜索
  selectedCategory.value = null
}

const selectCategory = (category) => {
  selectedCategory.value = category
  searchKeyword.value = ''
}

const markHelpful = (faq, isHelpful) => {
  if (isHelpful) {
    faq.helpful++
    ElMessage.success('感谢您的反馈！')
  } else {
    faq.notHelpful++
    ElMessage.info('我们会继续改进，谢谢反馈！')
  }
}

const openChat = () => {
  ElMessage.info('在线客服功能开发中...')
}
</script>

<style lang="scss" scoped>
.patient-help-page {
  padding: 24px;
  
  .page-header {
    margin-bottom: 24px;
    
    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }
  
  .search-card {
    margin-bottom: 24px;
  }
  
  .category-row {
    margin-bottom: 24px;
    
    .category-card {
      cursor: pointer;
      transition: all 0.3s ease;
      margin-bottom: 20px;
      
      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
      }
      
      .category-content {
        text-align: center;
        padding: 20px 0;
        
        .category-icon {
          font-size: 48px;
          margin-bottom: 16px;
        }
        
        h3 {
          margin: 0 0 8px 0;
          font-size: 18px;
          color: #303133;
        }
        
        p {
          margin: 0;
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
  
  .faq-card {
    margin-bottom: 24px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      h3 {
        margin: 0;
        font-size: 18px;
        color: #303133;
      }
    }
    
    .faq-answer {
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
      
      p {
        margin: 8px 0;
      }
      
      ul, ol {
        margin: 8px 0;
        padding-left: 20px;
      }
      
      strong {
        color: #303133;
      }
    }
    
    .faq-footer {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;
      display: flex;
      align-items: center;
      gap: 16px;
      
      .helpful-text {
        font-size: 13px;
        color: #909399;
      }
    }
  }
  
  .contact-card {
    margin-bottom: 24px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
    
    .contact-item {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 8px;
      
      .contact-icon {
        font-size: 32px;
        color: #409eff;
      }
      
      .contact-info {
        h4 {
          margin: 0 0 8px 0;
          font-size: 16px;
          color: #303133;
        }
        
        p {
          margin: 4px 0;
          font-size: 13px;
          color: #909399;
        }
        
        .contact-value {
          font-size: 18px;
          color: #409eff;
          font-weight: 600;
        }
      }
    }
  }
  
  .guide-card {
    h3 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
    
    :deep(.el-step__description) {
      p {
        margin: 4px 0;
        font-size: 13px;
        color: #606266;
      }
    }
  }
}
</style> 