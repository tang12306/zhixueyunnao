<template>
  <div class="loading-container" v-if="visible">
    <div class="loading-backdrop" @click="handleBackdropClick"></div>
    <div class="loading-content">
      <div class="spinner-container">
        <div class="spinner" :class="spinnerType">
          <div v-if="spinnerType === 'dots'" class="dots">
            <div class="dot" v-for="i in 3" :key="i"></div>
          </div>
          <div v-else-if="spinnerType === 'circle'" class="circle">
            <div class="circle-inner"></div>
          </div>
          <div v-else-if="spinnerType === 'pulse'" class="pulse">
            <div class="pulse-ring" v-for="i in 3" :key="i"></div>
          </div>
          <div v-else class="default-spinner"></div>
        </div>
      </div>
      <div class="loading-text" v-if="text">
        {{ text }}
      </div>
      <div class="loading-progress" v-if="showProgress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="progress-text">{{ progress }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: '加载中...'
  },
  spinnerType: {
    type: String,
    default: 'circle', // 'dots', 'circle', 'pulse', 'default'
    validator: (value) => ['dots', 'circle', 'pulse', 'default'].includes(value)
  },
  showProgress: {
    type: Boolean,
    default: false
  },
  progress: {
    type: Number,
    default: 0,
    validator: (value) => value >= 0 && value <= 100
  },
  canCancel: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['cancel'])

const handleBackdropClick = () => {
  if (props.canCancel) {
    emit('cancel')
  }
}

// 防止页面滚动
watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
.loading-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.3s ease-out;
}

.loading-content {
  position: relative;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  text-align: center;
  animation: slideIn 0.3s ease-out;
  max-width: 300px;
  width: 90%;
}

.spinner-container {
  margin-bottom: 20px;
}

.loading-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 20px;
  font-weight: 500;
}

/* 点状加载器 */
.dots {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #667eea;
  animation: dotPulse 1.4s ease-in-out infinite both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes dotPulse {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 圆形加载器 */
.circle {
  width: 50px;
  height: 50px;
  margin: 0 auto;
  position: relative;
}

.circle-inner {
  width: 100%;
  height: 100%;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: circleRotate 1s linear infinite;
}

@keyframes circleRotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 脉冲加载器 */
.pulse {
  width: 50px;
  height: 50px;
  margin: 0 auto;
  position: relative;
}

.pulse-ring {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border: 3px solid #667eea;
  border-radius: 50%;
  animation: pulseRing 1.25s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
}

.pulse-ring:nth-child(2) { animation-delay: 0.33s; }
.pulse-ring:nth-child(3) { animation-delay: 0.66s; }

@keyframes pulseRing {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

/* 默认加载器 */
.default-spinner {
  width: 50px;
  height: 50px;
  margin: 0 auto;
  border: 4px solid #f3f3f3;
  border-radius: 50%;
  border-top: 4px solid #667eea;
  animation: defaultSpin 1s linear infinite;
}

@keyframes defaultSpin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 进度条 */
.loading-progress {
  margin-top: 20px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 4px;
  transition: width 0.3s ease;
  animation: progressShine 2s ease-in-out infinite;
}

@keyframes progressShine {
  0% { background-position: -200px 0; }
  100% { background-position: 200px 0; }
}

.progress-text {
  font-size: 14px;
  color: #999;
  font-weight: 500;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .loading-content {
    padding: 30px 20px;
    margin: 20px;
  }
  
  .loading-text {
    font-size: 14px;
  }
}
</style>
