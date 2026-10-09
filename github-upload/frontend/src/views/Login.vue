<template>
  <div class="login-container">
    <!-- 背景粒子效果 -->
    <div class="particles-bg"></div>

    <!-- 浮动图标 -->
    <div class="floating-icons">
      <Icon icon="mdi:book-open-page-variant" class="floating-icon icon-1" />
      <Icon icon="mdi:school" class="floating-icon icon-2" />
      <Icon icon="mdi:lightbulb-on" class="floating-icon icon-3" />
      <Icon icon="mdi:chart-line" class="floating-icon icon-4" />
    </div>

    <el-card
      class="login-card"
      v-motion
      :initial="{ opacity: 0, y: 50, scale: 0.9 }"
      :enter="{ opacity: 1, y: 0, scale: 1, transition: { duration: 800, ease: 'easeOut' } }"
    >
      <div
        class="login-header"
        v-motion
        :initial="{ opacity: 0, y: -20 }"
        :enter="{ opacity: 1, y: 0, transition: { duration: 600, delay: 200 } }"
      >
        <div class="logo-container">
          <!-- 校徽图片 - 请将校徽文件放在 src/assets/ 目录下 -->
          <img src="@/assets/logo.png" alt="江苏师范大学校徽" class="logo-img" v-if="logoExists" @error="logoExists = false"/>
          <Icon icon="mdi:school" class="logo-icon" v-if="!logoExists"/>
          <h2 class="system-title">智慧教学云脑系统</h2>
        </div>
        <p class="system-subtitle">江苏师范大学</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="0"
        class="login-form"
        v-motion
        :initial="{ opacity: 0, y: 20 }"
        :enter="{ opacity: 1, y: 0, transition: { duration: 600, delay: 400 } }"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
            class="animated-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            class="animated-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-button btn-animate"
            :loading="loading"
            @click="handleLogin"
            v-motion
            :initial="{ scale: 1 }"
            :tap="{ scale: 0.95 }"
          >
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore, safeRedirect } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loginForm = reactive({
  username: '',
  password: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const loginFormRef = ref(null)
const loading = ref(false)

// 校徽图片状态管理
const logoExists = ref(true)

const handleLogin = () => {
  if (loginFormRef.value) {
    loginFormRef.value.validate(async valid => {
      if (valid) {
        loading.value = true
        try {
          // 登录成功后后端写入会话 Cookie；学生账号会被拒绝（学生端暂未开放）
          await authStore.login(loginForm.username, loginForm.password)
          ElMessage.success('登录成功')
          router.push(safeRedirect(route.query.redirect))
        } catch (error) {
          ElMessage.error(error.response?.data?.message || '登录失败，请稍后重试')
        } finally {
          loading.value = false
        }
      }
    })
  }
}
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 背景粒子效果 */
.particles-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(120, 119, 198, 0.2) 0%, transparent 50%);
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

/* 浮动图标 */
.floating-icons {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating-icon {
  position: absolute;
  color: rgba(255, 255, 255, 0.1);
  font-size: 3rem;
  animation: floatIcon 8s ease-in-out infinite;
}

.icon-1 {
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.icon-2 {
  top: 20%;
  right: 15%;
  animation-delay: 2s;
}

.icon-3 {
  bottom: 30%;
  left: 20%;
  animation-delay: 4s;
}

.icon-4 {
  bottom: 20%;
  right: 10%;
  animation-delay: 6s;
}

@keyframes floatIcon {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.1;
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
    opacity: 0.3;
  }
}

.login-card {
  width: 420px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.2);
  overflow: hidden;
  position: relative;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  padding-top: 20px;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

/* 校徽图片样式 */
.logo-img {
  height: 60px;
  width: auto;
  margin-right: 15px;
  animation: pulse 2s ease-in-out infinite;
}

.logo-icon {
  font-size: 2.5rem;
  color: #667eea;
  margin-right: 10px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.system-title {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.system-subtitle {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
  opacity: 0.8;
}

.login-form {
  padding: 0 20px 20px;
}

.animated-input {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.animated-input:hover {
  transform: translateY(-2px);
}

.animated-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.animated-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.animated-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.user-type-group {
  width: 100%;
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.user-type-radio {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  border-radius: 12px;
  background: rgba(102, 126, 234, 0.1);
  transition: all 0.3s ease;
  margin: 0 10px;
}

.user-type-radio:hover {
  background: rgba(102, 126, 234, 0.2);
  transform: translateY(-2px);
}

.radio-icon {
  margin-right: 8px;
  font-size: 1.2rem;
  color: #667eea;
}

.login-button {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 1.1rem;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.login-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.login-button:hover::before {
  left: 100%;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    margin: 0 20px;
  }

  .system-title {
    font-size: 1.5rem;
  }

  .floating-icon {
    font-size: 2rem;
  }
}

/* 加载状态动画 */
.login-button.is-loading {
  background: linear-gradient(135deg, #a0a0a0, #808080);
}

/* 表单验证错误动画 */
.el-form-item.is-error .animated-input {
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}
</style>