<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-animation">
        <div class="error-number">404</div>
        <div class="error-icon">
          <el-icon><Warning /></el-icon>
        </div>
      </div>
      
      <div class="error-text">
        <h1>页面未找到</h1>
        <p>抱歉，您访问的页面不存在或已被移除</p>
      </div>
      
      <div class="error-actions">
        <el-button 
          type="primary" 
          size="large"
          @click="goHome"
        >
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        
        <el-button 
          size="large"
          @click="goBack"
        >
          <el-icon><Back /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
    
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { Warning, HomeFilled, Back } from '@element-plus/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.not-found-content {
  text-align: center;
  z-index: 10;
  padding: 40px;
}

.error-animation {
  margin-bottom: 40px;
  position: relative;
  
  .error-number {
    font-size: 120px;
    font-weight: 900;
    color: rgba(255, 255, 255, 0.9);
    line-height: 1;
    margin-bottom: 20px;
    text-shadow: 0 8px 30px rgba(0, 0, 0, 0.3);
    animation: bounce 2s infinite;
  }
  
  .error-icon {
    .el-icon {
      font-size: 60px;
      color: #ffd93d;
      animation: pulse 2s infinite;
    }
  }
}

.error-text {
  margin-bottom: 40px;
  
  h1 {
    font-size: 36px;
    color: white;
    margin-bottom: 16px;
    font-weight: 700;
    text-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  }
  
  p {
    font-size: 18px;
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.6;
    margin: 0;
  }
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  
  .el-button {
    padding: 12px 24px;
    font-size: 16px;
    border-radius: 8px;
    
    &--primary {
      background: linear-gradient(135deg, #ffd93d 0%, #ff9a56 100%);
      border: none;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 25px rgba(255, 217, 61, 0.3);
      }
    }
    
    &:not(.el-button--primary) {
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      color: white;
      
      &:hover {
        background: rgba(255, 255, 255, 0.3);
        transform: translateY(-2px);
      }
    }
  }
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  
  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    animation: float 6s ease-in-out infinite;
    
    &.circle-1 {
      width: 200px;
      height: 200px;
      top: 10%;
      left: 10%;
      animation-delay: 0s;
    }
    
    &.circle-2 {
      width: 150px;
      height: 150px;
      top: 60%;
      right: 15%;
      animation-delay: 2s;
    }
    
    &.circle-3 {
      width: 100px;
      height: 100px;
      bottom: 20%;
      left: 20%;
      animation-delay: 4s;
    }
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .error-animation .error-number {
    font-size: 80px;
  }
  
  .error-text h1 {
    font-size: 28px;
  }
  
  .error-text p {
    font-size: 16px;
  }
  
  .error-actions {
    flex-direction: column;
    align-items: center;
    
    .el-button {
      width: 200px;
    }
  }
}
</style> 