<template>
  <button
    :class="buttonClasses"
    :disabled="disabled || loading"
    @click="handleClick"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    v-motion
    :initial="{ scale: 1 }"
    :hover="{ scale: hoverScale }"
    :tap="{ scale: tapScale }"
  >
    <span class="button-content">
      <Icon v-if="icon && !loading" :icon="icon" class="button-icon" />
      <div v-if="loading" class="loading-spinner"></div>
      <span class="button-text">
        <slot>{{ text }}</slot>
      </span>
    </span>
    
    <!-- 波纹效果 -->
    <div class="ripple-container">
      <div
        v-for="ripple in ripples"
        :key="ripple.id"
        class="ripple"
        :style="ripple.style"
      ></div>
    </div>
    
    <!-- 光泽效果 -->
    <div class="shine-effect" v-if="shine"></div>
  </button>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'success', 'warning', 'danger', 'info', 'default'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  text: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  round: {
    type: Boolean,
    default: false
  },
  circle: {
    type: Boolean,
    default: false
  },
  plain: {
    type: Boolean,
    default: false
  },
  gradient: {
    type: Boolean,
    default: true
  },
  ripple: {
    type: Boolean,
    default: true
  },
  shine: {
    type: Boolean,
    default: true
  },
  hoverScale: {
    type: Number,
    default: 1.05
  },
  tapScale: {
    type: Number,
    default: 0.95
  },
  animationType: {
    type: String,
    default: 'bounce',
    validator: (value) => ['bounce', 'pulse', 'shake', 'glow', 'none'].includes(value)
  }
})

const emit = defineEmits(['click'])

const ripples = ref([])
const isHovered = ref(false)

const buttonClasses = computed(() => [
  'animated-button',
  `animated-button--${props.type}`,
  `animated-button--${props.size}`,
  {
    'animated-button--round': props.round,
    'animated-button--circle': props.circle,
    'animated-button--plain': props.plain,
    'animated-button--gradient': props.gradient,
    'animated-button--loading': props.loading,
    'animated-button--disabled': props.disabled,
    'animated-button--hovered': isHovered.value,
    [`animated-button--${props.animationType}`]: props.animationType !== 'none'
  }
])

const handleClick = (event) => {
  if (props.disabled || props.loading) return
  
  if (props.ripple) {
    createRipple(event)
  }
  
  emit('click', event)
}

const handleMouseEnter = () => {
  isHovered.value = true
}

const handleMouseLeave = () => {
  isHovered.value = false
}

const createRipple = (event) => {
  const button = event.currentTarget
  const rect = button.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = event.clientX - rect.left - size / 2
  const y = event.clientY - rect.top - size / 2
  
  const ripple = {
    id: Date.now(),
    style: {
      width: size + 'px',
      height: size + 'px',
      left: x + 'px',
      top: y + 'px'
    }
  }
  
  ripples.value.push(ripple)
  
  setTimeout(() => {
    const index = ripples.value.findIndex(r => r.id === ripple.id)
    if (index > -1) {
      ripples.value.splice(index, 1)
    }
  }, 600)
}
</script>

<style scoped>
.animated-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  user-select: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  outline: none;
  font-family: inherit;
}

/* 尺寸 */
.animated-button--small {
  padding: 8px 16px;
  font-size: 12px;
  border-radius: 6px;
}

.animated-button--medium {
  padding: 12px 24px;
  font-size: 14px;
  border-radius: 8px;
}

.animated-button--large {
  padding: 16px 32px;
  font-size: 16px;
  border-radius: 10px;
}

/* 形状 */
.animated-button--round {
  border-radius: 50px;
}

.animated-button--circle {
  border-radius: 50%;
  width: 40px;
  height: 40px;
  padding: 0;
}

.animated-button--circle.animated-button--small {
  width: 32px;
  height: 32px;
}

.animated-button--circle.animated-button--large {
  width: 48px;
  height: 48px;
}

/* 类型样式 */
.animated-button--primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.animated-button--success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(103, 194, 58, 0.3);
}

.animated-button--warning {
  background: linear-gradient(135deg, #e6a23c 0%, #f7ba2a 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(230, 162, 60, 0.3);
}

.animated-button--danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(245, 108, 108, 0.3);
}

.animated-button--info {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(144, 147, 153, 0.3);
}

.animated-button--default {
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
  color: #606266;
  border: 1px solid #dcdfe6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 朴素样式 */
.animated-button--plain.animated-button--primary {
  background: transparent;
  color: #667eea;
  border: 1px solid #667eea;
}

.animated-button--plain.animated-button--success {
  background: transparent;
  color: #67c23a;
  border: 1px solid #67c23a;
}

/* 悬停效果 */
.animated-button:hover:not(.animated-button--disabled):not(.animated-button--loading) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

/* 动画类型 */
.animated-button--bounce:active {
  animation: bounce 0.3s ease;
}

.animated-button--pulse {
  animation: pulse 2s infinite;
}

.animated-button--shake:hover {
  animation: shake 0.5s ease-in-out;
}

.animated-button--glow {
  animation: glow 2s ease-in-out infinite alternate;
}

/* 状态 */
.animated-button--loading {
  pointer-events: none;
  opacity: 0.8;
}

.animated-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* 内容 */
.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.button-icon {
  margin-right: 6px;
  font-size: 1.2em;
}

.button-text {
  line-height: 1;
}

/* 加载动画 */
.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 6px;
}

/* 波纹效果 */
.ripple-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
  border-radius: inherit;
}

.ripple {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: scale(0);
  animation: rippleEffect 0.6s linear;
}

/* 光泽效果 */
.shine-effect {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.animated-button:hover .shine-effect {
  left: 100%;
}

/* 动画定义 */
@keyframes bounce {
  0%, 20%, 60%, 100% { transform: translateY(0); }
  40% { transform: translateY(-10px); }
  80% { transform: translateY(-5px); }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(102, 126, 234, 0); }
  100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0); }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}

@keyframes glow {
  from { box-shadow: 0 0 5px rgba(102, 126, 234, 0.5); }
  to { box-shadow: 0 0 20px rgba(102, 126, 234, 0.8); }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes rippleEffect {
  to {
    transform: scale(4);
    opacity: 0;
  }
}
</style>
