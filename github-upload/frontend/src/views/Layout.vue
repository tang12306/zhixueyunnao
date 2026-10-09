<template>
  <div class="layout">
    <el-container class="layout-container">
      <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
        <div class="logo-container">
          <!-- 校徽图片 - 请将校徽文件放在 src/assets/ 目录下 -->
          <img src="@/assets/logo.png" alt="江苏师范大学校徽" class="logo-img" v-if="!isCollapse && logoExists" @error="logoExists = false"/>
          <img src="@/assets/logo-small.png" alt="江苏师范大学校徽" class="logo-img-small" v-if="isCollapse && logoSmallExists" @error="logoSmallExists = false"/>

          <!-- 文字Logo - 当图片不存在时显示 -->
          <div v-if="!isCollapse" class="logo-content">
            <Icon icon="mdi:school" class="logo-icon" v-if="!logoExists"/>
            <span class="logo-text">智慧教学云脑</span>
          </div>
          <div v-if="isCollapse" class="logo-content-small">
            <Icon icon="mdi:school" class="logo-icon-small" v-if="!logoSmallExists"/>
            <span class="logo-text-small" v-if="!logoSmallExists">云脑</span>
          </div>
        </div>
        <el-menu
          :router="true"
          :default-active="$route.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          :collapse="isCollapse"
        >
          <el-menu-item index="/dashboard">
            <el-icon><House /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-sub-menu index="/question-bank">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>题库管理</span>
            </template>
            <el-menu-item index="/question-bank">
              <el-icon><Tickets /></el-icon>
              <span>题目列表</span>
            </el-menu-item>
            <el-menu-item index="/question-bank/create">
              <el-icon><Plus /></el-icon>
              <span>创建题目</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/auto-quiz">
            <template #title>
              <el-icon><Tickets /></el-icon>
              <span>智能组卷</span>
            </template>
            <el-menu-item index="/auto-quiz/ai-create">
              <el-icon><MagicStick /></el-icon>
              <span>AI智能出题</span>
            </el-menu-item>
            <el-menu-item index="/auto-quiz/select-from-bank">
              <el-icon><Document /></el-icon>
              <span>题库选题组卷</span>
            </el-menu-item>
            <el-menu-item index="/auto-quiz/ai-exam-generation">
              <el-icon><DocumentAdd /></el-icon>
              <span>AI一键出卷</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/student-management">
            <template #title>
              <el-icon><User /></el-icon>
              <span>学生管理</span>
            </template>
            <el-menu-item index="/student-management">
              <el-icon><UserFilled /></el-icon>
              <span>学生列表</span>
            </el-menu-item>
            <el-menu-item index="/student-management/create">
              <el-icon><Plus /></el-icon>
              <span>添加学生</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/system-management">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system-management/subjects">
              <el-icon><Files /></el-icon>
              <span>科目管理</span>
            </el-menu-item>
            <el-menu-item index="/system-management/chapters">
              <el-icon><Notebook /></el-icon>
              <span>章节管理</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/organization">
            <template #title>
              <el-icon><User /></el-icon>
              <span>组织管理</span>
            </template>
            <el-menu-item index="/organization/colleges">
              <el-icon><Files /></el-icon>
              <span>学院管理</span>
            </el-menu-item>
            <el-menu-item index="/organization/majors">
              <el-icon><Notebook /></el-icon>
              <span>专业管理</span>
            </el-menu-item>
            <el-menu-item index="/organization/classes">
              <el-icon><UserFilled /></el-icon>
              <span>班级管理</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/profile">
            <el-icon><Avatar /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
          <!-- 系统设置只对管理员开放 -->
          <el-menu-item v-if="authStore.isAdmin" index="/settings">
            <el-icon><Tools /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
          <el-menu-item @click="logout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <div class="header-left">
            <el-icon @click="toggleSidebar" class="collapse-icon">
              <component :is="isCollapse ? Expand : Fold" />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown>
              <span class="user-profile">
                <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" class="user-avatar" />
                <span>管理员</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人信息</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ref } from 'vue'
import { Icon } from '@iconify/vue'
import { 
  House,
  Collection,
  Tickets,
  Plus,
  MagicStick,
  Setting,
  Files,
  Notebook,
  Avatar,
  Tools,
  SwitchButton,
  Fold,
  Expand,
  User,
  UserFilled,
  Document,
  DocumentAdd
} from '@element-plus/icons-vue';

const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)

// 校徽图片状态管理
const logoExists = ref(true)
const logoSmallExists = ref(true)

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
  // 你可能需要在这里添加实际改变侧边栏宽度的逻辑，例如通过修改一个CSS变量或直接操作DOM/父组件状态
}

const logout = async () => {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout-container {
  height: 100%;
}

.sidebar {
  /* border: 5px solid red !important; */ /* 移除了测试边框 */
  background-color: #304156;
  height: 100%;
  transition: width 0.3s;
  overflow-y: auto;
}

.logo-container {
  height: 50px;
  line-height: 50px;
  text-align: center;
  background: #2b2f3a; /* Slightly darker background for logo */
  padding: 5px;
  color: #fff; /* Added color for text logo */
  overflow: hidden; /* Prevent text overflow when collapsing */
}

/* 校徽图片样式 */
.logo-img {
  height: 40px;
  width: auto;
  vertical-align: middle;
  margin-right: 8px;
}

.logo-img-small {
  height: 32px;
  width: 32px;
  vertical-align: middle;
}

/* Logo内容容器 */
.logo-content {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-content-small {
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-icon {
  font-size: 24px;
  margin-right: 8px;
  color: #409eff;
}

.logo-icon-small {
  font-size: 20px;
  color: #409eff;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  vertical-align: middle;
}

.logo-text-small {
  font-size: 18px; /* Adjust as needed */
  font-weight: bold;
  vertical-align: middle;
}

.header {
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-icon {
  margin-right: 15px;
  font-size: 20px;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  height: calc(100vh - 64px); /* 确保内容区域不被header遮挡 */
  overflow-y: auto;
}
</style> 